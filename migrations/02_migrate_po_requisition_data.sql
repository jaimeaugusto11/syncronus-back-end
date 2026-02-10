-- ============================================
-- Phase 2: Migrate Existing Data
-- ============================================
-- This migrates existing purchase_orders.requisition_id to the new junction table

-- Step 1: Insert existing relationships into junction table
INSERT INTO po_requisitions (id, purchase_order_id, requisition_id, tenant_id, created_at)
SELECT 
    UUID() as id,
    po.id as purchase_order_id,
    r.id as requisition_id,
    po.tenant_id,
    po.created_at
FROM purchase_orders po
INNER JOIN requisitions r ON po.requisition_id = r.id
WHERE po.requisition_id IS NOT NULL;

-- Step 2: Verify migration
-- This should return the count of migrated records
SELECT 
    'Migration Summary' as info,
    COUNT(*) as total_links_created,
    COUNT(DISTINCT purchase_order_id) as unique_pos,
    COUNT(DISTINCT requisition_id) as unique_requisitions
FROM po_requisitions;

-- Step 3: Check for orphaned POs (POs without requisitions)
SELECT 
    'Orphaned POs' as info,
    COUNT(*) as count
FROM purchase_orders
WHERE requisition_id IS NULL;

-- Step 4: Verify no data loss
-- This should return 0 if migration was successful
SELECT 
    'Data Loss Check' as info,
    COUNT(*) as pos_not_migrated
FROM purchase_orders po
WHERE po.requisition_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM po_requisitions pr
      WHERE pr.purchase_order_id = po.id
        AND pr.requisition_id = po.requisition_id
  );
