-- ============================================
-- ROLLBACK SCRIPT (Emergency Use Only)
-- ============================================
-- Use this ONLY if you need to rollback the migration

-- Step 1: Re-add the requisition_id column
ALTER TABLE purchase_orders 
ADD COLUMN requisition_id CHAR(36) AFTER rfq_id;

-- Step 2: Restore data from junction table (takes first requisition if multiple)
UPDATE purchase_orders po
INNER JOIN (
    SELECT 
        purchase_order_id,
        MIN(requisition_id) as requisition_id  -- Takes first requisition if multiple
    FROM po_requisitions
    GROUP BY purchase_order_id
) pr ON po.id = pr.purchase_order_id
SET po.requisition_id = pr.requisition_id;

-- Step 3: Re-add foreign key constraint
ALTER TABLE purchase_orders
ADD CONSTRAINT fk_po_requisition 
FOREIGN KEY (requisition_id) REFERENCES requisitions(id);

-- Step 4: Add index
ALTER TABLE purchase_orders
ADD INDEX idx_requisition_id (requisition_id);

-- Step 5: Verify rollback
SELECT 
    'Rollback Verification' as info,
    COUNT(*) as total_pos,
    COUNT(requisition_id) as pos_with_requisition,
    COUNT(*) - COUNT(requisition_id) as pos_without_requisition
FROM purchase_orders;

-- WARNING: If a PO was linked to multiple requisitions, only ONE will be restored!
-- Check for data loss:
SELECT 
    'Potential Data Loss' as warning,
    COUNT(*) as total_links_in_junction,
    COUNT(DISTINCT purchase_order_id) as unique_pos_in_junction,
    (COUNT(*) - COUNT(DISTINCT purchase_order_id)) as links_lost_in_rollback
FROM po_requisitions;
