package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Department;
import com.zap.procurement.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Department> departments = departmentRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/root")
    public ResponseEntity<List<Department>> getRootDepartments() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Department> departments = departmentRepository.findByTenantIdAndParentIsNull(tenantId);
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartment(@PathVariable java.util.UUID id) {
        return departmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            department.setTenantId(tenantId);
        } else if (department.getTenantId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable java.util.UUID id,
            @RequestBody Department department) {
        return departmentRepository.findById(id)
                .map(existing -> {
                    department.setId(id);
                    Department updated = departmentRepository.save(department);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
