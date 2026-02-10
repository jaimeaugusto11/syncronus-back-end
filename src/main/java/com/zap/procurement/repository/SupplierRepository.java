package com.zap.procurement.repository;

import com.zap.procurement.domain.Supplier;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    @EntityGraph(attributePaths = { "categories", "categories.category" })
    Optional<Supplier> findById(UUID id);

    Optional<Supplier> findByCode(String code);

    Optional<Supplier> findByEmail(String email);

    @EntityGraph(attributePaths = { "categories", "categories.category" })
    List<Supplier> findByTenantId(UUID tenantId);

    List<Supplier> findByStatus(Supplier.SupplierStatus status);
}
