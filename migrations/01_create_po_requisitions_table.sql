-- ============================================
-- Phase 1: Create Junction Table
-- ============================================
-- This creates the many-to-many relationship table between POs and Requisitions

CREATE TABLE IF NOT EXISTS po_requisitions (
    id CHAR(36) PRIMARY KEY,
    purchase_order_id CHAR(36) NOT NULL,
    requisition_id CHAR(36) NOT NULL,
    quantity_fulfilled DECIMAL(10,2) COMMENT 'How much of the requisition this PO fulfills',
    notes TEXT COMMENT 'Optional notes about this specific link',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id CHAR(36) NOT NULL,
    
    -- Foreign keys with cascade delete
    CONSTRAINT fk_po_req_po FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_po_req_requisition FOREIGN KEY (requisition_id) 
        REFERENCES requisitions(id) ON DELETE CASCADE,
    
    -- Prevent duplicate links
    CONSTRAINT unique_po_requisition UNIQUE (purchase_order_id, requisition_id),
    
    -- Performance indexes
    INDEX idx_po_req_po_id (purchase_order_id),
    INDEX idx_po_req_req_id (requisition_id),
    INDEX idx_po_req_tenant (tenant_id),
    INDEX idx_po_req_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Junction table for many-to-many relationship between POs and Requisitions';
