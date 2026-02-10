CREATE TABLE IF NOT EXISTS supplier_categories (
    id CHAR(36) PRIMARY KEY,
    supplier_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE COMMENT 'Categoria principal do fornecedor',
    rating DECIMAL(3,2) COMMENT 'Avaliação 0-5',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    tenant_id CHAR(36) NOT NULL,
    
    CONSTRAINT fk_sc_supplier FOREIGN KEY (supplier_id) 
        REFERENCES suppliers(id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT unique_supplier_category UNIQUE (supplier_id, category_id),
    
    INDEX idx_sc_category (category_id),
    INDEX idx_sc_supplier (supplier_id),
    INDEX idx_sc_tenant (tenant_id),
    INDEX idx_sc_primary (is_primary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
