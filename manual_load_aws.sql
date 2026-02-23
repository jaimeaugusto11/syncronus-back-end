-- =====================================================
-- Manual Data Population Script for AWS (MariaDB/MySQL)
-- =====================================================
-- Run this AFTER the application has started.
-- This script ENSURES that critical tables exist and are populated.

USE zap_procurement;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. CREATE CRITICAL TABLES (If Hibernate failed)
-- Standard Role table structure for Hibernate 6
CREATE TABLE IF NOT EXISTS roles (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    tenant_id BINARY(16) NOT NULL,
    active BIT NOT NULL,
    is_system BIT NOT NULL,
    description VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name_tenant (name, tenant_id),
    UNIQUE KEY uk_roles_slug_tenant (slug, tenant_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS permissions (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    tenant_id BINARY(16) NOT NULL,
    active BIT NOT NULL,
    description VARCHAR(255),
    group_name VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_name_tenant (name, tenant_id),
    UNIQUE KEY uk_permissions_slug_tenant (slug, tenant_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BINARY(16) NOT NULL,
    permission_id BINARY(16) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB;

-- 2. CLEAN ALL TABLES
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE payments;
TRUNCATE TABLE invoice_items;
TRUNCATE TABLE invoices;
TRUNCATE TABLE gr_items;
TRUNCATE TABLE goods_receipts;
TRUNCATE TABLE po_items;
TRUNCATE TABLE purchase_orders;
TRUNCATE TABLE proposal_items;
TRUNCATE TABLE supplier_proposals;
TRUNCATE TABLE rfq_suppliers;
TRUNCATE TABLE rfq_items;
TRUNCATE TABLE rfqs;
TRUNCATE TABLE requisition_approvals;
TRUNCATE TABLE requisition_items;
TRUNCATE TABLE requisitions;
TRUNCATE TABLE supplier_users;
TRUNCATE TABLE tenant_users;
TRUNCATE TABLE users;
TRUNCATE TABLE role_permissions;
TRUNCATE TABLE roles;
TRUNCATE TABLE permissions;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE departments;
TRUNCATE TABLE tenants;
TRUNCATE TABLE categories;
TRUNCATE TABLE supplier_categories;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 3. TENANTS
-- =====================================================
INSERT INTO tenants (id, name, code, status, created_at, updated_at) VALUES
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'Sonangol E.P.', 'SONANGOL', 'ACTIVE', NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000002', '-', '')), 'Unitel S.A.', 'UNITEL', 'ACTIVE', NOW(), NOW());

-- =====================================================
-- 4. ROLES
-- =====================================================
INSERT INTO roles (id, tenant_id, name, slug, description, is_system, active, created_at, updated_at) VALUES
(UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a1', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'ADMIN_GERAL', 'ADMIN_GERAL', 'Administrador Geral com acesso total', 1, 1, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a2', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'GESTOR_PROCUREMENT', 'GESTOR_PROCUREMENT', 'Gestor de Procurement e Compras', 1, 1, NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a3', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'REQUISITANTE', 'REQUISITANTE', 'Utilizador que pode criar requisições', 1, 1, NOW(), NOW());

-- =====================================================
-- 5. DEPARTMENTS
-- =====================================================
INSERT INTO departments (id, tenant_id, name, code, created_at, updated_at) VALUES
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000101', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'Tecnologia da Informação', 'TI', NOW(), NOW()),
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000102', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'Procurement', 'PROC', NOW(), NOW());

-- =====================================================
-- 6. USERS
-- =====================================================
INSERT INTO users (id, tenant_id, email, password, name, role_id, department_id, status, created_at, updated_at) VALUES
(UNHEX(REPLACE('00000000-0000-0000-0000-000000000301', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'admin@sonangol.co.ao', '$2a$10$N9qo8uLOickgx2ZMRZoMye7I73TZRQAuBWTbJ0wCKhCJL.dJb9Dka', 'Carlos Admin', UNHEX(REPLACE('00000000-0000-0000-0000-0000000000a1', '-', '')), UNHEX(REPLACE('00000000-0000-0000-0000-000000000101', '-', '')), 'ACTIVE', NOW(), NOW());

COMMIT;
