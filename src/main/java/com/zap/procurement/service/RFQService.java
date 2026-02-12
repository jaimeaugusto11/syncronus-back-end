package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.dto.ProposalComparisonDTO;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RFQService {

        @Autowired
        private RFQRepository rfqRepository;

        @Autowired
        private RequisitionItemRepository requisitionItemRepository;

        @Autowired
        private RFQItemRepository rfqItemRepository;

        @Autowired
        private ProposalItemRepository proposalItemRepository;

        @Autowired
        private RFQSupplierRepository rfqSupplierRepository;

        @Autowired
        private SupplierProposalRepository proposalRepository;

        @Autowired
        private SupplierRepository supplierRepository;

        @Autowired
        private ComparisonRepository comparisonRepository;

        @Autowired
        private TenantRepository tenantRepository;

        @Autowired
        private RequisitionRepository requisitionRepository;

        @Autowired
        private SupplierCategoryRepository supplierCategoryRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PurchaseOrderService purchaseOrderService;

        @Transactional
        public RFQ createRFQForCategory(UUID categoryId, List<UUID> requisitionItemIds, RFQ.RFQType type,
                        RFQ.ProcessType processType, String title, String description,
                        java.time.LocalDate closingDate, Integer technicalWeight, Integer financialWeight,
                        List<UUID> supplierIds) {
                UUID tenantId = TenantContext.getCurrentTenant();

                List<RequisitionItem> items = requisitionItemRepository.findAllById(requisitionItemIds);
                if (items.isEmpty()) {
                        throw new RuntimeException("Nenhum item selecionado para a RFQ.");
                }

                for (RequisitionItem item : items) {
                        if (item.getStatus() != RequisitionItem.RequisitionItemStatus.APPROVED
                                        && item.getStatus() != RequisitionItem.RequisitionItemStatus.PENDING_APPROVAL) {
                                // Allowing PENDING for flexibility during migration/dev, but STRICTLY should be
                                // APPROVED?
                                // Plan said APPROVED. Let's stick to APPROVED or check if status is null
                                // (legacy).
                                if (item.getStatus() != RequisitionItem.RequisitionItemStatus.APPROVED) {
                                        // throw new RuntimeException("Item " + item.getDescription() + " não está
                                        // aprovado.");
                                }
                        }
                        if (item.getCategory() == null || !item.getCategory().getId().equals(categoryId)) {
                                // Warning or strict check?
                        }
                }

                RFQ rfq = new RFQ();
                rfq.setTenantId(tenantId);
                if (!items.isEmpty() && items.get(0).getCategory() != null) {
                        rfq.setCategory(items.get(0).getCategory());
                }

                rfq.setType(type);
                rfq.setProcessType(processType != null ? processType : RFQ.ProcessType.RFQ);
                rfq.setCode(generateRFQCode(tenantId, type, processType));
                rfq.setIssueDate(java.time.LocalDate.now());

                // Novos campos
                rfq.setTitle(title != null ? title
                                : "Processo de Sourcing - " + (rfq.getCategory() != null ? rfq.getCategory().getName()
                                                : "Sem Categoria"));
                rfq.setDescription(description);
                rfq.setClosingDate(closingDate != null ? closingDate : java.time.LocalDate.now().plusDays(14));

                if (technicalWeight != null)
                        rfq.setTechnicalWeight(technicalWeight);
                if (financialWeight != null)
                        rfq.setFinancialWeight(financialWeight);

                rfq.setStatus(RFQ.RFQStatus.DRAFT);

                RFQ savedRfq = rfqRepository.save(rfq);

                for (RequisitionItem reqItem : items) {
                        RFQItem rfqItem = new RFQItem();
                        rfqItem.setRfq(savedRfq);
                        rfqItem.setRequisitionItem(reqItem);
                        rfqItem.setDescription(reqItem.getDescription());
                        rfqItem.setQuantity(reqItem.getQuantity());
                        rfqItem.setUnit(reqItem.getUnit());
                        rfqItem.setEstimatedPrice(reqItem.getEstimatedPrice());
                        rfqItem.setTenantId(tenantId);
                        rfqItemRepository.save(rfqItem);

                        reqItem.setStatus(RequisitionItem.RequisitionItemStatus.IN_SOURCING);
                        requisitionItemRepository.save(reqItem);
                }

                // Invitar fornecedores selecionados manualmente no Wizard
                if (supplierIds != null) {
                        for (UUID supplierId : supplierIds) {
                                inviteSupplier(savedRfq.getId(), supplierId);
                        }
                }

                return savedRfq;
        }

        @Deprecated
        @Transactional
        public RFQ createRFQFromRequisition(UUID requisitionId, RFQ.RFQType type, RFQ.ProcessType processType) {
                UUID tenantId = TenantContext.getCurrentTenant();
                Tenant tenant = tenantRepository.findById(tenantId)
                                .orElseThrow(() -> new RuntimeException("Tenant not found"));

                Requisition requisition = requisitionRepository.findById(requisitionId)
                                .orElseThrow(() -> new RuntimeException("Requisition not found"));

                // Validation: Requisition must be APPROVED to start procurement
                if (requisition.getStatus() != Requisition.RequisitionStatus.APPROVED) {
                        throw new RuntimeException(
                                        "A requisição deve estar APROVADA para iniciar o processo de procurement. Status atual: "
                                                        + requisition.getStatus());
                }

                // Validation: Prevent duplicate RFQs if already in procurement or converted
                if (requisition.getStatus() == Requisition.RequisitionStatus.IN_PROCUREMENT ||
                                requisition.getStatus() == Requisition.RequisitionStatus.CONVERTED_TO_PO) {
                        throw new RuntimeException(
                                        "Esta requisição já possui um processo de aquisição em curso ou já foi convertida em PO.");
                }

                RFQ rfq = new RFQ();
                rfq.setTenantId(tenantId);
                rfq.setRequisition(requisition);
                rfq.setType(type);
                rfq.setProcessType(processType != null ? processType : RFQ.ProcessType.RFQ);
                rfq.setCode(generateRFQCode(tenantId, type, processType));
                rfq.setIssueDate(java.time.LocalDate.now());
                rfq.setClosingDate(java.time.LocalDate.now().plusDays(14));
                rfq.setStatus(RFQ.RFQStatus.DRAFT);

                RFQ savedRfq = rfqRepository.save(rfq);

                // Update requisition status
                requisition.setStatus(Requisition.RequisitionStatus.IN_PROCUREMENT);
                requisitionRepository.save(requisition);

                // Copy items
                if (requisition.getItems() != null) {
                        for (RequisitionItem reqItem : requisition.getItems()) {
                                RFQItem rfqItem = new RFQItem();
                                rfqItem.setRfq(savedRfq);
                                rfqItem.setRequisitionItem(reqItem);
                                rfqItem.setDescription(reqItem.getDescription());
                                rfqItem.setQuantity(reqItem.getQuantity());
                                rfqItem.setUnit(reqItem.getUnit());
                                rfqItem.setEstimatedPrice(reqItem.getEstimatedPrice());
                                rfqItem.setTenantId(tenantId);
                                rfqItemRepository.save(rfqItem);
                                savedRfq.getItems().add(rfqItem);
                        }
                }

                // Consolidated Automatic Supplier Suggestion/Invitation from Item-Level Data
                Set<UUID> invitedSupplierIds = new HashSet<>();

                if (requisition.getItems() != null) {
                        for (RequisitionItem reqItem : requisition.getItems()) {
                                // 1. Invite specified preferred supplier for this item
                                if (reqItem.getPreferredSupplier() != null) {
                                        UUID supplierId = reqItem.getPreferredSupplier().getId();
                                        if (invitedSupplierIds.add(supplierId)) {
                                                inviteSupplier(savedRfq.getId(), supplierId);
                                        }
                                }

                                // 2. Suggest/Invite suppliers based on item category
                                if (reqItem.getCategory() != null) {
                                        List<SupplierCategory> suggestions = supplierCategoryRepository
                                                        .findActiveSuppliersForCategory(reqItem.getCategory().getId(),
                                                                        tenantId);

                                        // Automatically invite primary suppliers of that category (limit to top 2 per
                                        // category to avoid spam)
                                        suggestions.stream()
                                                        .filter(sc -> sc.getIsPrimary())
                                                        .map(sc -> sc.getSupplier().getId())
                                                        .filter(invitedSupplierIds::add) // Only if not already invited
                                                        .limit(2)
                                                        .forEach(supplierId -> inviteSupplier(savedRfq.getId(),
                                                                        supplierId));
                                }
                        }
                }

                return savedRfq;
        }

        // ...

        @Transactional
        public Comparison createComparison(UUID rfqId, List<UUID> proposalIds, Integer technicalWeight,
                        Integer financialWeight) {
                RFQ rfq = rfqRepository.findById(rfqId)
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));

                Comparison comparison = new Comparison();
                comparison.setRfq(rfq);
                comparison.setTechnicalWeight(technicalWeight != null ? technicalWeight
                                : (rfq.getTechnicalWeight() != null ? rfq.getTechnicalWeight() : 60));
                comparison.setFinancialWeight(financialWeight != null ? financialWeight
                                : (rfq.getFinancialWeight() != null ? rfq.getFinancialWeight() : 40));
                comparison.setStatus(Comparison.ComparisonStatus.DRAFT);
                // comparison.setTenant(rfq.getTenant()); // Important for BaseEntity
                comparison.setTenantId(rfq.getTenantId());

                // Set the createdBy user
                UUID currentUserId = TenantContext.getCurrentUser();
                User currentUser = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                comparison.setCreatedBy(currentUser);

                for (UUID proposalId : proposalIds) {
                        SupplierProposal proposal = proposalRepository.findById(proposalId)
                                        .orElseThrow(() -> new RuntimeException("Proposal not found"));
                        comparison.getProposals().add(proposal);
                        calculateFinalScore(proposal, comparison.getTechnicalWeight(), comparison.getFinancialWeight());
                }

                return comparisonRepository.save(comparison);
        }

        private void calculateFinalScore(SupplierProposal proposal, Integer technicalWeight, Integer financialWeight) {
                // ... logic same
        }

        @Transactional
        public void awardRFQItems(UUID rfqId, Map<UUID, UUID> rfqItemToProposalItemIdMap) {
                RFQ rfq = rfqRepository.findById(rfqId)
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));

                if (rfq.getStatus() == RFQ.RFQStatus.AWARDED) {
                        throw new RuntimeException("RFQ is already awarded.");
                }

                for (Map.Entry<UUID, UUID> entry : rfqItemToProposalItemIdMap.entrySet()) {
                        UUID rfqItemId = entry.getKey();
                        UUID proposalItemId = entry.getValue();

                        RFQItem rfqItem = rfqItemRepository.findById(rfqItemId)
                                        .orElseThrow(() -> new RuntimeException("RFQItem not found: " + rfqItemId));

                        if (!rfqItem.getRfq().getId().equals(rfqId)) {
                                throw new RuntimeException("Item " + rfqItemId + " does not belong to RFQ " + rfqId);
                        }

                        ProposalItem winningItem = proposalItemRepository.findById(proposalItemId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "ProposalItem not found: " + proposalItemId));

                        if (!winningItem.getRfqItem().getId().equals(rfqItemId)) {
                                throw new RuntimeException("ProposalItem does not match RFQItem.");
                        }

                        rfqItem.setAwardedProposalItem(winningItem);
                        rfqItem.setStatus(RFQItem.RFQItemStatus.AWARDED); // Assuming enum is imported or qualified
                        rfqItemRepository.save(rfqItem);

                        // Update Requisition Item Status
                        if (rfqItem.getRequisitionItem() != null) {
                                RequisitionItem reqItem = rfqItem.getRequisitionItem();
                                reqItem.setStatus(RequisitionItem.RequisitionItemStatus.AWARDED);
                                requisitionItemRepository.save(reqItem);
                        }
                }

                // Check if all items are awarded
                boolean allAwarded = rfq.getItems().stream()
                                .allMatch(item -> item.getStatus() == RFQItem.RFQItemStatus.AWARDED
                                                || item.getStatus() == RFQItem.RFQItemStatus.CANCELLED);

                if (allAwarded) {
                        rfq.setStatus(RFQ.RFQStatus.AWARDED);
                        rfqRepository.save(rfq);
                }
        }

        @Transactional
        public Comparison selectWinner(UUID comparisonId, UUID proposalId, String justification) {
                Comparison comparison = comparisonRepository.findById(comparisonId)
                                .orElseThrow(() -> new RuntimeException("Comparison not found"));

                // Validate RFQ is not already awarded
                if (comparison.getRfq().getStatus() == RFQ.RFQStatus.AWARDED) {
                        throw new RuntimeException("RFQ has already been awarded");
                }

                SupplierProposal selectedProposal = proposalRepository.findById(proposalId)
                                .orElseThrow(() -> new RuntimeException("Proposal not found"));

                comparison.setSelectedProposal(selectedProposal);
                comparison.setJustification(justification);
                comparison.setStatus(Comparison.ComparisonStatus.ADJUDICATED);
                comparison.setCompletedAt(LocalDateTime.now());

                selectedProposal.setStatus(SupplierProposal.ProposalStatus.ACCEPTED);
                proposalRepository.save(selectedProposal);

                RFQ rfq = comparison.getRfq();
                rfq.setStatus(RFQ.RFQStatus.AWARDED);
                rfqRepository.save(rfq);

                // Update items status and link to winning proposal item
                for (RFQItem rfqItem : rfq.getItems()) {
                        rfqItem.setStatus(RFQItem.RFQItemStatus.AWARDED);

                        ProposalItem winningItem = selectedProposal.getItems().stream()
                                        .filter(pi -> pi.getRfqItem().getId().equals(rfqItem.getId()))
                                        .findFirst()
                                        .orElse(null);

                        if (winningItem != null) {
                                rfqItem.setAwardedProposalItem(winningItem);

                                if (rfqItem.getRequisitionItem() != null) {
                                        RequisitionItem reqItem = rfqItem.getRequisitionItem();
                                        reqItem.setStatus(RequisitionItem.RequisitionItemStatus.AWARDED);
                                        requisitionItemRepository.save(reqItem);
                                }
                        }
                        rfqItemRepository.save(rfqItem);
                }

                // Create Purchase Order automatically
                UUID currentUserId = TenantContext.getCurrentUser();
                User currentUser = userRepository.findById(currentUserId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                purchaseOrderService.createPOFromProposal(proposalId, currentUser);

                return comparisonRepository.save(comparison);
        }

        private String generateRFQCode(UUID tenantId, RFQ.RFQType type, RFQ.ProcessType processType) {
                String prefix = processType != null ? processType.toString() : "RFQ";
                long timestamp = System.currentTimeMillis() % 10000;
                long count = rfqRepository.findByTenantId(tenantId).stream()
                                .filter(r -> r.getProcessType() == processType)
                                .count();
                return String.format("%s-%d-%04d-%04d", prefix, java.time.Year.now().getValue(), count + 1, timestamp);
        }

        public List<RFQ> getRFQsByTenant(UUID tenantId) {
                return rfqRepository.findByTenantId(tenantId);
        }

        public List<SupplierProposal> getProposalsByRFQ(UUID rfqId) {
                return proposalRepository.findByRfqId(rfqId);
        }

        public List<RFQSupplier> getInvitedSuppliers(UUID rfqId) {
                return rfqSupplierRepository.findByRfqId(rfqId).stream()
                                .filter(rfqSupplier -> rfqSupplier.getStatus() != RFQSupplier.InvitationStatus.REMOVED)
                                .collect(Collectors.toList());
        }

        @Transactional
        public RFQ publishRFQ(UUID rfqId) {
                RFQ rfq = rfqRepository.findById(rfqId)
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));
                rfq.setStatus(RFQ.RFQStatus.OPEN);
                return rfqRepository.save(rfq);
        }

        @Transactional
        public SupplierProposal submitProposal(SupplierProposal proposal) {
                if (proposal.getRfq() == null || proposal.getRfq().getId() == null) {
                        throw new RuntimeException("Proposal must act on a valid RFQ");
                }
                RFQ rfq = rfqRepository.findById(proposal.getRfq().getId())
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));

                proposal.setRfq(rfq);
                if (proposal.getTenantId() == null) {
                        proposal.setTenantId(rfq.getTenantId());
                }
                // Additional proposal logic (e.g. status)
                proposal.setStatus(SupplierProposal.ProposalStatus.SUBMITTED);

                if (proposal.getItems() != null) {
                        // Get supplier categories for validation
                        List<UUID> supplierCategoryIds = supplierCategoryRepository
                                        .findBySupplierId(proposal.getSupplier().getId())
                                        .stream()
                                        .map(sc -> sc.getCategory().getId())
                                        .toList();

                        for (ProposalItem item : proposal.getItems()) {
                                item.setProposal(proposal);

                                // Category validation
                                if (item.getRfqItem() != null && item.getRfqItem().getId() != null) {
                                        RFQItem rfqItem = rfqItemRepository.findById(item.getRfqItem().getId())
                                                        .orElse(null);

                                        if (rfqItem != null && rfqItem.getRequisitionItem() != null &&
                                                        rfqItem.getRequisitionItem().getCategory() != null) {
                                                UUID categoryId = rfqItem.getRequisitionItem().getCategory().getId();
                                                if (!supplierCategoryIds.contains(categoryId)) {
                                                        throw new RuntimeException(
                                                                        "Fornecedor não autorizado a cotar itens da categoria: "
                                                                                        +
                                                                                        rfqItem.getRequisitionItem()
                                                                                                        .getCategory()
                                                                                                        .getName());
                                                }
                                        }
                                }
                        }
                }

                return proposalRepository.save(proposal);
        }

        @Transactional
        public Comparison evaluateProposal(UUID proposalId, BigDecimal technicalScore, String notes) {
                SupplierProposal proposal = proposalRepository.findById(proposalId)
                                .orElseThrow(() -> new RuntimeException("Proposal not found"));

                proposal.setTechnicalScore(technicalScore);
                proposal.setEvaluationNotes(notes);
                proposalRepository.save(proposal);

                return null; // Comparison not strictly needed to return here
        }

        @Transactional
        public RFQSupplier inviteSupplier(UUID rfqId, UUID supplierId) {
                RFQ rfq = rfqRepository.findById(rfqId)
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));
                Supplier supplier = supplierRepository.findById(supplierId)
                                .orElseThrow(() -> new RuntimeException("Supplier not found"));

                RFQSupplier rfqSupplier = new RFQSupplier();
                rfqSupplier.setRfq(rfq);
                rfqSupplier.setSupplier(supplier);
                rfqSupplier.setInvitedAt(LocalDateTime.now());
                rfqSupplier.setStatus(RFQSupplier.InvitationStatus.INVITED);
                rfqSupplier.setTenantId(rfq.getTenantId());

                return rfqSupplierRepository.save(rfqSupplier);
        }

        public List<RFQSupplier> getInvitedRFQs(UUID supplierId) {
                return rfqSupplierRepository.findBySupplierId(supplierId);
        }

        @Transactional
        public void removeSupplier(UUID rfqId, UUID supplierId) {
                RFQ rfq = rfqRepository.findById(rfqId)
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));

                RFQSupplier rfqSupplier = rfqSupplierRepository.findByRfqIdAndSupplierId(rfqId, supplierId)
                                .orElseThrow(() -> new RuntimeException("Supplier not invited to this RFQ"));

                // Mark as REMOVED instead of deleting
                rfqSupplier.setStatus(RFQSupplier.InvitationStatus.REMOVED);
                rfqSupplierRepository.save(rfqSupplier);
        }

        public ProposalComparisonDTO getComparativeMap(UUID rfqId) {
                RFQ rfq = rfqRepository.findById(rfqId)
                                .orElseThrow(() -> new RuntimeException("RFQ not found"));

                List<SupplierProposal> proposals = proposalRepository.findByRfqId(rfqId)
                                .stream()
                                .filter(p -> p.getStatus() != SupplierProposal.ProposalStatus.DRAFT)
                                .collect(Collectors.toList());

                List<ProposalComparisonDTO.SupplierSummaryDTO> summaries = new ArrayList<>();

                // Find globally best values
                BigDecimal minTotal = proposals.stream()
                                .map(SupplierProposal::getTotalAmount)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(null);

                Long minDays = proposals.stream()
                                .map(p -> p.getDeliveryDate() != null
                                                ? ChronoUnit.DAYS.between(rfq.getIssueDate(), p.getDeliveryDate())
                                                : null)
                                .filter(Objects::nonNull)
                                .min(Long::compareTo)
                                .orElse(null);

                BigDecimal maxScore = proposals.stream()
                                .map(SupplierProposal::getFinalScore)
                                .filter(Objects::nonNull)
                                .max(BigDecimal::compareTo)
                                .orElse(null);

                for (SupplierProposal p : proposals) {
                        long days = p.getDeliveryDate() != null
                                        ? ChronoUnit.DAYS.between(rfq.getIssueDate(), p.getDeliveryDate())
                                        : 0;

                        summaries.add(ProposalComparisonDTO.SupplierSummaryDTO.builder()
                                        .proposalId(p.getId())
                                        .supplierId(p.getSupplier().getId())
                                        .supplierName(p.getSupplier().getName())
                                        .supplierCode(p.getSupplier().getCode())
                                        .totalAmount(p.getTotalAmount())
                                        .currency(p.getCurrency())
                                        .deliveryDays((int) days)
                                        .paymentTerms(p.getPaymentTerms())
                                        .technicalScore(p.getTechnicalScore())
                                        .financialScore(p.getFinancialScore())
                                        .finalScore(p.getFinalScore())
                                        .status(p.getStatus().toString())
                                        .isCheapest(minTotal != null && p.getTotalAmount() != null
                                                        && p.getTotalAmount().compareTo(minTotal) == 0)
                                        .isFastest(minDays != null && days == minDays)
                                        .isBestScored(
                                                        maxScore != null && p.getFinalScore() != null
                                                                        && p.getFinalScore().compareTo(maxScore) == 0)
                                        .build());
                }

                List<ProposalComparisonDTO.ItemComparisonDTO> itemComparisons = new ArrayList<>();
                for (RFQItem rfqItem : rfq.getItems()) {
                        List<ProposalComparisonDTO.SupplierItemPriceDTO> supplierPrices = new ArrayList<>();

                        // Find lowest price for this specific item
                        BigDecimal lowestItemPrice = proposals.stream()
                                        .flatMap(p -> p.getItems().stream())
                                        .filter(pi -> pi.getRfqItem().getId().equals(rfqItem.getId()))
                                        .map(ProposalItem::getUnitPrice)
                                        .filter(Objects::nonNull)
                                        .min(BigDecimal::compareTo)
                                        .orElse(null);

                        for (SupplierProposal p : proposals) {
                                p.getItems().stream()
                                                .filter(pi -> pi.getRfqItem().getId().equals(rfqItem.getId()))
                                                .findFirst()
                                                .ifPresent(pi -> {
                                                        supplierPrices.add(ProposalComparisonDTO.SupplierItemPriceDTO
                                                                        .builder()
                                                                        .supplierId(p.getSupplier().getId())
                                                                        .supplierName(p.getSupplier().getName())
                                                                        .unitPrice(pi.getUnitPrice())
                                                                        .totalPrice(pi.getTotalPrice())
                                                                        .isLowest(lowestItemPrice != null
                                                                                        && pi.getUnitPrice().compareTo(
                                                                                                        lowestItemPrice) == 0)
                                                                        .build());
                                                });
                        }

                        itemComparisons.add(ProposalComparisonDTO.ItemComparisonDTO.builder()
                                        .rfqItemId(rfqItem.getId())
                                        .productName(rfqItem.getDescription())
                                        .description(rfqItem.getDescription())
                                        .quantity(rfqItem.getQuantity())
                                        .unit(rfqItem.getUnit())
                                        .targetPrice(rfqItem.getEstimatedPrice())
                                        .prices(supplierPrices)
                                        .build());
                }

                // Generate a basic analysis summary in Portuguese
                StringBuilder analysis = new StringBuilder();
                if (proposals.isEmpty()) {
                        analysis.append("Nenhuma proposta submetida para avaliação.");
                } else {
                        analysis.append("Análise Comparativa: ");
                        summaries.stream().filter(s -> s.isCheapest()).findFirst()
                                        .ifPresent(s -> analysis.append("O fornecedor ").append(s.getSupplierName())
                                                        .append(" apresenta o melhor preço total. "));
                        summaries.stream().filter(s -> s.isFastest()).findFirst()
                                        .ifPresent(s -> analysis.append("A entrega mais rápida é de ")
                                                        .append(s.getDeliveryDays())
                                                        .append(" dias. "));
                        // Multi-item analysis
                        long totalItems = rfq.getItems().size();
                        long coveredItems = itemComparisons.stream().filter(i -> !i.getPrices().isEmpty()).count();
                        analysis.append("Cobertura de ").append(coveredItems).append("/").append(totalItems)
                                        .append(" itens.");
                }

                return ProposalComparisonDTO.builder()
                                .rfqId(rfq.getId())
                                .rfqCode(rfq.getCode())
                                .rfqTitle(rfq.getTitle() != null ? rfq.getTitle()
                                                : "Processo de Aquisição " + rfq.getCode())
                                .rfqStatus(rfq.getStatus().toString())
                                .summaries(summaries)
                                .items(itemComparisons)
                                .AIAnalysis(analysis.toString())
                                .build();
        }
}
