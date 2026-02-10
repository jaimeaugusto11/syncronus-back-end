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
    public ResponseEntity<List<BudgetLine>> getAllBudgets() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(budgetLineRepository.findByTenantId(tenantId));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<BudgetLine>> getByDepartment(@PathVariable UUID departmentId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(budgetLineRepository.findByTenantIdAndDepartmentId(tenantId, departmentId));
    }

    @PostMapping
    public ResponseEntity<BudgetLine> createBudget(@RequestBody BudgetLineDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

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
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetLine> updateBudget(@PathVariable UUID id, @RequestBody BudgetLineDTO dto) {
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

        String oldValue = "Description: " + budget.getDescription() + ", Amount: " + budget.getTotalAmount()
                + ", Active: " + budget.isActive();
        budget.setDescription(dto.getDescription());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setEquipmentList(dto.getEquipmentList());
        budget.setActive(dto.getActive() != null ? dto.getActive() : budget.isActive());

        BudgetLine saved = budgetLineRepository.save(budget);
        String newValue = "Description: " + saved.getDescription() + ", Amount: " + saved.getTotalAmount()
                + ", Active: " + saved.isActive();
        auditLogService.log("BUDGET_LINE", saved.getId(), "UPDATE", "Updated budget line: " + saved.getDescription(),
                oldValue, newValue);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        BudgetLine budget = budgetLineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget line not found"));

        if (!budget.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to budget line");
        }

        budgetLineRepository.deleteById(id);
        auditLogService.log("BUDGET_LINE", id, "DELETE", "Deleted budget line");
        return ResponseEntity.noContent().build();
    }

    // Inner DTO for convenience
    public static class BudgetLineDTO {
        private String code;
        private String description;
        private java.math.BigDecimal totalAmount;
        private String equipmentList;
        private UUID departmentId;
        private Boolean active;

        // Getters and Setters
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public java.math.BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(java.math.BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getEquipmentList() {
            return equipmentList;
        }

        public void setEquipmentList(String equipmentList) {
            this.equipmentList = equipmentList;
        }

        public UUID getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(UUID departmentId) {
            this.departmentId = departmentId;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }
}
