package com.zap.procurement.config;

import com.zap.procurement.domain.Permission;
import com.zap.procurement.domain.Role;
import com.zap.procurement.repository.PermissionRepository;
import com.zap.procurement.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class RBACSeeder implements CommandLineRunner {

        @Autowired
        private PermissionRepository permissionRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private com.zap.procurement.repository.TenantRepository tenantRepository;

        @Override
        public void run(String... args) throws Exception {
                com.zap.procurement.domain.Tenant defaultTenant = tenantRepository.findByCode("SYSTEM")
                                .orElseGet(() -> {
                                        com.zap.procurement.domain.Tenant t = new com.zap.procurement.domain.Tenant();
                                        t.setName("System Tenant");
                                        t.setCode("SYSTEM");
                                        t.setStatus(com.zap.procurement.domain.Tenant.TenantStatus.ACTIVE);
                                        return tenantRepository.save(t);
                                });

                if (permissionRepository.count() == 0) {
                        seedPermissions(defaultTenant.getId());
                }
                if (roleRepository.count() == 0) {
                        seedRoles(defaultTenant.getId());
                }
        }

        private void seedPermissions(UUID tenantId) {
                List<Permission> permissions = Arrays.asList(
                                createPermission("Ver Orçamentos", "VIEW_BUDGETS", "Permite visualizar orçamentos",
                                                "FINANCE",
                                                tenantId),
                                createPermission("Gerir Orçamentos", "MANAGE_BUDGETS",
                                                "Permite criar e editar orçamentos", "FINANCE",
                                                tenantId),
                                createPermission("Aprovar Requisições", "APPROVE_REQUISITIONS",
                                                "Permite aprovar requisições de compra",
                                                "PROCUREMENT", tenantId),
                                createPermission("Criar Requisições", "CREATE_REQUISITIONS",
                                                "Permite criar requisições de compra",
                                                "PROCUREMENT", tenantId),
                                createPermission("Gerir Fornecedores", "MANAGE_SUPPLIERS",
                                                "Permite gerir base de dados de fornecedores", "PROCUREMENT", tenantId),
                                createPermission("Ver Logs de Auditoria", "VIEW_AUDIT_LOGS",
                                                "Permite visualizar logs do sistema",
                                                "ADMIN", tenantId),
                                createPermission("Gerir Utilizadores", "MANAGE_USERS",
                                                "Permite gerir utilizadores e permissões",
                                                "ADMIN", tenantId));
                permissionRepository.saveAll(permissions);
        }

        private void seedRoles(UUID tenantId) {
                // Fetch all permissions to assign to ADMIN_GERAL
                List<Permission> allPerms = permissionRepository.findAll();
                Set<Permission> adminPerms = new HashSet<>(allPerms);

                // Procurement manager permissions (subset)
                Set<Permission> procurementPerms = new HashSet<>();
                permissionRepository.findBySlug("APPROVE_REQUISITIONS").ifPresent(procurementPerms::add);
                permissionRepository.findBySlug("CREATE_REQUISITIONS").ifPresent(procurementPerms::add);
                permissionRepository.findBySlug("MANAGE_SUPPLIERS").ifPresent(procurementPerms::add);

                // Requisitioner permissions
                Set<Permission> requisitionerPerms = new HashSet<>();
                permissionRepository.findBySlug("CREATE_REQUISITIONS").ifPresent(requisitionerPerms::add);

                roleRepository.saveAll(Arrays.asList(
                                createRole("ADMIN_GERAL", "Administrador Geral com acesso total", adminPerms, true,
                                                tenantId),
                                createRole("GESTOR_PROCUREMENT", "Gestor de Procurement e Compras", procurementPerms,
                                                true, tenantId),
                                createRole("REQUISITANTE", "Utilizador que pode criar requisições", requisitionerPerms,
                                                true,
                                                tenantId)));
        }

        private Role createRole(String name, String desc, Set<Permission> perms, boolean system, UUID tenantId) {
                Role r = new Role();
                r.setName(name);
                r.setSlug(name); // Use name as slug since it's already in slug format (ADMIN_GERAL)
                r.setDescription(desc);
                r.setPermissions(perms);
                r.setSystem(system);
                r.setTenantId(tenantId);
                return r;
        }

        private Permission createPermission(String name, String slug, String desc, String group, UUID tenantId) {
                Permission p = new Permission();
                p.setName(name);
                p.setSlug(slug);
                p.setDescription(desc);
                p.setGroup(group);
                p.setTenantId(tenantId);
                return p;
        }
}
