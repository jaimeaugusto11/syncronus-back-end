package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Permission;
import com.zap.procurement.domain.Role;
import com.zap.procurement.repository.PermissionRepository;
import com.zap.procurement.repository.RoleRepository;
import com.zap.procurement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(roleRepository.findByTenantId(tenantId));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody RoleDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();

        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setTenantId(tenantId);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = dto.getPermissionIds().stream()
                    .map(id -> permissionRepository.findById(id).orElse(null))
                    .filter(p -> p != null)
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);
        auditLogService.log("ROLE", saved.getId(), "CREATE", "Created role: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable UUID id, @RequestBody RoleDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (!role.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to role");
        }

        if (role.isSystem()) {
            throw new RuntimeException("System roles cannot be modified");
        }

        String oldValue = "Name: " + role.getName() + ", Description: " + role.getDescription();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());

        if (dto.getPermissionIds() != null) {
            Set<Permission> permissions = dto.getPermissionIds().stream()
                    .map(pid -> permissionRepository.findById(pid).orElse(null))
                    .filter(p -> p != null)
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);
        String newValue = "Name: " + saved.getName() + ", Description: " + saved.getDescription();
        auditLogService.log("ROLE", saved.getId(), "UPDATE", "Updated role: " + saved.getName(), oldValue, newValue);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (!role.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to role");
        }

        if (role.isSystem()) {
            throw new RuntimeException("System roles cannot be deleted");
        }

        roleRepository.deleteById(id);
        auditLogService.log("ROLE", id, "DELETE", "Deleted role: " + role.getName());
        return ResponseEntity.noContent().build();
    }

    public static class RoleDTO {
        private String name;
        private String description;
        private List<UUID> permissionIds;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<UUID> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<UUID> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}
