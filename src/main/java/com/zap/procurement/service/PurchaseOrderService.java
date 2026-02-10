package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public PurchaseOrder createPOFromProposal(UUID proposalId, User createdBy) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        SupplierProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        PurchaseOrder po = new PurchaseOrder();
        // po.setTenant(tenant);
        po.setTenantId(tenantId);
        po.setRfq(proposal.getRfq());
        po.setSupplier(proposal.getSupplier());
        po.setCreatedBy(createdBy);
        po.setCode(generatePOCode(tenantId));
        po.setOrderDate(java.time.LocalDate.now());
        po.setExpectedDeliveryDate(proposal.getDeliveryDate());
        po.setTotalAmount(proposal.getTotalAmount());
        po.setCurrency(proposal.getCurrency());
        po.setPaymentTerms(proposal.getPaymentTerms());
        po.setStatus(PurchaseOrder.POStatus.DRAFT);

        // Link with original requisition from RFQ
        if (proposal.getRfq() != null && proposal.getRfq().getRequisition() != null) {
            po.addRequisition(proposal.getRfq().getRequisition(), null);
        }

        // Copy items from proposal
        for (ProposalItem propItem : proposal.getItems()) {
            POItem poItem = new POItem();
            poItem.setPurchaseOrder(po);
            // poItem.setProposalItem(propItem); // Assuming relationship exists
            poItem.setDescription(propItem.getRfqItem().getDescription()); // Assuming indirect link logic
            // poItem.setSpecifications(propItem.getRfqItem().getSpecifications());
            poItem.setQuantity(propItem.getRfqItem().getQuantity()); // Using BigDecimal directly
            // poItem.setUnit(propItem.getRfqItem().getUnit());
            poItem.setUnitPrice(propItem.getUnitPrice());
            poItem.setTotalPrice(propItem.getTotalPrice());
            po.getItems().add(poItem);
        }

        return poRepository.save(po);
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
        // 1. Department Head (from initial requisition if possible, otherwise
        // requester's dept head)
        // 2. Financial/Admin Director
        // 3. General Director

        int level = 1;

        // Level 1: Functional/Department Approval
        // Try to find the dept head of the user who created the PO
        if (po.getCreatedBy() != null && po.getCreatedBy().getDepartment() != null) {
            User deptHead = po.getCreatedBy().getDepartment().getHead();
            if (deptHead != null) {
                createPOApproval(po, deptHead, level++);
            }
        }
        // Fallback for Level 1 if no dept head found: Any Manager
        if (level == 1) {
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "GESTOR_PROCUREMENT".equalsIgnoreCase(u.getRole().getName()))
                    .findFirst()
                    .ifPresent(u -> createPOApproval(po, u, 1));
            level++;
        }

        // Level 2: Financial/Admin
        // For now, assigning to users with ADMIN_GERAL role as placeholders for
        // Financial Director
        List<User> directors = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ADMIN_GERAL".equalsIgnoreCase(u.getRole().getName()))
                .toList();

        if (!directors.isEmpty()) {
            createPOApproval(po, directors.get(0), level++);
        }

        // Level 3: Final Approval (e.g. CEO or Top Director)
        // Using the same pool for now but ensuring distinct levels
        if (!directors.isEmpty()) {
            // If we have more than 1 admin, use the second one, else reuse the first (demo
            // purpose)
            User finalApprover = directors.size() > 1 ? directors.get(1) : directors.get(0);
            createPOApproval(po, finalApprover, level++);
        }

        // Ensure we explicitly have 3 levels created if possible in this demo env
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
            // Requisition/PO stays in PENDING_APPROVAL, next level approver takes action
            // In a real system we might send notifications here
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

    public List<POApproval> getPendingApprovals(UUID userId) {
        return poApprovalRepository.findByApproverIdAndStatus(userId, POApproval.ApprovalStatus.PENDING);
    }
}
