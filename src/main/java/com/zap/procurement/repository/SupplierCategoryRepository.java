package com.zap.procurement.repository;

import com.zap.procurement.domain.SupplierCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierCategoryRepository extends JpaRepository<SupplierCategory, UUID> {

    // Find all categories for a supplier
    List<SupplierCategory> findBySupplierId(UUID supplierId);

    // Find all suppliers for a category
    List<SupplierCategory> findByCategoryId(UUID categoryId);

    // Check if supplier already has this category
    boolean existsBySupplierIdAndCategoryId(UUID supplierId, UUID categoryId);

    // Find active suppliers for a category (for suggestion)
    @Query("SELECT sc FROM SupplierCategory sc " +
            "JOIN FETCH sc.supplier s " +
            "JOIN FETCH sc.category c " +
            "WHERE c.id = :categoryId " +
            "AND s.status = 'ACTIVE' " +
            "AND s.tenantId = :tenantId " +
            "ORDER BY sc.isPrimary DESC, sc.rating DESC NULLS LAST, s.name ASC")
    List<SupplierCategory> findActiveSuppliersForCategory(
            @Param("categoryId") UUID categoryId,
            @Param("tenantId") UUID tenantId);

    // Find with eager loading
    @Query("SELECT sc FROM SupplierCategory sc " +
            "LEFT JOIN FETCH sc.category " +
            "WHERE sc.supplier.id = :supplierId")
    List<SupplierCategory> findBySupplierWithCategories(@Param("supplierId") UUID supplierId);

    // Delete specific link
    void deleteBySupplierIdAndCategoryId(UUID supplierId, UUID categoryId);
}
