-- ============================================
-- Phase 3: Remove Old Column (BREAKING CHANGE)
-- ============================================
-- WARNING: Only run this AFTER verifying the migration was successful!
-- This is a BREAKING CHANGE and cannot be easily reversed.

-- Step 1: Backup check - ensure junction table has data
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'SAFE TO PROCEED'
        ELSE 'DANGER: Junction table is empty!'
    END as migration_status,
    COUNT(*) as junction_table_records
FROM po_requisitions;

-- Step 2: Drop the foreign key constraint first
ALTER TABLE purchase_orders 
DROP FOREIGN KEY IF EXISTS purchase_orders_ibfk_1;

-- Step 3: Drop the index on requisition_id
ALTER TABLE purchase_orders 
DROP INDEX IF EXISTS requisition_id;

-- Step 4: Drop the requisition_id column
ALTER TABLE purchase_orders 
DROP COLUMN IF EXISTS requisition_id;

-- Step 5: Verify column is removed
SHOW COLUMNS FROM purchase_orders LIKE 'requisition_id';
-- This should return empty result set

-- Step 6: Verify junction table still has data
SELECT 
    'Post-Migration Check' as info,
    COUNT(*) as total_links,
    COUNT(DISTINCT purchase_order_id) as unique_pos,
    COUNT(DISTINCT requisition_id) as unique_requisitions
FROM po_requisitions;
