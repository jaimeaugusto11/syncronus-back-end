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
@org.springframework.core.annotation.Order(10)
public class RBACSeeder implements CommandLineRunner {

        @Autowired
        private PermissionRepository permissionRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private com.zap.procurement.repository.TenantRepository tenantRepository;

        @Override
        public void run(String... args) throws Exception {
                // Ensure SYSTEM tenant exists
                com.zap.procurement.domain.Tenant systemTenant = tenantRepository.findByCode("SYSTEM")
                                .orElseGet(() -> {
                                        com.zap.procurement.domain.Tenant t = new com.zap.procurement.domain.Tenant();
                                        t.setName("System Tenant");
                                        t.setCode("SYSTEM");
                                        t.setStatus(com.zap.procurement.domain.Tenant.TenantStatus.ACTIVE);
                                        return tenantRepository.save(t);
                                });

                // Seed ALL tenants to ensure they all have permissions
                List<com.zap.procurement.domain.Tenant> tenants = tenantRepository.findAll();
                System.out.println("[RBACSeeder] Starting seeding for " + tenants.size() + " tenants...");
                for (com.zap.procurement.domain.Tenant tenant : tenants) {
                        try {
                                seedTenant(tenant.getId());
                        } catch (Exception e) {
                                System.err.println("[RBACSeeder] Error seeding tenant " + tenant.getCode() + ": "
                                                + e.getMessage());
                        }
                }
                System.out.println("[RBACSeeder] Global seeding process completed.");
        }

        @org.springframework.transaction.annotation.Transactional
        public void seedTenant(UUID tenantId) {
                // Ensure permissions exist
                seedPermissions(tenantId);

                // Always seed roles or update them if permissions changed
                seedRoles(tenantId);
        }

        private void seedPermissions(UUID tenantId) {
                List<Permission> permissions = Arrays.asList(
                                // Dashboard
                                createPermission("Ver Dashboard", "VIEW_DASHBOARD", "Acesso ao dashboard principal",
                                                "DASHBOARD", tenantId),

                                // Requisitions
                                createPermission("Ver Requisições", "VIEW_REQUISITIONS", "Ver lista de requisições",
                                                "REQUISITIONS", tenantId),
                                createPermission("Criar Requisições", "CREATE_REQUISITION", "Criar novas requisições",
                                                "REQUISITIONS", tenantId),
                                createPermission("Aprovar Requisições", "APPROVE_REQUISITION",
                                                "Aprovar/Rejeitar requisições", "REQUISITIONS", tenantId),

                                // RFQs
                                createPermission("Ver RFQs", "VIEW_RFQS", "Ver RFQs", "RFQS", tenantId),
                                createPermission("Gerir RFQs", "MANAGE_RFQS", "Criar, Publicar, Fechar RFQs", "RFQS",
                                                tenantId),

                                // Proposals
                                createPermission("Ver Propostas", "VIEW_PROPOSALS", "Ver propostas recebidas",
                                                "PROPOSALS", tenantId),

                                // Purchase Orders
                                createPermission("Ver Ordens de Compra", "VIEW_POS", "Ver Ordens de Compra",
                                                "PURCHASE_ORDERS", tenantId),
                                createPermission("Criar Ordens de Compra", "CREATE_PO", "Criar PO", "PURCHASE_ORDERS",
                                                tenantId),
                                createPermission("Gerir Ordens de Compra", "MANAGE_POS", "Gerir e Editar POs",
                                                "PURCHASE_ORDERS",
                                                tenantId),
                                createPermission("Aprovar Ordens de Compra", "APPROVE_PO", "Aprovar POs",
                                                "PURCHASE_ORDERS", tenantId),

                                // Goods Receipts
                                createPermission("Ver Recepções", "VIEW_GRN", "Ver Guias de Recepção", "GOODS_RECEIPTS",
                                                tenantId),
                                createPermission("Gerir Recepções", "MANAGE_GRN", "Criar/Aprovar GRNs",
                                                "GOODS_RECEIPTS", tenantId),

                                // Invoices
                                createPermission("Ver Faturas", "VIEW_INVOICES", "Ver Faturas", "INVOICES", tenantId),
                                createPermission("Gerir Faturas", "MANAGE_INVOICES", "Processar Faturas", "INVOICES",
                                                tenantId),

                                // Payments
                                createPermission("Ver Pagamentos", "VIEW_PAYMENTS", "Ver Pagamentos", "PAYMENTS",
                                                tenantId),
                                createPermission("Gerir Pagamentos", "MANAGE_PAYMENTS", "Processar Pagamentos",
                                                "PAYMENTS", tenantId),
                                
                                // Contracts
                                createPermission("Ver Contratos", "VIEW_CONTRACTS", "Ver lista de contratos", "CONTRACTS", tenantId),
                                createPermission("Gerir Contratos", "MANAGE_CONTRACTS", "Criar/Editar contratos", "CONTRACTS", tenantId),
                                createPermission("Assinar Contratos", "SIGN_CONTRACT", "Assinar contratos digitalmente", "CONTRACTS", tenantId),
                                
                                // Auctions
                                createPermission("Ver Leilões", "VIEW_AUCTIONS", "Ver leilões em tempo real", "AUCTIONS", tenantId),
                                createPermission("Gerir Leilões", "MANAGE_AUCTIONS", "Iniciar/Parar leilões", "AUCTIONS", tenantId),
                                createPermission("Efectuar Lance", "PLACE_BID", "Participar em leilões reversos", "AUCTIONS", tenantId),

                                // Suppliers
                                createPermission("Ver Fornecedores", "VIEW_SUPPLIERS",
                                                "Ver base de dados de fornecedores", "SUPPLIERS", tenantId),
                                createPermission("Gerir Fornecedores", "MANAGE_SUPPLIERS",
                                                "Adicionar/Editar Fornecedores", "SUPPLIERS", tenantId),

                                // Admin
                                createPermission("Acesso Admin", "ADMIN_ACCESS", "Acesso à área de Admin", "ADMIN",
                                                tenantId),
                                createPermission("Gerir Utilizadores", "MANAGE_USERS", "Gerir Utilizadores e Papéis",
                                                "ADMIN", tenantId),
                                createPermission("Gerir Definições", "MANAGE_SETTINGS",
                                                "Gerir Configurações do Sistema", "ADMIN", tenantId));

                for (Permission p : permissions) {
                        java.util.Optional<Permission> existingBySlug = permissionRepository
                                        .findBySlugAndTenantId(p.getSlug(), tenantId);
                        if (existingBySlug.isPresent()) {
                                Permission existing = existingBySlug.get();
                                existing.setName(p.getName());
                                existing.setDescription(p.getDescription());
                                existing.setGroup(p.getGroup());
                                permissionRepository.save(existing);
                        } else {
                                permissionRepository.save(p);
                        }
                }
        }

        private void seedRoles(UUID tenantId) {
                List<Permission> allPerms = permissionRepository.findByTenantId(tenantId);
                Set<Permission> adminPerms = new HashSet<>(allPerms);

                Set<Permission> procurementPerms = new HashSet<>();
                addPerm(procurementPerms, "VIEW_DASHBOARD", tenantId);
                addPerm(procurementPerms, "VIEW_REQUISITIONS", tenantId);
                addPerm(procurementPerms, "APPROVE_REQUISITION", tenantId);
                addPerm(procurementPerms, "VIEW_RFQS", tenantId);
                addPerm(procurementPerms, "MANAGE_RFQS", tenantId);
                addPerm(procurementPerms, "VIEW_PROPOSALS", tenantId);
                addPerm(procurementPerms, "VIEW_POS", tenantId);
                addPerm(procurementPerms, "CREATE_PO", tenantId);
                addPerm(procurementPerms, "MANAGE_POS", tenantId);
                addPerm(procurementPerms, "APPROVE_PO", tenantId);
                addPerm(procurementPerms, "MANAGE_SUPPLIERS", tenantId);
                addPerm(procurementPerms, "VIEW_CONTRACTS", tenantId);
                addPerm(procurementPerms, "MANAGE_CONTRACTS", tenantId);
                addPerm(procurementPerms, "SIGN_CONTRACT", tenantId);
                addPerm(procurementPerms, "VIEW_AUCTIONS", tenantId);
                addPerm(procurementPerms, "MANAGE_AUCTIONS", tenantId);

                Set<Permission> requisitionerPerms = new HashSet<>();
                addPerm(requisitionerPerms, "VIEW_DASHBOARD", tenantId);
                addPerm(requisitionerPerms, "VIEW_REQUISITIONS", tenantId);
                addPerm(requisitionerPerms, "CREATE_REQUISITION", tenantId);

                Set<Permission> approverPerms = new HashSet<>();
                addPerm(approverPerms, "VIEW_DASHBOARD", tenantId);
                addPerm(approverPerms, "VIEW_REQUISITIONS", tenantId);
                addPerm(approverPerms, "APPROVE_REQUISITION", tenantId);
                addPerm(approverPerms, "VIEW_POS", tenantId);
                addPerm(approverPerms, "APPROVE_PO", tenantId);
                // Also give ADMIN_ACCESS to ADMIN_GERAL specifically if not already there
                addPerm(adminPerms, "ADMIN_ACCESS", tenantId);

                createRoleIfNotExists("ADMIN_GERAL", "Administrador Geral com acesso total", adminPerms, false,
                                tenantId);
                createRoleIfNotExists("DIRETOR_GERAL", "Diretor Geral - Aprovador Final de Excecionais", approverPerms,
                                false,
                                tenantId);
                createRoleIfNotExists("GESTOR_PROCUREMENT", "Gestor de Procurement e Compras", procurementPerms, false,
                                tenantId);
                createRoleIfNotExists("REQUISITANTE", "Utilizador que pode criar requisições", requisitionerPerms,
                                false,
                                tenantId);
                createRoleIfNotExists("APROVADOR", "Aprovador de nível intermédio", approverPerms, false,
                                tenantId);
        }

        private void addPerm(Set<Permission> set, String slug, UUID tenantId) {
                permissionRepository.findBySlugAndTenantId(slug, tenantId).ifPresent(set::add);
        }

        private void createRoleIfNotExists(String name, String desc, Set<Permission> perms, boolean system,
                        UUID tenantId) {
                // Search case-insensitively in logic or assume DB does it (MySQL usually does)
                // But let's be safe and also try to find by slug
                roleRepository.findByNameAndTenantId(name, tenantId).ifPresentOrElse(
                                existingRole -> {
                                        // Update to system role if name matches
                                        existingRole.setSystem(system);
                                        existingRole.setPermissions(perms);
                                        // Ensure slug is also correct (uppercase for system roles)
                                        existingRole.setSlug(name.toUpperCase());
                                        roleRepository.save(existingRole);
                                },
                                () -> {
                                        roleRepository.save(createRole(name, desc, perms, system, tenantId));
                                });
        }

        private Role createRole(String name, String desc, Set<Permission> perms, boolean system, UUID tenantId) {
                Role r = new Role();
                r.setName(name);
                r.setSlug(name);
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
