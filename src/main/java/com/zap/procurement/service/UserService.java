package com.zap.procurement.service;

import com.zap.procurement.domain.Department;
import com.zap.procurement.domain.Tenant;
import com.zap.procurement.domain.User;
import com.zap.procurement.repository.DepartmentRepository;
import com.zap.procurement.repository.TenantRepository;
import com.zap.procurement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zap.procurement.domain.Role;
import com.zap.procurement.repository.RoleRepository;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailWithRole(email);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(passwordEncoder.encode("password123")); // Default password
        }
        return userRepository.save(user);
    }

    @Transactional
    public User createDemoUser(String email, String roleName, UUID tenantId) {
        Tenant tenant;
        if (tenantId != null) {
            tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        } else {
            // Picking the first available tenant or creating a default one
            tenant = tenantRepository.findAll().stream().findFirst().orElseGet(() -> {
                Tenant newTenant = new Tenant();
                newTenant.setName("Demo Organization");
                newTenant.setCode("DEMO");
                newTenant.setStatus(Tenant.TenantStatus.ACTIVE);
                newTenant.setSubscriptionPlan(Tenant.SubscriptionPlan.FREE);
                return tenantRepository.save(newTenant);
            });
        }

        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).get();
        }

        User user = new User();
        user.setEmail(email);
        user.setName("Demo User: " + email.split("@")[0]);
        user.setPassword(passwordEncoder.encode("password"));

        if (roleName != null) {
            Role role = roleRepository.findByNameAndTenantId(roleName, tenant.getId()).orElse(null);
            if (role != null) {
                user.setRole(role);
                // Force initialization of permissions if lazily loaded (though it's EAGER now)
                role.getPermissions().size();
            }
        }

        user.setTenantId(tenant.getId());
        user.setStatus(User.UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    public java.util.List<User> findAll(UUID tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getTenantId().equals(tenantId))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public User updateUser(UUID id, com.zap.procurement.dto.UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getName() != null)
            user.setName(dto.getName());
        if (dto.getRole() != null) {
            String roleIdentifier = dto.getRole();
            Optional<Role> roleOpt = roleRepository.findByNameAndTenantId(roleIdentifier, user.getTenantId());
            if (roleOpt.isEmpty()) {
                roleOpt = roleRepository.findBySlugAndTenantId(roleIdentifier, user.getTenantId());
            }
            Role role = roleOpt.orElseThrow(() -> new RuntimeException("Papel não encontrado: " + roleIdentifier));
            user.setRole(role);
        }

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            user.setDepartment(dept);
        }

        if (dto.getStatus() != null) {
            user.setStatus(User.UserStatus.valueOf(dto.getStatus()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
    }
}
