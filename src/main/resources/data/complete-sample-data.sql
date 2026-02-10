-- =====================================================
-- SupplySync - Simplified Sample Data (Core Entities Only)
-- =====================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE users;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE departments;
TRUNCATE TABLE tenants;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 1. TENANTS
-- =====================================================
INSERT INTO tenants (id, name, subdomain, active, created_at, updated_at) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'Sonangol E.P.', 'sonangol', true, NOW(), NOW());

-- =====================================================
-- 2. DEPARTMENTS
-- =====================================================
INSERT INTO departments (id, tenant_id, name, code, budget, created_at, updated_at) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'Tecnologia da Informação', 'TI', 8000000.00, NOW(), NOW()),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000102'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'Procurement', 'PROC', 15000000.00, NOW(), NOW()),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000103'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'Operações', 'OPS', 12000000.00, NOW(), NOW());

-- =====================================================
-- 3. SUPPLIERS
-- =====================================================
INSERT INTO suppliers (id, tenant_id, code, name, nif, email, phone, address, category, status, performance_score, contact_person, created_at, updated_at) VALUES
(UUID_TO_BIN('00000000-0000-0000-0000-000000000201'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'FORN-1001', 'Angola Cables', '5417281934', 'comercial@angolacables.co.ao', '+244 923 456 789', 'Rua Rainha Ginga 197, Luanda', 'TI & Telecomunicações', 'ACTIVE', 95, 'Paulo Silva', NOW(), NOW()),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000202'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'FORN-1002', 'Dell Technologies', '5417281938', 'sales@dell.co.ao', '+244 923 456 793', 'Talatona, Luanda', 'TI & Hardware', 'ACTIVE', 90, 'Sandra Costa', NOW(), NOW());

-- =====================================================
-- 4. USERS (Password: password123)
-- =====================================================
INSERT INTO users (id, tenant_id, email, password, name, role, department_id, status, created_at, updated_at) VALUES
-- ADMIN
(UUID_TO_BIN('00000000-0000-0000-0000-000000000301'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'admin@sonangol.co.ao', '$2a$10$N9qo8uLOickgx2ZMRZoMye7I73TZRQAuBWTbJ0wCKhCJL.dJb9Dka', 'Carlos Admin', 'ADMIN', UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), 'ACTIVE', NOW(), NOW()),
-- GESTOR_PROCUREMENT
(UUID_TO_BIN('00000000-0000-0000-0000-000000000302'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'maria.gestora@sonangol.co.ao', '$2a$10$N9qo8uLOickgx2ZMRZoMye7I73TZRQAuBWTbJ0wCKhCJL.dJb9Dka', 'Maria Gestora', 'GESTOR_PROCUREMENT', UUID_TO_BIN('00000000-0000-0000-0000-000000000102'), 'ACTIVE', NOW(), NOW()),
-- REQUISITANTE
(UUID_TO_BIN('00000000-0000-0000-0000-000000000304'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'joao.silva@sonangol.co.ao', '$2a$10$N9qo8uLOickgx2ZMRZoMye7I73TZRQAuBWTbJ0wCKhCJL.dJb9Dka', 'João Silva', 'REQUISITANTE', UUID_TO_BIN('00000000-0000-0000-0000-000000000103'), 'ACTIVE', NOW(), NOW()),
(UUID_TO_BIN('00000000-0000-0000-0000-000000000305'), UUID_TO_BIN('00000000-0000-0000-0000-000000000001'), 'ana.santos@sonangol.co.ao', '$2a$10$N9qo8uLOickgx2ZMRZoMye7I73TZRQAuBWTbJ0wCKhCJL.dJb9Dka', 'Ana Santos', 'REQUISITANTE', UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), 'ACTIVE', NOW(), NOW());

COMMIT;

-- Verification
SELECT 'Tenants loaded:' as info, COUNT(*) as count FROM tenants;
SELECT 'Departments loaded:' as info, COUNT(*) as count FROM departments;
SELECT 'Suppliers loaded:' as info, COUNT(*) as count FROM suppliers;
SELECT 'Users loaded:' as info, COUNT(*) as count FROM users;
