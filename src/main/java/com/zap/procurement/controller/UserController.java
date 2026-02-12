package com.zap.procurement.controller;

import com.zap.procurement.domain.Department;
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
import java.util.UUID;

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
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        UUID tenantId = com.zap.procurement.config.TenantContext.getCurrentTenant();
        user.setTenantId(tenantId);

        if (dto.getRole() != null) {
            Optional<com.zap.procurement.domain.Role> roleOpt = roleRepository.findByNameAndTenantId(dto.getRole(),
                    tenantId);
            if (roleOpt.isEmpty()) {
                roleOpt = roleRepository.findBySlugAndTenantId(dto.getRole(), tenantId);
            }
            roleOpt.ifPresent(user::setRole);
        }

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
            if (dept != null && dept.getTenantId().equals(tenantId)) {
                user.setDepartment(dept);
            }
        }

        User saved = userService.createUser(user);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PostMapping("/demo")
    public ResponseEntity<UserDTO> createDemoUser(@RequestBody DemoUserRequest request) {
        // Demo endpoint - likely public or protected by special key, but for now open
        // or just authenticated?
        // Let's leave it open or simple authenticated as it might be used for
        // onboarding.
        // Actually, if it's "demo", likely open.
        // But we put 'anyRequest().authenticated()' in SecurityConfig.
        // So user must be logged in?
        // Or we should allow it in SecurityConfig if it is for registration.
        // Assuming it requires at least a token, or if it's typically for initial
        // setup, maybe AllowAnonymous?
        // Use 'permitAll' in security config if needed. For now treating as
        // authenticated or skipped.
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
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_USERS')")
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

        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(401).build();
        }

        User user = userOpt.get();
        // Logs removed for safety and cleanliness

        UserDTO dto = toDTO(user);
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
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody UserDTO dto) {
        User updated = userService.updateUser(id, dto);
        return ResponseEntity.ok(toDTO(updated));
    }

    @Autowired
    private com.zap.procurement.config.RBACSeeder rbacSeeder;

    @GetMapping("/init-system")
    @org.springframework.security.access.prepost.PreAuthorize("permitAll()")
    public ResponseEntity<String> initSystem() {
        try {
            // 1. Get current tenant from context (already set by JwtAuthenticationFilter)
            UUID tenantId = com.zap.procurement.config.TenantContext.getCurrentTenant();

            if (tenantId == null) {
                // Fallback: try to find admin.demo if no tenant in context
                Optional<User> adminOpt = userService.findByEmail("admin.demo@supplySinc.com");
                if (adminOpt.isPresent()) {
                    tenantId = adminOpt.get().getTenantId();
                } else {
                    return ResponseEntity.status(400).body("Could not determine tenant to initialize.");
                }
            }

            // 2. Seed RBAC for this tenant
            rbacSeeder.seedTenant(tenantId);

            // 3. Ensure the current user has correct role if they match system roles (even
            // if already assigned)
            UUID userId = com.zap.procurement.config.TenantContext.getCurrentUser();
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    // Update user role if it matches system roles by name (case insensitive)
                    String currentRoleName = user.getRole() != null ? user.getRole().getName() : "";

                    if (user.getRole() == null || currentRoleName.equalsIgnoreCase("APROVADOR")
                            || currentRoleName.equalsIgnoreCase("ADMIN_GERAL")) {
                        String targetRole = (currentRoleName.equalsIgnoreCase("APROVADOR")
                                || user.getEmail().contains("gestor"))
                                        ? "APROVADOR"
                                        : "ADMIN_GERAL";

                        roleRepository.findByNameAndTenantId(targetRole, user.getTenantId()).ifPresent(role -> {
                            user.setRole(role);
                            userRepository.save(user);
                        });
                    }
                });
            }

            return ResponseEntity.ok(
                    "System initialized for tenant " + tenantId + ". RBAC roles and permissions updated.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/debug/my-authorities")
    public ResponseEntity<?> getMyAuthorities() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return ResponseEntity.status(401).body("Not authenticated");

        return ResponseEntity.ok(java.util.Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .toList(),
                "details", auth.getPrincipal()));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_USERS')")
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
            // Use slug for consistent role identification (e.g. APROVADOR instead of
            // Aprovador)
            dto.setRole(user.getRole().getSlug());
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
