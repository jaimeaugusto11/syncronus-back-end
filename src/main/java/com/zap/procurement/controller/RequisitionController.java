package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.dto.ApprovalActionDTO;
import com.zap.procurement.dto.ApprovalDTO;
import com.zap.procurement.dto.PendingApprovalDTO;
import com.zap.procurement.dto.RequisitionDTO;
import com.zap.procurement.dto.RequisitionItemDTO;
import com.zap.procurement.repository.*;
import com.zap.procurement.service.RequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requisitions")
@CrossOrigin(origins = "*")
public class RequisitionController {

    @Autowired
    private RequisitionService requisitionService;

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BudgetLineRepository budgetLineRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_REQUISITIONS')")
    public ResponseEntity<List<RequisitionDTO>> getAllRequisitions() {
        UUID tenantId = TenantContext.getCurrentTenant();
        System.out.println("[RequisitionController] Getting all requisitions for tenant: " + tenantId);

        if (tenantId == null) {
            System.out.println("[RequisitionController] Tenant ID is NULL!");
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        List<Requisition> requisitions = requisitionService.getRequisitionsByTenant(tenantId);
        System.out.println("[RequisitionController] Found " + requisitions.size() + " requisitions");

        return ResponseEntity.ok(requisitions.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/my")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_REQUISITIONS')")
    public ResponseEntity<List<RequisitionDTO>> getMyRequisitions(@RequestParam UUID userId) {
        List<Requisition> requisitions = requisitionService.getMyRequisitions(userId);
        return ResponseEntity.ok(requisitions.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_REQUISITIONS')")
    public ResponseEntity<RequisitionDTO> getRequisition(@PathVariable UUID id) {
        return requisitionRepository.findById(id)
                .map(req -> ResponseEntity.ok(this.toDTO(req)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_REQUISITION')")
    public ResponseEntity<RequisitionDTO> createRequisition(@RequestBody RequisitionDTO dto) {
        Requisition requisition = new Requisition();

        // Map fields
        requisition.setTitle(dto.getTitle());
        requisition.setJustification(dto.getJustification()); // Using title/justification field
        if (dto.getPriority() != null) {
            requisition.setPriority(Requisition.Priority.valueOf(dto.getPriority()));
        }
        requisition.setNeededBy(dto.getNeededBy());
        requisition.setExtraordinary(dto.isExtraordinary());

        // Relationships
        if (dto.getDepartmentId() != null) {
            departmentRepository.findById(dto.getDepartmentId())
                    .ifPresent(requisition::setDepartment);
        }

        if (dto.getBudgetLineId() != null) {
            budgetLineRepository.findById(dto.getBudgetLineId())
                    .ifPresent(requisition::setBudgetLine);
        }

        // Items
        if (dto.getItems() != null) {
            List<RequisitionItem> items = dto.getItems().stream().map(itemDto -> {
                RequisitionItem item = new RequisitionItem();
                item.setDescription(itemDto.getDescription());
                item.setQuantity(java.math.BigDecimal.valueOf(itemDto.getQuantity()));
                item.setUnit(itemDto.getUnit());
                item.setEstimatedPrice(itemDto.getEstimatedUnitPrice());
                item.setTotalPrice(itemDto.getEstimatedTotalPrice());
                item.setRequisition(requisition);

                if (itemDto.getCategoryId() != null) {
                    categoryRepository.findById(itemDto.getCategoryId()).ifPresent(item::setCategory);
                }
                if (itemDto.getPreferredSupplierId() != null) {
                    supplierRepository.findById(itemDto.getPreferredSupplierId()).ifPresent(item::setPreferredSupplier);
                }

                return item;
            }).collect(Collectors.toList());
            requisition.setItems(items);
        }

        // Set Requester from Authenticated User
        UUID userId = TenantContext.getCurrentUser();
        if (userId != null) {
            userRepository.findById(userId).ifPresent(requisition::setRequester);
        } else {
            // Fallback only if no auth context (should verify why)
            User currentUser = userRepository.findAll().stream().findFirst().orElse(null);
            if (currentUser != null)
                requisition.setRequester(currentUser);
        }

        Requisition created = requisitionService.createRequisition(requisition);
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_REQUISITION')")
    public ResponseEntity<?> updateRequisition(@PathVariable UUID id, @RequestBody RequisitionDTO dto) {
        return requisitionRepository.findById(id)
                .map(requisition -> {
                    // Cannot edit APPROVED requisitions
                    if (requisition.getStatus() == Requisition.RequisitionStatus.APPROVED) {
                        return ResponseEntity.badRequest().body("Cannot edit approved requisitions");
                    }

                    // Update fields
                    requisition.setTitle(dto.getTitle());
                    requisition.setJustification(dto.getJustification());
                    if (dto.getPriority() != null) {
                        requisition.setPriority(Requisition.Priority.valueOf(dto.getPriority()));
                    }
                    requisition.setNeededBy(dto.getNeededBy());
                    requisition.setExtraordinary(dto.isExtraordinary());

                    // Update department if changed
                    if (dto.getDepartmentId() != null) {
                        departmentRepository.findById(dto.getDepartmentId())
                                .ifPresent(requisition::setDepartment);
                    }

                    // Update budget line if changed
                    if (dto.getBudgetLineId() != null) {
                        budgetLineRepository.findById(dto.getBudgetLineId())
                                .ifPresent(requisition::setBudgetLine);
                    }

                    // Update items - remove old ones and add new ones
                    if (dto.getItems() != null) {
                        requisition.getItems().clear();
                        List<RequisitionItem> items = dto.getItems().stream().map(itemDto -> {
                            RequisitionItem item = new RequisitionItem();
                            item.setDescription(itemDto.getDescription());
                            item.setQuantity(java.math.BigDecimal.valueOf(itemDto.getQuantity()));
                            item.setUnit(itemDto.getUnit());
                            item.setEstimatedPrice(itemDto.getEstimatedUnitPrice());
                            item.setTotalPrice(itemDto.getEstimatedTotalPrice());
                            item.setRequisition(requisition);
                            item.setTenantId(requisition.getTenantId());

                            if (itemDto.getCategoryId() != null) {
                                categoryRepository.findById(itemDto.getCategoryId()).ifPresent(item::setCategory);
                            }
                            if (itemDto.getPreferredSupplierId() != null) {
                                supplierRepository.findById(itemDto.getPreferredSupplierId())
                                        .ifPresent(item::setPreferredSupplier);
                            }

                            return item;
                        }).collect(Collectors.toList());
                        requisition.getItems().addAll(items);
                    }

                    Requisition updated = requisitionRepository.save(requisition);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_REQUISITION')")
    public ResponseEntity<?> deleteRequisition(@PathVariable UUID id) {
        return requisitionRepository.findById(id)
                .map(requisition -> {
                    // Cannot delete APPROVED requisitions
                    if (requisition.getStatus() == Requisition.RequisitionStatus.APPROVED) {
                        return ResponseEntity.badRequest().body("Cannot delete approved requisitions");
                    }

                    requisitionRepository.delete(requisition);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/submit")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_REQUISITION')")
    public ResponseEntity<RequisitionDTO> submitForApproval(@PathVariable UUID id) {
        Requisition requisition = requisitionService.submitForApproval(id);
        return ResponseEntity.ok(toDTO(requisition));
    }

    @GetMapping("/approvals/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('APPROVE_REQUISITION')")
    public ResponseEntity<List<PendingApprovalDTO>> getPendingApprovals(@RequestParam UUID userId) {
        List<RequisitionApproval> approvals = requisitionService.getPendingApprovals(userId);
        return ResponseEntity.ok(approvals.stream().map(this::toPendingApprovalDTO).collect(Collectors.toList()));
    }

    @PostMapping("/approvals/action")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('APPROVE_REQUISITION')")
    public ResponseEntity<?> processApproval(@RequestBody ApprovalActionDTO actionDTO) {
        requisitionService.processApproval(
                actionDTO.getApprovalId(),
                actionDTO.getAction(),
                actionDTO.getComments(),
                actionDTO.getDelegateToUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/approvals")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_REQUISITIONS')")
    public ResponseEntity<List<ApprovalDTO>> getRequisitionApprovals(@PathVariable UUID id) {
        List<RequisitionApproval> approvals = requisitionService.getApprovalsByRequisition(id);
        return ResponseEntity.ok(approvals.stream().map(this::toApprovalDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/purchase-orders")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_REQUISITIONS') or hasAuthority('VIEW_POS')")
    public ResponseEntity<List<com.zap.procurement.dto.PurchaseOrderSummaryDTO>> getLinkedPOs(@PathVariable UUID id) {
        return ResponseEntity.ok(requisitionService.getPurchaseOrdersForRequisition(id));
    }

    @GetMapping("/items/approved")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<List<RequisitionItemDTO>> getApprovedItems() {
        List<RequisitionItem> items = requisitionService.getApprovedItems();
        return ResponseEntity.ok(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
    }

    @GetMapping("/items/approved-by-category")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<List<RequisitionItemDTO>> getApprovedItemsByCategory(@RequestParam UUID categoryId) {
        List<RequisitionItem> items = requisitionService.getApprovedItemsByCategory(categoryId);
        return ResponseEntity.ok(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
    }

    private RequisitionDTO toDTO(Requisition req) {
        RequisitionDTO dto = new RequisitionDTO();
        dto.setId(req.getId());
        dto.setTitle(req.getTitle());
        dto.setCode(req.getCode());
        if (req.getRequester() != null) {
            dto.setRequesterId(req.getRequester().getId());
            dto.setRequesterName(req.getRequester().getName());
        }
        if (req.getDepartment() != null) {
            dto.setDepartmentId(req.getDepartment().getId());
            dto.setDepartmentName(req.getDepartment().getName());
        }
        // dto.setBudgetType(req.getBudgetType()); // If enum to string
        dto.setCostCenter(req.getCostCenter());
        dto.setTotalAmount(req.getTotalAmount());
        if (req.getStatus() != null) {
            dto.setStatus(req.getStatus().toString());
        }
        dto.setJustification(req.getJustification());
        if (req.getPriority() != null) {
            dto.setPriority(req.getPriority().toString());
        }
        dto.setNeededBy(req.getNeededBy());
        if (req.getCreatedAt() != null) {
            dto.setCreatedAt(req.getCreatedAt().toString());
        }

        if (req.getBudgetLine() != null) {
            dto.setBudgetLineId(req.getBudgetLine().getId());
            dto.setBudgetLineCode(req.getBudgetLine().getCode());
        }
        dto.setExtraordinary(req.isExtraordinary());

        if (req.getItems() != null) {
            dto.setItems(req.getItems().stream().map(this::toItemDTO).collect(Collectors.toList()));
        }

        return dto;
    }

    private RequisitionItemDTO toItemDTO(RequisitionItem item) {
        RequisitionItemDTO dto = new RequisitionItemDTO();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        if (item.getQuantity() != null)
            dto.setQuantity(item.getQuantity().intValue());
        dto.setEstimatedUnitPrice(item.getEstimatedPrice());
        dto.setEstimatedTotalPrice(item.getTotalPrice());

        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getId());
            dto.setCategoryName(item.getCategory().getName());
        }
        if (item.getPreferredSupplier() != null) {
            dto.setPreferredSupplierId(item.getPreferredSupplier().getId());
            dto.setPreferredSupplierName(item.getPreferredSupplier().getName());
        }

        // Add metadata from Parent Requisition
        if (item.getRequisition() != null) {
            dto.setRequisitionCode(item.getRequisition().getCode());
            if (item.getRequisition().getRequester() != null) {
                dto.setRequesterName(item.getRequisition().getRequester().getName());
            }
            if (item.getRequisition().getDepartment() != null) {
                dto.setDepartmentName(item.getRequisition().getDepartment().getName());
            }
        }

        return dto;
    }

    private ApprovalDTO toApprovalDTO(RequisitionApproval approval) {
        ApprovalDTO dto = new ApprovalDTO();
        dto.setId(approval.getId());
        dto.setLevel(approval.getLevel());
        dto.setStatus(approval.getStatus().toString());
        dto.setComments(approval.getComments());
        if (approval.getActionDate() != null) {
            dto.setActionDate(approval.getActionDate().toString());
        }
        if (approval.getApprover() != null) {
            dto.setApproverId(approval.getApprover().getId());
            dto.setApproverName(approval.getApprover().getName());
            dto.setApproverEmail(approval.getApprover().getEmail());
            if (approval.getApprover().getRole() != null) {
                dto.setApproverRole(approval.getApprover().getRole().getName());
            }
        }
        return dto;
    }

    private PendingApprovalDTO toPendingApprovalDTO(RequisitionApproval approval) {
        PendingApprovalDTO dto = new PendingApprovalDTO();
        dto.setId(approval.getId());
        dto.setLevel(approval.getLevel());
        dto.setStatus(approval.getStatus().toString());

        // Approver info
        if (approval.getApprover() != null) {
            dto.setApproverId(approval.getApprover().getId());
            dto.setApproverName(approval.getApprover().getName());
            dto.setApproverEmail(approval.getApprover().getEmail());
            if (approval.getApprover().getRole() != null) {
                dto.setApproverRole(approval.getApprover().getRole().getName());
            }
        }

        // Requisition info
        if (approval.getRequisition() != null) {
            Requisition req = approval.getRequisition();
            dto.setRequisitionId(req.getId());
            dto.setRequisitionCode(req.getCode());
            dto.setJustification(req.getJustification());
            dto.setExtraordinary(req.isExtraordinary());
            dto.setCreatedAt(req.getCreatedAt() != null ? req.getCreatedAt().toString() : null);
            dto.setTotalAmount(req.getTotalAmount() != null ? req.getTotalAmount().toString() : "0.00");

            if (req.getRequester() != null) {
                dto.setRequesterName(req.getRequester().getName());
            }
            if (req.getDepartment() != null) {
                dto.setDepartmentName(req.getDepartment().getName());
            }
        }

        return dto;
    }
}
