-- Sample Data for SupplySync Platform
-- This script creates sample data for all tables in the database

-- Insert Tenants
INSERT INTO tenants (name, subdomain, active, created_at, updated_at) VALUES
('Sonangol', 'sonangol', true, NOW(), NOW()),
('Unitel', 'unitel', true, NOW(), NOW()),
('BAI', 'bai', true, NOW(), NOW());

-- Insert Departments for Tenant 1 (Sonangol)
INSERT INTO departments (tenant_id, name, code, budget, created_at, updated_at) VALUES
(1, 'Tecnologia da Informação', 'TI', 5000000.00, NOW(), NOW()),
(1, 'Administração', 'ADM', 2000000.00, NOW(), NOW()),
(1, 'Logística', 'LOG', 3000000.00, NOW(), NOW()),
(1, 'Facilities', 'FAC', 1500000.00, NOW(), NOW());

-- Insert Users for Tenant 1
INSERT INTO users (tenant_id, email, password, name, role, department_id, active, created_at, updated_at) VALUES
(1, 'joao.silva@sonangol.co.ao', '$2a$10$dummyhashedpassword', 'João Silva', 'PROCUREMENT_MANAGER', 1, true, NOW(), NOW()),
(1, 'maria.santos@sonangol.co.ao', '$2a$10$dummyhashedpassword', 'Maria Santos', 'DEPARTMENT_HEAD', 2, true, NOW(), NOW()),
(1, 'pedro.costa@sonangol.co.ao', '$2a$10$dummyhashedpassword', 'Pedro Costa', 'REQUESTER', 3, true, NOW(), NOW()),
(1, 'ana.ferreira@sonangol.co.ao', '$2a$10$dummyhashedpassword', 'Ana Ferreira', 'FINANCE', 2, true, NOW(), NOW());

-- Insert Suppliers for Tenant 1
INSERT INTO suppliers (tenant_id, code, name, nif, email, phone, address, category, status, performance_score, created_at, updated_at) VALUES
(1, 'FORN-1022', 'Angola Cables', '5417281934', 'comercial@angolacables.co.ao', '+244 923 456 789', 'Luanda, Angola', 'TI & Hardware', 'ACTIVE', 92, NOW(), NOW()),
(1, 'FORN-1023', 'Standard Logistics', '5417281935', 'vendas@standardlog.co.ao', '+244 923 456 790', 'Luanda, Angola', 'Logística', 'ACTIVE', 88, NOW(), NOW()),
(1, 'FORN-1024', 'TotalEnergies E&P Angola', '5417281936', 'procurement@totalenergies.co.ao', '+244 923 456 791', 'Luanda, Angola', 'Energia', 'ACTIVE', 95, NOW(), NOW()),
(1, 'FORN-1025', 'Office Solutions Angola', '5417281937', 'vendas@officesolutions.co.ao', '+244 923 456 792', 'Luanda, Angola', 'Material de Escritório', 'ACTIVE', 85, NOW(), NOW()),
(1, 'FORN-1026', 'Dell Technologies Angola', '5417281938', 'sales@dell.co.ao', '+244 923 456 793', 'Luanda, Angola', 'TI & Hardware', 'ACTIVE', 90, NOW(), NOW());

-- Insert Requisitions for Tenant 1
INSERT INTO requisitions (tenant_id, code, title, description, department_id, requester_id, status, priority, total_value, required_date, created_at, updated_at) VALUES
(1, 'REQ-2024-0142', 'Equipamento de Escritório - Luanda HQ', 'Mobiliário e equipamentos para novo escritório', 2, 2, 'PENDING_APPROVAL', 'MEDIUM', 45000.00, '2024-03-15', NOW(), NOW()),
(1, 'REQ-2024-0141', 'Servidores Dell PowerEdge R750', 'Servidores para data center', 1, 1, 'APPROVED', 'HIGH', 280000.00, '2024-03-01', NOW(), NOW()),
(1, 'REQ-2024-0140', 'Material de Limpeza - Q1 2024', 'Material de limpeza trimestral', 4, 3, 'CONVERTED_TO_RFQ', 'LOW', 12500.00, '2024-02-28', NOW(), NOW()),
(1, 'REQ-2024-0139', 'Veículos Toyota Hilux 4x4', 'Veículos para operações de campo', 3, 3, 'REJECTED', 'URGENT', 1200000.00, '2024-04-01', NOW(), NOW());

-- Insert Requisition Items
INSERT INTO requisition_items (requisition_id, description, quantity, unit_price, total_price, specifications) VALUES
(1, 'Mesa de Escritório Executiva', 15, 2500.00, 37500.00, 'Madeira nobre, 1.80m x 0.80m'),
(1, 'Cadeira Ergonômica', 15, 500.00, 7500.00, 'Ajustável, suporte lombar'),
(2, 'Dell PowerEdge R750 - 32GB RAM', 4, 70000.00, 280000.00, 'Intel Xeon, 2TB SSD, Redundant PSU'),
(3, 'Detergente Industrial 5L', 50, 150.00, 7500.00, 'Biodegradável'),
(3, 'Papel Higiênico Industrial', 100, 50.00, 5000.00, 'Folha dupla, 300m');

-- Insert RFQs (Request for Quotations)
INSERT INTO rfqs (tenant_id, code, title, description, requisition_id, status, deadline, created_by, created_at, updated_at) VALUES
(1, 'RFQ-2024-089', 'Servidores Dell PowerEdge R750', 'Cotação para servidores de data center', 2, 'OPEN', '2024-02-15 23:59:59', 1, NOW(), NOW()),
(1, 'RFQ-2024-090', 'Material de Limpeza Q1 2024', 'Cotação para material de limpeza trimestral', 3, 'EVALUATION', '2024-02-10 23:59:59', 1, NOW(), NOW()),
(1, 'RFQ-2024-092', 'Material de Escritório Q1 2024', 'Cotação para material de escritório', 1, 'OPEN', '2024-02-20 23:59:59', 1, NOW(), NOW());

-- Insert RFQ Items
INSERT INTO rfq_items (rfq_id, description, quantity, unit, specifications, estimated_unit_price) VALUES
(1, 'Dell PowerEdge R750 - 32GB RAM', 4, 'UN', 'Intel Xeon, 2TB SSD, Redundant PSU', 70000.00),
(2, 'Detergente Industrial 5L', 50, 'UN', 'Biodegradável', 150.00),
(2, 'Papel Higiênico Industrial', 100, 'UN', 'Folha dupla, 300m', 50.00),
(3, 'Mesa de Escritório Executiva', 15, 'UN', 'Madeira nobre, 1.80m x 0.80m', 2500.00),
(3, 'Cadeira Ergonômica', 15, 'UN', 'Ajustável, suporte lombar', 500.00);

-- Link RFQ to Suppliers
INSERT INTO rfq_suppliers (rfq_id, supplier_id, invited_at) VALUES
(1, 1, NOW()),
(1, 5, NOW()),
(2, 4, NOW()),
(3, 4, NOW());

-- Insert Proposals
INSERT INTO proposals (rfq_id, supplier_id, total_value, validity_days, payment_terms, delivery_terms, status, submitted_at, created_at, updated_at) VALUES
(1, 1, 265000.00, 30, '30 dias após entrega', '15 dias úteis', 'UNDER_REVIEW', NOW(), NOW(), NOW()),
(1, 5, 275000.00, 30, '45 dias após entrega', '10 dias úteis', 'UNDER_REVIEW', NOW(), NOW(), NOW()),
(2, 4, 11500.00, 15, '15 dias após entrega', '5 dias úteis', 'ACCEPTED', NOW(), NOW(), NOW());

-- Insert Proposal Items
INSERT INTO proposal_items (proposal_id, rfq_item_id, quantity, unit_price, total_price, notes) VALUES
(1, 1, 4, 66250.00, 265000.00, 'Inclui instalação e configuração'),
(2, 1, 4, 68750.00, 275000.00, 'Garantia estendida de 3 anos'),
(3, 2, 50, 140.00, 7000.00, 'Desconto por volume'),
(3, 3, 100, 45.00, 4500.00, 'Entrega mensal');

-- Insert Purchase Orders
INSERT INTO purchase_orders (tenant_id, code, supplier_id, requisition_id, proposal_id, total_value, status, delivery_date, payment_terms, created_by, approved_by, created_at, updated_at) VALUES
(1, 'PO-2024-156', 1, 2, 1, 265000.00, 'CONFIRMED', '2024-02-25', '30 dias após entrega', 1, 1, NOW(), NOW()),
(1, 'PO-2024-157', 4, 3, 3, 11500.00, 'DELIVERED', '2024-02-15', '15 dias após entrega', 1, 1, NOW(), NOW());

-- Insert PO Items
INSERT INTO purchase_order_items (purchase_order_id, description, quantity, unit_price, total_price, specifications) VALUES
(1, 'Dell PowerEdge R750 - 32GB RAM', 4, 66250.00, 265000.00, 'Intel Xeon, 2TB SSD, Redundant PSU, Instalação incluída'),
(2, 'Detergente Industrial 5L', 50, 140.00, 7000.00, 'Biodegradável'),
(2, 'Papel Higiênico Industrial', 100, 45.00, 4500.00, 'Folha dupla, 300m');

-- Insert Goods Receipts
INSERT INTO goods_receipts (tenant_id, code, purchase_order_id, received_date, received_by, status, notes, created_at, updated_at) VALUES
(1, 'GR-2024-089', 2, '2024-02-14', 3, 'COMPLETED', 'Todos os itens recebidos em perfeito estado', NOW(), NOW());

-- Insert GR Items
INSERT INTO goods_receipt_items (goods_receipt_id, po_item_id, quantity_ordered, quantity_received, quantity_accepted, quantity_rejected, notes) VALUES
(1, 2, 50, 50, 50, 0, 'Conforme especificado'),
(1, 3, 100, 100, 100, 0, 'Conforme especificado');

-- Insert Invoices
INSERT INTO invoices (tenant_id, code, supplier_id, purchase_order_id, goods_receipt_id, invoice_number, invoice_date, due_date, total_value, status, created_at, updated_at) VALUES
(1, 'INV-2024-089', 4, 2, 1, 'FAT-2024-001234', '2024-02-14', '2024-02-28', 11500.00, 'PENDING_PAYMENT', NOW(), NOW()),
(1, 'INV-2024-090', 1, 1, NULL, 'FAT-2024-001235', '2024-02-20', '2024-03-22', 265000.00, 'PENDING_3WAY_MATCH', NOW(), NOW());

-- Insert Invoice Items
INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, total_price, tax_rate, tax_amount) VALUES
(1, 'Detergente Industrial 5L', 50, 140.00, 7000.00, 14.00, 980.00),
(1, 'Papel Higiênico Industrial', 100, 45.00, 4500.00, 14.00, 630.00),
(2, 'Dell PowerEdge R750 - 32GB RAM', 4, 66250.00, 265000.00, 14.00, 37100.00);

-- Insert Payments
INSERT INTO payments (tenant_id, invoice_id, payment_date, amount, payment_method, reference, status, created_at, updated_at) VALUES
(1, 1, '2024-02-27', 11500.00, 'BANK_TRANSFER', 'TRF-2024-0089', 'COMPLETED', NOW(), NOW());

-- Insert Contracts
INSERT INTO contracts (tenant_id, code, title, supplier_id, contract_type, start_date, end_date, value, status, auto_renewal, renewal_notice_days, created_at, updated_at) VALUES
(1, 'CONT-2024-001', 'Contrato Framework - Angola Cables', 1, 'FRAMEWORK', '2024-01-01', '2026-12-31', 5000000.00, 'ACTIVE', true, 90, NOW(), NOW()),
(1, 'CONT-2024-002', 'Fornecimento Material de Escritório', 4, 'FIXED_PRICE', '2024-01-01', '2024-12-31', 150000.00, 'ACTIVE', false, 60, NOW(), NOW());

-- Insert Analytics Data (Spend Analysis)
INSERT INTO spend_analysis (tenant_id, period_start, period_end, category, total_spend, transaction_count, avg_transaction_value, top_supplier_id, created_at) VALUES
(1, '2024-01-01', '2024-01-31', 'TI & Hardware', 450000.00, 12, 37500.00, 1, NOW()),
(1, '2024-01-01', '2024-01-31', 'Material de Escritório', 85000.00, 25, 3400.00, 4, NOW()),
(1, '2024-01-01', '2024-01-31', 'Logística', 120000.00, 8, 15000.00, 2, NOW());

COMMIT;
