package com.zap.procurement.repository;

import com.zap.procurement.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByTenantId(UUID tenantId);

    List<Category> findByTenantIdAndParentIsNull(UUID tenantId);
}
