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

    @Autowired
    private com.zap.procurement.repository.UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<com.zap.procurement.dto.DepartmentDTO>> getAllDepartments() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Department> departments = departmentRepository.findByTenantId(tenantId);

        List<com.zap.procurement.dto.DepartmentDTO> dtos = departments.stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/root")
    public ResponseEntity<List<com.zap.procurement.dto.DepartmentDTO>> getRootDepartments() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Department> departments = departmentRepository.findByTenantIdAndParentIsNull(tenantId);

        List<com.zap.procurement.dto.DepartmentDTO> dtos = departments.stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.zap.procurement.dto.DepartmentDTO> getDepartment(@PathVariable java.util.UUID id) {
        return departmentRepository.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<com.zap.procurement.dto.UserDTO>> getDepartmentUsers(@PathVariable java.util.UUID id) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        // Since we don't have a direct repository method for findByDepartmentId yet,
        // let's assume one or filter
        // Actually UserService usually handles this. But for now let's use
        // UserRepository if possible or filter a findAll
        // Best practice: add findByDepartmentId to UserRepository. For now, let's
        // filter all users (inefficient but works for MVP)

        List<com.zap.procurement.domain.User> users = userRepository.findAll().stream()
                .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(id))
                .filter(u -> u.getTenantId().equals(tenantId)) // Security check
                .collect(java.util.stream.Collectors.toList());

        List<com.zap.procurement.dto.UserDTO> dtos = users.stream()
                .map(this::toUserDTO)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/head")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.DepartmentDTO> assignHead(@PathVariable java.util.UUID id,
            @RequestBody java.util.Map<String, String> body) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        String userIdStr = body.get("userId");
        if (userIdStr == null) {
            return ResponseEntity.badRequest().build();
        }

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (!department.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to department");
        }

        if (userIdStr.isEmpty()) {
            department.setHead(null);
        } else {
            java.util.UUID userId = java.util.UUID.fromString(userIdStr);
            com.zap.procurement.domain.User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getTenantId().equals(tenantId)) {
                throw new RuntimeException("Unauthorized access to user");
            }
            department.setHead(user);
        }

        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.DepartmentDTO> createDepartment(
            @RequestBody com.zap.procurement.dto.DepartmentDTO dto) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        Department department = new Department();
        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setCostCenter(dto.getCostCenter());
        if (dto.getBudgetType() != null) {
            department.setBudgetType(Department.BudgetType.valueOf(dto.getBudgetType()));
        }
        department.setTenantId(tenantId);
        department.setActive(true);

        if (dto.getHeadId() != null) {
            userRepository.findById(dto.getHeadId()).ifPresent(department::setHead);
        }

        if (dto.getParentId() != null) {
            departmentRepository.findById(dto.getParentId()).ifPresent(department::setParent);
        }

        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.DepartmentDTO> updateDepartment(@PathVariable java.util.UUID id,
            @RequestBody com.zap.procurement.dto.DepartmentDTO dto) {
        return departmentRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setDescription(dto.getDescription());
                    existing.setCostCenter(dto.getCostCenter());
                    if (dto.getBudgetType() != null) {
                        existing.setBudgetType(Department.BudgetType.valueOf(dto.getBudgetType()));
                    }
                    existing.setActive(dto.isActive());

                    if (dto.getHeadId() != null) {
                        userRepository.findById(dto.getHeadId()).ifPresent(existing::setHead);
                    } else {
                        existing.setHead(null);
                    }

                    if (dto.getParentId() != null) {
                        departmentRepository.findById(dto.getParentId()).ifPresent(existing::setParent);
                    } else {
                        existing.setParent(null);
                    }

                    Department updated = departmentRepository.save(existing);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private com.zap.procurement.dto.DepartmentDTO toDTO(Department dept) {
        com.zap.procurement.dto.DepartmentDTO dto = new com.zap.procurement.dto.DepartmentDTO();
        dto.setId(dept.getId());
        dto.setName(dept.getName());
        dto.setDescription(dept.getDescription());
        dto.setCostCenter(dept.getCostCenter());
        if (dept.getBudgetType() != null)
            dto.setBudgetType(dept.getBudgetType().name());
        dto.setActive(dept.isActive());

        if (dept.getHead() != null) {
            dto.setHeadId(dept.getHead().getId());
            dto.setHeadName(dept.getHead().getName());
            dto.setHeadAvatarUrl(dept.getHead().getAvatarUrl());
        }

        if (dept.getParent() != null) {
            dto.setParentId(dept.getParent().getId());
        }

        // Count members - N+1 issue, optimization needed for production but acceptable
        // for MVP
        long count = userRepository.findByTenantId(TenantContext.getCurrentTenant()).stream()
                .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(dept.getId()))
                .count();
        dto.setMemberCount(count);

        return dto;
    }

    private com.zap.procurement.dto.UserDTO toUserDTO(com.zap.procurement.domain.User user) {
        com.zap.procurement.dto.UserDTO dto = new com.zap.procurement.dto.UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setTenantId(user.getTenantId());
        if (user.getRole() != null) {
            dto.setRole(user.getRole().getSlug());
        }
        dto.setStatus(user.getStatus().name());
        return dto;
    }
}
