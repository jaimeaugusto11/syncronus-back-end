package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.BudgetLine;
import com.zap.procurement.domain.Department;
import com.zap.procurement.repository.BudgetLineRepository;
import com.zap.procurement.repository.DepartmentRepository;
import com.zap.procurement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/budget-lines")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BudgetLineController {

    private final BudgetLineRepository budgetLineRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<com.zap.procurement.dto.BudgetLineDTO>> getAllBudgets() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(budgetLineRepository.findByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<com.zap.procurement.dto.BudgetLineDTO>> getByDepartment(
            @PathVariable UUID departmentId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(budgetLineRepository.findByTenantIdAndDepartmentId(tenantId, departmentId).stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.BudgetLineDTO> createBudget(
            @RequestBody com.zap.procurement.dto.BudgetLineDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (!dept.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to department");
        }

        BudgetLine budget = new BudgetLine();
        budget.setCode(dto.getCode());
        budget.setDescription(dto.getDescription());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setEquipmentList(dto.getEquipmentList());
        budget.setDepartment(dept);
        budget.setTenantId(tenantId);
        budget.setActive(dto.getActive() != null ? dto.getActive() : true);

        BudgetLine saved = budgetLineRepository.save(budget);
        auditLogService.log("BUDGET_LINE", saved.getId(), "CREATE", "Created budget line: " + saved.getDescription());
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.BudgetLineDTO> updateBudget(@PathVariable UUID id,
            @RequestBody com.zap.procurement.dto.BudgetLineDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        BudgetLine budget = budgetLineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget line not found"));

        if (!budget.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to budget line");
        }

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            if (!dept.getTenantId().equals(tenantId)) {
                throw new RuntimeException("Unauthorized access to department");
            }
            budget.setDepartment(dept);
        }

        budget.setCode(dto.getCode());
        budget.setDescription(dto.getDescription());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setEquipmentList(dto.getEquipmentList());
        budget.setActive(dto.getActive() != null ? dto.getActive() : budget.isActive());

        BudgetLine updated = budgetLineRepository.save(budget);
        auditLogService.log("BUDGET_LINE", updated.getId(), "UPDATE",
                "Updated budget line: " + updated.getDescription());
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        BudgetLine budget = budgetLineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget line not found"));

        if (!budget.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to budget line");
        }

        budgetLineRepository.delete(budget);
        auditLogService.log("BUDGET_LINE", id, "DELETE", "Deleted budget line: " + budget.getDescription());
        return ResponseEntity.noContent().build();
    }

    private com.zap.procurement.dto.BudgetLineDTO toDTO(BudgetLine budget) {
        com.zap.procurement.dto.BudgetLineDTO dto = new com.zap.procurement.dto.BudgetLineDTO();
        dto.setId(budget.getId());
        dto.setCode(budget.getCode());
        dto.setDescription(budget.getDescription());
        dto.setTotalAmount(budget.getTotalAmount());
        dto.setSpentAmount(budget.getSpentAmount());
        dto.setRemainingAmount(budget.getRemainingAmount());
        dto.setEquipmentList(budget.getEquipmentList());
        dto.setActive(budget.isActive());
        if (budget.getDepartment() != null) {
            dto.setDepartmentId(budget.getDepartment().getId());
            dto.setDepartmentName(budget.getDepartment().getName());
        }
        return dto;
    }
}
