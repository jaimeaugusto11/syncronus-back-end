package com.zap.procurement.controller;

import com.zap.procurement.config.RBACSeeder;
import com.zap.procurement.domain.Department;
import com.zap.procurement.domain.Role;
import com.zap.procurement.domain.Tenant;
import com.zap.procurement.domain.User;
import com.zap.procurement.dto.SetupRequestDTO;
import com.zap.procurement.repository.DepartmentRepository;
import com.zap.procurement.repository.RoleRepository;
import com.zap.procurement.repository.TenantRepository;
import com.zap.procurement.repository.UserRepository;
import com.zap.procurement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/setup")
@CrossOrigin(origins = "http://localhost:3000")
public class SetupController {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RBACSeeder rbacSeeder;

    @Autowired
    private UserService userService;

    @PostMapping("/initial")
    @Transactional
    public ResponseEntity<?> initialSetup(@RequestBody SetupRequestDTO request) {
        try {
            // 1. Validation
            if (tenantRepository.findByCode(request.getTenantCode()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Um tenant com este código já existe."));
            }

            if (userRepository.existsByEmail(request.getAdminEmail())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Um utilizador com este email já existe."));
            }

            // 2. Create Tenant
            Tenant tenant = new Tenant();
            tenant.setName(request.getTenantName());
            tenant.setCode(request.getTenantCode());
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
            tenant.setSubscriptionPlan(Tenant.SubscriptionPlan.ENTERPRISE);
            tenant = tenantRepository.save(tenant);

            // 3. Seed RBAC for this tenant
            rbacSeeder.seedTenant(tenant.getId());

            // 4. Create first Department
            Department dept = new Department();
            dept.setName("Administração");
            dept.setDescription("Departamento administrativo inicial");
            dept.setTenantId(tenant.getId());
            dept.setActive(true);
            dept = departmentRepository.save(dept);

            // 5. Create Admin User
            User admin = new User();
            admin.setName(request.getAdminName());
            admin.setEmail(request.getAdminEmail());
            admin.setPassword(request.getAdminPassword() != null ? request.getAdminPassword() : "admin123");
            admin.setTenantId(tenant.getId());
            admin.setDepartment(dept);
            admin.setStatus(User.UserStatus.ACTIVE);

            // Find ADMIN_GERAL role for this tenant
            Optional<Role> adminRole = roleRepository.findByNameAndTenantId("ADMIN_GERAL", tenant.getId());
            if (adminRole.isEmpty()) {
                adminRole = roleRepository.findBySlugAndTenantId("ADMIN_GERAL", tenant.getId());
            }

            if (adminRole.isPresent()) {
                admin.setRole(adminRole.get());
            } else {
                throw new RuntimeException("Papel ADMIN_GERAL não encontrado após o seeding.");
            }

            userService.createUser(admin);

            return ResponseEntity.ok(Map.of(
                    "message", "Configuração inicial concluída com sucesso.",
                    "tenantId", tenant.getId(),
                    "adminEmail", admin.getEmail()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Erro durante a configuração: " + e.getMessage()));
        }
    }
}
