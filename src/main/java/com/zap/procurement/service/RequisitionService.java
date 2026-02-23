package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.zap.procurement.dto.CheckoutCartDTO;

@Service
public class RequisitionService {

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private PORequisitionRepository poRequisitionRepository;

    @Autowired
    private CatalogItemRepository catalogItemRepository;

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

        // Propagate status to items
        if (requisition.getItems() != null) {
            requisition.getItems()
                    .forEach(item -> item.setStatus(RequisitionItem.RequisitionItemStatus.PENDING_APPROVAL));
        }

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
        UUID tenantId = requisition.getTenantId();
        Department dept = requisition.getDepartment();
        int level = 1;

        // 1. All flows start with Department Head
        if (dept != null && dept.getHead() != null) {
            User head = dept.getHead();
            // If requester is the head, they shouldn't approve their own request
            if (head.getId().equals(requisition.getRequester().getId())) {
                // Try parent department head
                if (dept.getParent() != null && dept.getParent().getHead() != null) {
                    createApproval(requisition, dept.getParent().getHead(), level++);
                } else {
                    // Skip this level if no alternative head found
                    System.out.println(
                            "[RequisitionService] Requester is Dept Head and no parent head found. Skipping level 1.");
                }
            } else {
                createApproval(requisition, head, level++);
            }
        }

        // 2. Extraordinary Flow: Needs General Director
        if (requisition.isExtraordinary()) {
            // Find a user with DIRETOR_GERAL or ADMIN_GERAL role in THIS tenant
            User generalDirector = userRepository.findUsersByRoleNameAndTenantId("DIRETOR_GERAL", tenantId)
                    .stream()
                    .findFirst()
                    .orElse(userRepository.findUsersByRoleNameAndTenantId("ADMIN_GERAL", tenantId)
                            .stream()
                            .findFirst()
                            .orElse(userRepository.findAll().stream()
                                    .filter(u -> u.getRole() != null
                                            && ("DIRETOR_GERAL".equalsIgnoreCase(u.getRole().getName())
                                                    || "ADMIN_GERAL".equalsIgnoreCase(u.getRole().getName())))
                                    .findFirst()
                                    .orElse(null)));

            if (generalDirector != null) {
                // Extraordinary requisitions need the DG after the Dept Head
                createApproval(requisition, generalDirector, level++);
            }
        }

        // 3. Final Step: Prior logic added Procurement Managers here.
        // REQ CHANGE: Procurement Managers should NOT be approvers.
        // Once business approval (Dept Head / Director) is done, it goes to APPROVED
        // state
        // and becomes available for Sourcing.

        // So we remove the code that added GESTOR_PROCUREMENT as approvers.
    }

    private void createConfiguredApprovalWorkflow(Requisition requisition, WorkflowTemplate workflow) {
        int level = 1;
        for (WorkflowStep step : workflow.getSteps()) {
            boolean levelCreated = false;
            for (WorkflowApprover approver : step.getApprovers()) {
                User user = resolveApprover(approver, requisition);
                if (user != null) {
                    createApproval(requisition, user, level);
                    levelCreated = true;
                }
            }
            if (levelCreated) {
                level++;
            }
        }

        // If no levels were created via template, fallback to default
        if (level == 1) {
            createDefaultApprovalWorkflow(requisition);
        }
    }

    private User resolveApprover(WorkflowApprover approver, Requisition requisition) {
        if (approver.getUser() != null) {
            return approver.getUser();
        }
        Department dept = requisition.getDepartment();
        if (approver.getIsDepartmentHead() != null && approver.getIsDepartmentHead() && dept != null) {
            User head = dept.getHead();

            // If requester is the head, they shouldn't approve their own request
            if (head != null && head.getId().equals(requisition.getRequester().getId())) {
                // Try parent department head
                if (dept.getParent() != null) {
                    return dept.getParent().getHead();
                }
                return null; // Skip if no parent dept
            }
            return head;
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

                // Propagate status to items
                if (requisition.getItems() != null) {
                    requisition.getItems()
                            .forEach(item -> item.setStatus(RequisitionItem.RequisitionItemStatus.CANCELLED));
                }

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

        int currentActiveLevel = 1;
        boolean allLevelsCompleted = true;

        for (int l = 1; l <= maxLevel; l++) {
            final int level = l;
            List<RequisitionApproval> levelApprovals = allApprovals.stream()
                    .filter(a -> a.getLevel() == level)
                    .toList();

            boolean levelApproved = levelApprovals.stream()
                    .anyMatch(a -> a.getStatus() == RequisitionApproval.ApprovalStatus.APPROVED
                            || a.getStatus() == RequisitionApproval.ApprovalStatus.SKIPPED);

            if (!levelApproved) {
                // Optimization: If the current user just approved level - 1, and is also in
                // level, auto-approve
                UUID currentUserId = TenantContext.getCurrentUser();
                if (currentUserId != null) {
                    for (RequisitionApproval current : levelApprovals) {
                        if (current.getStatus() == RequisitionApproval.ApprovalStatus.PENDING &&
                                current.getApprover().getId().equals(currentUserId)) {

                            // Check if they were the ones who approved the previous level (or any level
                            // before)
                            // Actually, just being in the next level is enough if we want to streamline
                            current.setStatus(RequisitionApproval.ApprovalStatus.APPROVED);
                            current.setActionDate(LocalDateTime.now());
                            current.setComments("Auto-aprovado (mesmo aprovador do nível anterior)");
                            approvalRepository.save(current);
                            checkAndProgressRequisition(requisition); // Recursive call
                            return;
                        }
                    }
                }

                allLevelsCompleted = false;
                currentActiveLevel = level;
                break;
            }
        }

        if (allLevelsCompleted) {
            requisition.setStatus(Requisition.RequisitionStatus.APPROVED);

            // Propagate status to items - they are now ready for Sourcing!
            if (requisition.getItems() != null) {
                requisition.getItems().forEach(item -> item.setStatus(RequisitionItem.RequisitionItemStatus.APPROVED));
            }
        } else {
            // Set more granular status based on level if using default workflow levels
            if (currentActiveLevel == 1) {
                requisition.setStatus(Requisition.RequisitionStatus.DEPT_HEAD_APPROVAL);
            } else if (currentActiveLevel == 2 && requisition.isExtraordinary()) {
                requisition.setStatus(Requisition.RequisitionStatus.GENERAL_DIRECTOR_APPROVAL);
            } else {
                requisition.setStatus(Requisition.RequisitionStatus.PENDING_APPROVAL);
            }
        }
        requisitionRepository.save(requisition);
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
        Requisition.RequisitionStatus status = req.getStatus();

        // Allow all statuses indicate approval is in progress
        boolean isPending = status == Requisition.RequisitionStatus.PENDING_APPROVAL ||
                status == Requisition.RequisitionStatus.DEPT_HEAD_APPROVAL ||
                status == Requisition.RequisitionStatus.DEPT_DIRECTOR_APPROVAL ||
                status == Requisition.RequisitionStatus.GENERAL_DIRECTOR_APPROVAL;

        if (!isPending) {
            return false;
        }

        List<RequisitionApproval> allReqApprovals = approvalRepository.findByRequisitionId(req.getId());

        for (int l = 1; l < approval.getLevel(); l++) {
            final int level = l;
            boolean levelCompleted = allReqApprovals.stream()
                    .filter(a -> a.getLevel() == level)
                    .anyMatch(a -> a.getStatus() == RequisitionApproval.ApprovalStatus.APPROVED
                            || a.getStatus() == RequisitionApproval.ApprovalStatus.SKIPPED);

            if (!levelCompleted) {
                return false;
            }
        }
        return true;
    }

    @Autowired
    private RequisitionItemRepository requisitionItemRepository;

    public List<RequisitionItem> getApprovedItems() {
        UUID tenantId = TenantContext.getCurrentTenant();
        // We want items that are APPROVED but not yet in an RFQ (status APPROVED)
        // Requisition itself must be APPROVED or similar.
        return requisitionItemRepository.findByStatus(RequisitionItem.RequisitionItemStatus.APPROVED).stream()
                .filter(item -> item.getTenantId().equals(tenantId))
                .toList();
    }

    public List<RequisitionItem> getApprovedItemsByCategory(UUID categoryId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return requisitionItemRepository
                .findApprovedItemsByCategory(categoryId, RequisitionItem.RequisitionItemStatus.APPROVED).stream()
                .filter(item -> item.getTenantId().equals(tenantId))
                .toList();
    }

    @Transactional
    public Requisition createFromCatalog(CheckoutCartDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        UUID userId = TenantContext.getCurrentUser();

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Departamento não encontrado"));

        Requisition requisition = new Requisition();
        requisition.setTenantId(tenantId);
        requisition.setCode(generateRequisitionCode(tenantId));
        requisition.setTitle(dto.getTitle());
        requisition.setJustification(dto.getJustification());
        requisition.setPriority(Requisition.Priority.valueOf(dto.getPriority()));
        requisition.setRequester(requester);
        requisition.setDepartment(dept);
        requisition.setStatus(Requisition.RequisitionStatus.DRAFT);

        if (dto.getBudgetLineId() != null) {
            BudgetLine bl = budgetLineRepository.findById(dto.getBudgetLineId()).orElse(null);
            requisition.setBudgetLine(bl);
            if (bl == null)
                requisition.setExtraordinary(true);
        } else {
            requisition.setExtraordinary(true);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<RequisitionItem> reqItems = new ArrayList<>();

        for (CheckoutCartDTO.CartItemDTO cartItem : dto.getItems()) {
            CatalogItem catalogItem = catalogItemRepository.findById(cartItem.getCatalogItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "Item do catálogo não encontrado: " + cartItem.getCatalogItemId()));

            RequisitionItem item = new RequisitionItem();
            item.setRequisition(requisition);
            item.setDescription(catalogItem.getName());
            item.setQuantity(BigDecimal.valueOf(cartItem.getQuantity()));
            item.setEstimatedPrice(catalogItem.getPrice());
            item.setTotalPrice(catalogItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            item.setCategory(catalogItem.getCategory());
            item.setPreferredSupplier(catalogItem.getSupplier());
            item.setTenantId(tenantId);
            item.setStatus(RequisitionItem.RequisitionItemStatus.DRAFT);
            item.setUnit("UN"); // Default unit for catalog items

            reqItems.add(item);
            total = total.add(item.getTotalPrice());
        }

        requisition.setItems(reqItems);
        requisition.setTotalAmount(total);

        return requisitionRepository.save(requisition);
    }

    public List<RequisitionApproval> getApprovalsByRequisition(UUID requisitionId) {
        return approvalRepository.findByRequisitionId(requisitionId);
    }
}
