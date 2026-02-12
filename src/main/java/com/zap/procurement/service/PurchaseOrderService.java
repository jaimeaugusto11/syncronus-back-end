package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import com.zap.procurement.dto.POApprovalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private POApprovalRepository poApprovalRepository;

    @Autowired
    private SupplierProposalRepository proposalRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PORequisitionRepository poRequisitionRepository;

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private RequisitionItemRepository requisitionItemRepository;

    @Autowired
    private RFQItemRepository rfqItemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Transactional
    public PurchaseOrder createPO(PurchaseOrder po, User createdBy) {
        UUID tenantId = TenantContext.getCurrentTenant();

        po.setTenantId(tenantId);
        po.setCreatedBy(createdBy);
        po.setCode(generatePOCode(tenantId));
        po.setStatus(PurchaseOrder.POStatus.DRAFT);

        // Set tenant ID for all items
        if (po.getItems() != null) {
            for (POItem item : po.getItems()) {
                item.setPurchaseOrder(po);
            }
        }

        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder updatePO(UUID poId, PurchaseOrder updatedPO) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        // Only allow updates if status is DRAFT
        if (po.getStatus() != PurchaseOrder.POStatus.DRAFT) {
            throw new RuntimeException("Can only update POs in DRAFT status");
        }

        // Update fields
        po.setSupplier(updatedPO.getSupplier());
        po.setOrderDate(updatedPO.getOrderDate());
        po.setExpectedDeliveryDate(updatedPO.getExpectedDeliveryDate());
        po.setDeliveryAddress(updatedPO.getDeliveryAddress());
        po.setPaymentTerms(updatedPO.getPaymentTerms());
        po.setTermsAndConditions(updatedPO.getTermsAndConditions());
        po.setNotes(updatedPO.getNotes());
        po.setTotalAmount(updatedPO.getTotalAmount());

        // Update items
        if (updatedPO.getItems() != null) {
            po.getItems().clear();
            for (POItem item : updatedPO.getItems()) {
                item.setPurchaseOrder(po);
                po.getItems().add(item);
            }
        }

        return poRepository.save(po);
    }

    @Transactional
    public void deletePO(UUID poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        // Only allow deletion if status is DRAFT
        if (po.getStatus() != PurchaseOrder.POStatus.DRAFT) {
            throw new RuntimeException("Can only delete POs in DRAFT status");
        }

        poRepository.delete(po);
    }

    @Transactional
    public PurchaseOrder createPOFromAwardedItems(UUID supplierId, List<UUID> rfqItemIds, User createdBy) {
        UUID tenantId = TenantContext.getCurrentTenant();

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(tenantId);
        po.setSupplier(supplier);
        po.setCreatedBy(createdBy);
        po.setCode(generatePOCode(tenantId));
        po.setOrderDate(java.time.LocalDate.now());
        // Expected delivery date? Maybe max of items or default?
        // po.setExpectedDeliveryDate(...);
        po.setTotalAmount(BigDecimal.ZERO); // Recalculate from items
        po.setCurrency("EUR"); // Default or from supplier/items?
        po.setStatus(PurchaseOrder.POStatus.DRAFT);

        PurchaseOrder savedPO = poRepository.save(po);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (UUID rfqItemId : rfqItemIds) {
            RFQItem rfqItem = rfqItemRepository.findById(rfqItemId)
                    .orElseThrow(() -> new RuntimeException("RFQItem not found: " + rfqItemId));

            POItem poItem = new POItem();

            // We need the AWARDED price.
            // In the "Per Item" award model, we should have this info.
            if (rfqItem.getAwardedProposalItem() != null) {
                ProposalItem winningItem = rfqItem.getAwardedProposalItem();
                if (!winningItem.getProposal().getSupplier().getId().equals(supplierId)) {
                    throw new RuntimeException(
                            "Item " + rfqItem.getDescription() + " was awarded to a different supplier.");
                }
                poItem.setUnitPrice(winningItem.getUnitPrice());
                poItem.setTotalPrice(winningItem.getTotalPrice());
            } else {
                // Fallback if not explicitly marked (legacy or pre-award logic?)
                // For now, allow estimated price but maybe log warning or assume strictly
                // awarded.
                // Given the method name "createPOFromAwardedItems", we should expect them to be
                // awarded.
                // But for development/migration robustness:
                poItem.setUnitPrice(rfqItem.getEstimatedPrice());
                poItem.setTotalPrice(rfqItem.getEstimatedPrice().multiply(rfqItem.getQuantity()));
            }

            poItem.setPurchaseOrder(savedPO);
            poItem.setDescription(rfqItem.getDescription());
            poItem.setQuantity(rfqItem.getQuantity());

            poItem.setSourceRfqItem(rfqItem);
            if (rfqItem.getRequisitionItem() != null) {
                poItem.setSourceRequisitionItem(rfqItem.getRequisitionItem());

                // Link Requisition
                Requisition req = rfqItem.getRequisitionItem().getRequisition();
                if (req != null) {
                    boolean alreadyLinked = savedPO.getRequisitions().stream()
                            .anyMatch(pr -> pr.getRequisition().getId().equals(req.getId()));
                    if (!alreadyLinked) {
                        savedPO.addRequisition(req, BigDecimal.ZERO);
                    }
                }
            }
            savedPO.getItems().add(poItem);
            totalAmount = totalAmount.add(poItem.getTotalPrice());
        }

        savedPO.setTotalAmount(totalAmount);
        return poRepository.save(savedPO);
    }

    @Transactional
    public PurchaseOrder createPOFromProposal(UUID proposalId, User createdBy) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        SupplierProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(tenantId);
        // po.setRfq(proposal.getRfq()); // Deprecated, don't set or set for legacy
        // reference?
        // proposal.getRfq() is 1 RFQ.

        po.setSupplier(proposal.getSupplier());
        po.setCreatedBy(createdBy);
        po.setCode(generatePOCode(tenantId));
        po.setOrderDate(java.time.LocalDate.now());
        po.setExpectedDeliveryDate(proposal.getDeliveryDate());
        po.setTotalAmount(proposal.getTotalAmount());
        po.setCurrency(proposal.getCurrency());
        po.setPaymentTerms(proposal.getPaymentTerms());
        po.setStatus(PurchaseOrder.POStatus.DRAFT);

        PurchaseOrder savedPO = poRepository.save(po);

        // Copy items from proposal and Link with Requisitions via Items
        for (ProposalItem propItem : proposal.getItems()) {
            POItem poItem = new POItem();
            poItem.setPurchaseOrder(savedPO);
            poItem.setDescription(propItem.getRfqItem().getDescription());
            poItem.setQuantity(propItem.getRfqItem().getQuantity());
            poItem.setUnitPrice(propItem.getUnitPrice());
            poItem.setTotalPrice(propItem.getTotalPrice());

            // Link sources
            poItem.setSourceRfqItem(propItem.getRfqItem());
            if (propItem.getRfqItem().getRequisitionItem() != null) {
                poItem.setSourceRequisitionItem(propItem.getRfqItem().getRequisitionItem());

                // Link Requisition to PO (Header level)
                Requisition req = propItem.getRfqItem().getRequisitionItem().getRequisition();
                if (req != null) {
                    // Check if already linked
                    boolean alreadyLinked = savedPO.getRequisitions().stream()
                            .anyMatch(pr -> pr.getRequisition().getId().equals(req.getId()));

                    if (!alreadyLinked) {
                        savedPO.addRequisition(req, BigDecimal.ZERO); // Quantity fulfilled calculation is complex,
                                                                      // keeping 0 or updating later
                    }

                    // Construct fulfiled quantity logic?
                }
            }

            savedPO.getItems().add(poItem);
        }

        return poRepository.save(savedPO);
    }

    @Transactional
    public void linkRequisitionsToPO(UUID poId, List<com.zap.procurement.dto.RequisitionLinkDTO> links) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        for (com.zap.procurement.dto.RequisitionLinkDTO link : links) {
            Requisition req = requisitionRepository.findById(link.getRequisitionId())
                    .orElseThrow(() -> new RuntimeException("Requisition not found: " + link.getRequisitionId()));

            if (poRequisitionRepository.existsByPurchaseOrderIdAndRequisitionId(poId, link.getRequisitionId())) {
                continue; // Skip if already linked
            }

            po.addRequisition(req, link.getQuantityFulfilled());
        }

        poRepository.save(po);
    }

    @Transactional
    public void unlinkRequisition(UUID poId, UUID requisitionId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        po.getRequisitions().removeIf(pr -> pr.getRequisition().getId().equals(requisitionId));
        poRepository.save(po);
    }

    public List<com.zap.procurement.dto.RequisitionSummaryDTO> getRequisitionsForPO(UUID poId) {
        return poRequisitionRepository.findByPOWithRequisitions(poId).stream()
                .map(this::toRequisitionSummary)
                .toList();
    }

    private com.zap.procurement.dto.RequisitionSummaryDTO toRequisitionSummary(PORequisition pr) {
        Requisition req = pr.getRequisition();
        com.zap.procurement.dto.RequisitionSummaryDTO dto = new com.zap.procurement.dto.RequisitionSummaryDTO();
        dto.setId(req.getId());
        dto.setCode(req.getCode());
        dto.setTitle(req.getTitle());
        dto.setStatus(req.getStatus().toString());
        dto.setTotalAmount(req.getTotalAmount());
        dto.setQuantityFulfilled(pr.getQuantityFulfilled());
        if (req.getRequester() != null) {
            dto.setRequesterName(req.getRequester().getName());
        }
        if (req.getDepartment() != null) {
            dto.setDepartmentName(req.getDepartment().getName());
        }
        return dto;
    }

    @Transactional
    public PurchaseOrder submitForApproval(UUID poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        createApprovalWorkflow(po);
        po.setStatus(PurchaseOrder.POStatus.PENDING_APPROVAL);
        return poRepository.save(po);
    }

    private void createApprovalWorkflow(PurchaseOrder po) {
        // Requirement: 3 approvals before PO follows its course
        // 1. Department Head (from initial requisition if possible)
        // 2. Financial/Admin Director
        // 3. General Director

        int level = 1;

        // Level 1: Functional/Department Approval
        // IMPORTANT: Use the department of the original requisition, not the PO creator
        Department targetDept = null;
        if (po.getRequisitions() != null && !po.getRequisitions().isEmpty()) {
            Requisition req = po.getRequisitions().get(0).getRequisition();
            if (req != null) {
                targetDept = req.getDepartment();
            }
        }

        // Fallback to creator's dept if no requisition linked
        if (targetDept == null && po.getCreatedBy() != null) {
            targetDept = po.getCreatedBy().getDepartment();
        }

        if (targetDept != null && targetDept.getHead() != null) {
            User head = targetDept.getHead();
            // If creator is the head, skip to next level or use parent head (simplified for
            // now)
            createPOApproval(po, head, level++);
        }

        // Fallback for Level 1 if no head found
        if (level == 1) {
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "GESTOR_PROCUREMENT".equalsIgnoreCase(u.getRole().getName()))
                    .findFirst()
                    .ifPresent(u -> createPOApproval(po, u, 1));
            level++;
        }

        // Level 2 & 3: Financial & General Directors
        List<User> directors = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ADMIN_GERAL".equalsIgnoreCase(u.getRole().getName()))
                .toList();

        if (!directors.isEmpty()) {
            createPOApproval(po, directors.get(0), level++);

            User finalApprover = directors.size() > 1 ? directors.get(1) : directors.get(0);
            createPOApproval(po, finalApprover, level++);
        }

        // Ensure we have at least 3 levels
        while (level <= 3) {
            if (!directors.isEmpty()) {
                createPOApproval(po, directors.get(0), level++);
            } else {
                break;
            }
        }
    }

    private void createPOApproval(PurchaseOrder po, User approver, int level) {
        POApproval approval = new POApproval();
        approval.setPurchaseOrder(po);
        approval.setApprover(approver);
        approval.setLevel(level);
        approval.setStatus(POApproval.ApprovalStatus.PENDING);
        // Set tenantId if needed by BaseEntity/TenantContext
        approval.setTenantId(po.getTenantId());
        poApprovalRepository.save(approval);
    }

    @Transactional
    public void processApproval(UUID approvalId, String action, String comments) {
        POApproval approval = poApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("Approval not found"));

        approval.setActionDate(LocalDateTime.now());
        approval.setComments(comments);

        PurchaseOrder po = approval.getPurchaseOrder();

        switch (action.toUpperCase()) {
            case "APPROVE":
                approval.setStatus(POApproval.ApprovalStatus.APPROVED);
                progressPOApproval(po, approval.getLevel());
                break;
            case "REJECT":
                approval.setStatus(POApproval.ApprovalStatus.REJECTED);
                po.setStatus(PurchaseOrder.POStatus.REJECTED);
                poRepository.save(po);
                break;
        }

        poApprovalRepository.save(approval);
    }

    private void progressPOApproval(PurchaseOrder po, Integer currentLevel) {
        List<POApproval> approvals = poApprovalRepository.findByPurchaseOrderId(po.getId());

        // Check if current level is fully approved
        boolean currentLevelApproved = approvals.stream()
                .filter(a -> a.getLevel().equals(currentLevel))
                .allMatch(a -> a.getStatus() == POApproval.ApprovalStatus.APPROVED);

        if (!currentLevelApproved)
            return;

        // Check if there is a next level
        boolean nextLevelExists = approvals.stream()
                .anyMatch(a -> a.getLevel() > currentLevel);

        if (nextLevelExists) {
            // Optimization: If the next level approver is the same as the current one,
            // auto-approve it
            final int nextLevel = currentLevel + 1;
            UUID currentApproverId = approvals.stream()
                    .filter(a -> a.getLevel().equals(currentLevel)
                            && a.getStatus() == POApproval.ApprovalStatus.APPROVED)
                    .map(a -> a.getApprover().getId())
                    .findFirst()
                    .orElse(null);

            if (currentApproverId != null) {
                List<POApproval> nextApprovals = approvals.stream()
                        .filter(a -> a.getLevel().equals(nextLevel)
                                && a.getStatus() == POApproval.ApprovalStatus.PENDING)
                        .toList();

                for (POApproval next : nextApprovals) {
                    if (next.getApprover().getId().equals(currentApproverId)) {
                        next.setStatus(POApproval.ApprovalStatus.APPROVED);
                        next.setActionDate(LocalDateTime.now());
                        next.setComments("Auto-aprovado (mesmo aprovador do nível anterior)");
                        poApprovalRepository.save(next);
                        // Recursively progress
                        progressPOApproval(po, nextLevel);
                        return;
                    }
                }
            }
            return;
        }

        // All levels approved
        po.setStatus(PurchaseOrder.POStatus.APPROVED);

        // Synchronize linked requisitions status
        if (po.getRequisitions() != null) {
            for (PORequisition pr : po.getRequisitions()) {
                Requisition req = pr.getRequisition();
                if (req != null) {
                    req.setStatus(Requisition.RequisitionStatus.CONVERTED_TO_PO);
                    requisitionRepository.save(req);
                }
            }
        }

        poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder sendToSupplier(UUID poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        if (po.getStatus() != PurchaseOrder.POStatus.APPROVED) {
            throw new RuntimeException("PO must be approved first");
        }

        po.setStatus(PurchaseOrder.POStatus.SENT_TO_SUPPLIER);
        po.setSentToSupplierAt(LocalDateTime.now());
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder confirmBySupplier(UUID poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        po.setStatus(PurchaseOrder.POStatus.SUPPLIER_CONFIRMED);
        po.setSupplierConfirmedAt(LocalDateTime.now());
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder cancelPO(UUID poId, String reason) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        po.setStatus(PurchaseOrder.POStatus.CANCELLED);
        po.setNotes((po.getNotes() != null ? po.getNotes() + "\n" : "") + "Cancelled: " + reason);
        return poRepository.save(po);
    }

    private String generatePOCode(UUID tenantId) {
        long count = poRepository.findByTenantId(tenantId).size();
        return String.format("PO-%d-%04d", java.time.Year.now().getValue(), count + 1);
    }

    public List<PurchaseOrder> getPOsByTenant(UUID tenantId) {
        return poRepository.findByTenantId(tenantId);
    }

    public List<POApprovalDTO> getPendingApprovals(UUID userId) {
        return poApprovalRepository.findByApproverIdAndStatus(userId, POApproval.ApprovalStatus.PENDING)
                .stream()
                .filter(this::isPOApprovalActive)
                .map(this::toPOApprovalDTO)
                .toList();
    }

    private boolean isPOApprovalActive(POApproval approval) {
        PurchaseOrder po = approval.getPurchaseOrder();
        if (po.getStatus() != PurchaseOrder.POStatus.PENDING_APPROVAL) {
            return false;
        }

        List<POApproval> allApprovals = poApprovalRepository.findByPurchaseOrderId(po.getId());

        for (int l = 1; l < approval.getLevel(); l++) {
            final int level = l;
            boolean levelCompleted = allApprovals.stream()
                    .filter(a -> a.getLevel() == level)
                    .anyMatch(a -> a.getStatus() == POApproval.ApprovalStatus.APPROVED
                            || a.getStatus() == POApproval.ApprovalStatus.SKIPPED);

            if (!levelCompleted) {
                return false;
            }
        }
        return true;
    }

    private POApprovalDTO toPOApprovalDTO(POApproval approval) {
        PurchaseOrder po = approval.getPurchaseOrder();
        return POApprovalDTO.builder()
                .id(approval.getId())
                .level(approval.getLevel())
                .status(approval.getStatus().toString())
                .comments(approval.getComments())
                .actionDate(approval.getActionDate())
                .purchaseOrderId(po.getId())
                .purchaseOrderCode(po.getCode())
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : "N/A")
                .departmentName(resolvePODepartmentName(po))
                .totalAmount(po.getTotalAmount() != null ? po.getTotalAmount().toString() : "0.00")
                .currency(po.getCurrency())
                .poCreatedAt(po.getCreatedAt())
                .approverId(approval.getApprover().getId())
                .approverName(approval.getApprover().getName())
                .build();
    }

    private String resolvePODepartmentName(PurchaseOrder po) {
        if (po.getRequisitions() != null && !po.getRequisitions().isEmpty()) {
            Requisition req = po.getRequisitions().get(0).getRequisition();
            if (req != null && req.getDepartment() != null) {
                return req.getDepartment().getName();
            }
        }
        if (po.getCreatedBy() != null && po.getCreatedBy().getDepartment() != null) {
            return po.getCreatedBy().getDepartment().getName();
        }
        return "N/A";
    }
}
