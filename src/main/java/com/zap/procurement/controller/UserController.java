package com.zap.procurement.controller;

import com.zap.procurement.domain.Department;
import com.zap.procurement.domain.Role;
import com.zap.procurement.domain.User;
import com.zap.procurement.dto.DemoUserRequest;
import com.zap.procurement.dto.UserDTO;
import com.zap.procurement.repository.DepartmentRepository;
import com.zap.procurement.repository.RoleRepository;
import com.zap.procurement.repository.UserRepository;
import com.zap.procurement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        UUID tenantId = com.zap.procurement.config.TenantContext.getCurrentTenant();
        user.setTenantId(tenantId);
        // Note: Role assignment should be handled via UserService.updateUser or
        // dedicated endpoint

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
            user.setDepartment(dept);
        }

        user.setTenantId(com.zap.procurement.config.TenantContext.getCurrentTenant());

        User saved = userService.createUser(user);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PostMapping("/demo")
    public ResponseEntity<UserDTO> createDemoUser(@RequestBody DemoUserRequest request) {
        String roleName = request.getRole();

        String email = request.getEmail() != null ? request.getEmail()
                : "demo-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        User user = userService.createDemoUser(email, roleName, request.getTenantId());

        UserDTO response = new UserDTO();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        if (user.getRole() != null) {
            response.setRole(user.getRole().getName());
        }
        if (user.getDepartment() != null) {
            response.setDepartmentId(user.getDepartment().getId());
            response.setDepartmentName(user.getDepartment().getName());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<java.util.List<UserDTO>> getAllUsers() {
        // Assume default tenant for now or get from context
        java.util.UUID tenantId = com.zap.procurement.config.TenantContext.getCurrentTenant();
        java.util.List<User> users = userService.findAll(tenantId);

        java.util.List<UserDTO> dtos = users.stream().map(this::toDTO).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        // Extract email from JWT token
        String token = authHeader.replace("Bearer ", "");
        String email = extractEmailFromToken(token);

        System.out.println("[UserController] /users/me called with email: " + email);

        if (email == null) {
            System.out.println("[UserController] Email is null, returning 401");
            return ResponseEntity.status(401).build();
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (!userOpt.isPresent()) {
            System.out.println("[UserController] User not found for email: " + email);
            return ResponseEntity.status(401).build();
        }

        User user = userOpt.get();
        System.out.println("[UserController] User found: " + user.getEmail());
        System.out
                .println("[UserController] User role: " + (user.getRole() != null ? user.getRole().getName() : "NULL"));
        System.out.println("[UserController] User role object: " + user.getRole());

        UserDTO dto = toDTO(user);
        System.out.println("[UserController] DTO role: " + dto.getRole());

        return ResponseEntity.ok(dto);
    }

    private String extractEmailFromToken(String token) {
        // Simple JWT parsing - in production use proper JWT library
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Extract "sub" field which contains the email
                int subIndex = payload.indexOf("\"sub\":\"");
                if (subIndex != -1) {
                    int start = subIndex + 7;
                    int end = payload.indexOf("\"", start);
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            // Return null if parsing fails
        }
        return null;
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody UserDTO dto) {
        User updated = userService.updateUser(id, dto);
        return ResponseEntity.ok(toDTO(updated));
    }

    @GetMapping("/init-system")
    public ResponseEntity<String> initSystem() {
        try {
            // 1. Find admin.demo user
            Optional<User> userOpt = userService.findByEmail("admin.demo@supplySinc.com");
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(404).body("User admin.demo@supplySinc.com not found");
            }
            User user = userOpt.get();
            UUID tenantId = user.getTenantId();

            // 2. Ensure Permissions exist (Global or per tenant? Model says per tenant in
            // Seeder)
            // For simplicity, we assume permissions are seeded. If not, we should seed
            // them.
            // But permissions in RBACSeeder seem to be created with a specific tenantId.
            // Let's check if we need to create permissions for this tenant.

            // 3. Ensure Roles exist for this tenant
            ensureRoleExists("ADMIN_GERAL", "Administrador Geral com acesso total", tenantId, true);
            ensureRoleExists("GESTOR_PROCUREMENT", "Gestor de Procurement e Compras", tenantId, true);
            ensureRoleExists("REQUISITANTE", "Utilizador que pode criar requisições", tenantId, true);

            // 4. Assign ADMIN_GERAL to user
            Role adminRole = roleRepository.findByNameAndTenantId("ADMIN_GERAL", tenantId)
                    .orElseThrow(() -> new RuntimeException("ADMIN_GERAL role could not be created/found"));

            user.setRole(adminRole);
            userRepository.save(user);

            return ResponseEntity.ok(
                    "System initialized for tenant " + tenantId + ". User " + user.getEmail() + " is now ADMIN_GERAL.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private Role ensureRoleExists(String name, String description, UUID tenantId, boolean isSystem) {
        return roleRepository.findByNameAndTenantId(name, tenantId)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(name);
                    newRole.setSlug(name);
                    newRole.setDescription(description);
                    newRole.setSystem(isSystem);
                    newRole.setTenantId(tenantId);
                    newRole.setActive(true);
                    // We could assign all permissions to ADMIN_GERAL here, but for now let's just
                    // create the role
                    // to unblock the login.
                    return roleRepository.save(newRole);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setTenantId(user.getTenantId());
        if (user.getRole() != null) {
            dto.setRole(user.getRole().getName());
            dto.setPermissions(user.getRole().getPermissions().stream()
                    .map(com.zap.procurement.domain.Permission::getSlug)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        dto.setStatus(user.getStatus().name());
        if (user.getDepartment() != null) {
            dto.setDepartmentId(user.getDepartment().getId());
            dto.setDepartmentName(user.getDepartment().getName());
        }
        return dto;
    }
}
