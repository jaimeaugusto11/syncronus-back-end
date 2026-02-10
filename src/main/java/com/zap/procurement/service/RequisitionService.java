package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RequisitionService {

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private PORequisitionRepository poRequisitionRepository;

    @Transactional(readOnly = true)
    public List<com.zap.procurement.dto.PurchaseOrderSummaryDTO> getPurchaseOrdersForRequisition(UUID requisitionId) {
        return poRequisitionRepository.findByRequisitionWithPOs(requisitionId).stream()
                .map(this::toPOSummary)
                .toList();
    }

    private com.zap.procurement.dto.PurchaseOrderSummaryDTO toPOSummary(PORequisition pr) {
        PurchaseOrder po = pr.getPurchaseOrder();
        com.zap.procurement.dto.PurchaseOrderSummaryDTO dto = new com.zap.procurement.dto.PurchaseOrderSummaryDTO();
        dto.setId(po.getId());
        dto.setCode(po.getCode());
        dto.setStatus(po.getStatus().toString());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setOrderDate(po.getOrderDate());
        if (po.getSupplier() != null) {
            dto.setSupplierName(po.getSupplier().getName());
        }
        return dto;
    }

    @Autowired
    private RequisitionApprovalRepository approvalRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BudgetLineRepository budgetLineRepository;

    @Transactional
    public Requisition createRequisition(Requisition requisition) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        requisition.setTenantId(tenantId);
        requisition.setCode(generateRequisitionCode(tenantId));
        requisition.setStatus(Requisition.RequisitionStatus.DRAFT);

        if (requisition.getItems() != null) {
            requisition.getItems().forEach(item -> item.setTenantId(tenantId));
        }

        BigDecimal total = requisition.getItems().stream()
                .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        requisition.setTotalAmount(total);

        if (requisition.getBudgetLine() == null) {
            requisition.setExtraordinary(true);
        }

        return requisitionRepository.save(requisition);
    }

    @Transactional
    public Requisition submitForApproval(UUID requisitionId) {
        Requisition requisition = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new RuntimeException("Requisition not found"));
        createApprovalWorkflow(requisition);
        requisition.setStatus(Requisition.RequisitionStatus.PENDING_APPROVAL);
        return requisitionRepository.save(requisition);
    }

    private void createApprovalWorkflow(Requisition requisition) {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<WorkflowTemplate> templates = workflowTemplateRepository.findByTenantId(tenantId);
        WorkflowTemplate workflow = templates.stream()
                .filter(w -> w.getType() == WorkflowTemplate.WorkflowType.REQUISITION_APPROVAL && w.getIsActive())
                .findFirst()
                .orElse(null);

        if (workflow == null) {
            createDefaultApprovalWorkflow(requisition);
        } else {
            createConfiguredApprovalWorkflow(requisition, workflow);
        }
    }

    private void createDefaultApprovalWorkflow(Requisition requisition) {
        Department dept = requisition.getDepartment();
        int level = 1;

        // 1. All flows start with Department Head
        if (dept != null && dept.getHead() != null) {
            createApproval(requisition, dept.getHead(), level++);
        }

        // 2. Extraordinary Flow: Needs General Director
        if (requisition.isExtraordinary()) {
            User generalDirector = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "ADMIN_GERAL".equalsIgnoreCase(u.getRole().getName()))
                    .findFirst()
                    .orElse(null);

            if (generalDirector != null) {
                createApproval(requisition, generalDirector, level++);
            }
        }

        // 3. Final Step: Procurement Managers
        List<User> procurementManagers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "GESTOR_PROCUREMENT".equalsIgnoreCase(u.getRole().getName()))
                .toList();

        // Use a single level for all procurement managers (any one can approve)
        for (User manager : procurementManagers) {
            createApproval(requisition, manager, level);
        }
    }

    private void createConfiguredApprovalWorkflow(Requisition requisition, WorkflowTemplate workflow) {
        int level = 1;
        for (WorkflowStep step : workflow.getSteps()) {
            for (WorkflowApprover approver : step.getApprovers()) {
                User user = resolveApprover(approver, requisition);
                if (user != null) {
                    createApproval(requisition, user, level);
                }
            }
            level++;
        }
    }

    private User resolveApprover(WorkflowApprover approver, Requisition requisition) {
        if (approver.getUser() != null) {
            return approver.getUser();
        }
        Department dept = requisition.getDepartment();
        if (approver.getIsDepartmentHead() != null && approver.getIsDepartmentHead()) {
            return dept.getHead();
        }
        return null;
    }

    private void createApproval(Requisition requisition, User approver, int level) {
        RequisitionApproval approval = new RequisitionApproval();
        approval.setRequisition(requisition);
        approval.setApprover(approver);
        approval.setLevel(level);
        approval.setStatus(RequisitionApproval.ApprovalStatus.PENDING);
        approval.setTenantId(requisition.getTenantId());
        approvalRepository.save(approval);
    }

    @Transactional
    public void processApproval(UUID approvalId, String action, String comments, UUID delegateToUserId) {
        RequisitionApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new RuntimeException("Approval not found"));

        if (approval.getStatus() != RequisitionApproval.ApprovalStatus.PENDING) {
            throw new RuntimeException("This approval is no longer pending.");
        }

        approval.setActionDate(LocalDateTime.now());
        approval.setComments(comments);
        Requisition requisition = approval.getRequisition();

        switch (action.toUpperCase()) {
            case "APPROVE":
                approval.setStatus(RequisitionApproval.ApprovalStatus.APPROVED);
                // Mark other people at the SAME level as SKIPPED (Any-of logic)
                List<RequisitionApproval> levelApprovals = approvalRepository.findByRequisitionId(requisition.getId())
                        .stream()
                        .filter(a -> a.getLevel().equals(approval.getLevel())
                                && a.getStatus() == RequisitionApproval.ApprovalStatus.PENDING)
                        .filter(a -> !a.getId().equals(approval.getId()))
                        .toList();

                for (RequisitionApproval other : levelApprovals) {
                    other.setStatus(RequisitionApproval.ApprovalStatus.SKIPPED);
                    other.setComments("Aprovado por " + approval.getApprover().getName());
                    other.setActionDate(LocalDateTime.now());
                    approvalRepository.save(other);
                }

                checkAndProgressRequisition(requisition);
                break;
            case "REJECT":
                approval.setStatus(RequisitionApproval.ApprovalStatus.REJECTED);
                requisition.setStatus(Requisition.RequisitionStatus.REJECTED);
                requisitionRepository.save(requisition);

                // Mark ALL other PENDING approvals as SKIPPED
                List<RequisitionApproval> allPending = approvalRepository.findByRequisitionId(requisition.getId())
                        .stream()
                        .filter(a -> a.getStatus() == RequisitionApproval.ApprovalStatus.PENDING)
                        .filter(a -> !a.getId().equals(approval.getId()))
                        .toList();

                for (RequisitionApproval other : allPending) {
                    other.setStatus(RequisitionApproval.ApprovalStatus.SKIPPED);
                    other.setComments("Rejeitado por " + approval.getApprover().getName());
                    other.setActionDate(LocalDateTime.now());
                    approvalRepository.save(other);
                }
                break;
            case "DELEGATE":
                if (delegateToUserId != null) {
                    User delegateTo = userRepository.findById(delegateToUserId)
                            .orElseThrow(() -> new RuntimeException("Delegate user not found"));
                    approval.setStatus(RequisitionApproval.ApprovalStatus.DELEGATED);
                    createApproval(requisition, delegateTo, approval.getLevel());
                }
                break;
        }
        approvalRepository.save(approval);
    }

    private void checkAndProgressRequisition(Requisition requisition) {
        List<RequisitionApproval> allApprovals = approvalRepository.findByRequisitionId(requisition.getId());

        int maxLevel = allApprovals.stream()
                .mapToInt(RequisitionApproval::getLevel)
                .max()
                .orElse(0);

        boolean allLevelsCompleted = true;
        for (int l = 1; l <= maxLevel; l++) {
            final int level = l;
            boolean levelApproved = allApprovals.stream()
                    .filter(a -> a.getLevel() == level)
                    .anyMatch(a -> a.getStatus() == RequisitionApproval.ApprovalStatus.APPROVED);

            if (!levelApproved) {
                allLevelsCompleted = false;
                break;
            }
        }

        if (allLevelsCompleted) {
            requisition.setStatus(Requisition.RequisitionStatus.APPROVED);
            requisitionRepository.save(requisition);
        }
    }

    private String generateRequisitionCode(UUID tenantId) {
        long timestamp = System.currentTimeMillis() % 10000;
        long count = requisitionRepository.findByTenantId(tenantId).size();
        return String.format("REQ-%d-%04d-%04d", java.time.Year.now().getValue(), count + 1, timestamp);
    }

    public List<Requisition> getRequisitionsByTenant(UUID tenantId) {
        return requisitionRepository.findByTenantId(tenantId);
    }

    public List<Requisition> getMyRequisitions(UUID userId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return requisitionRepository.findByRequesterId(userId).stream()
                .filter(r -> r.getTenantId().equals(tenantId))
                .toList();
    }

    public List<RequisitionApproval> getPendingApprovals(UUID userId) {
        List<RequisitionApproval> userApprovals = approvalRepository.findByApproverIdAndStatus(userId,
                RequisitionApproval.ApprovalStatus.PENDING);

        return userApprovals.stream()
                .filter(this::isApprovalActive)
                .toList();
    }

    private boolean isApprovalActive(RequisitionApproval approval) {
        Requisition req = approval.getRequisition();
        if (req.getStatus() != Requisition.RequisitionStatus.PENDING_APPROVAL) {
            return false;
        }

        List<RequisitionApproval> allReqApprovals = approvalRepository.findByRequisitionId(req.getId());

        for (int l = 1; l < approval.getLevel(); l++) {
            final int level = l;
            boolean levelApproved = allReqApprovals.stream()
                    .filter(a -> a.getLevel() == level)
                    .anyMatch(a -> a.getStatus() == RequisitionApproval.ApprovalStatus.APPROVED);

            if (!levelApproved) {
                return false;
            }
        }
        return true;
    }

    public List<RequisitionApproval> getApprovalsByRequisition(UUID requisitionId) {
        return approvalRepository.findByRequisitionId(requisitionId);
    }
}
