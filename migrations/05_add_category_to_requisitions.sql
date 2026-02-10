-- Adicionar colunas
ALTER TABLE requisitions 
ADD COLUMN category_id CHAR(36) AFTER department_id,
ADD COLUMN preferred_supplier_id CHAR(36) AFTER category_id;

-- Adicionar constraints
ALTER TABLE requisitions
ADD CONSTRAINT fk_requisition_category 
    FOREIGN KEY (category_id) REFERENCES categories(id),
ADD CONSTRAINT fk_requisition_preferred_supplier 
    FOREIGN KEY (preferred_supplier_id) REFERENCES suppliers(id);

-- Adicionar índices
CREATE INDEX idx_requisition_category ON requisitions(category_id);
CREATE INDEX idx_requisition_preferred_supplier ON requisitions(preferred_supplier_id);
