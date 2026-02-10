package com.zap.procurement.repository;

import com.zap.procurement.domain.RFQSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RFQSupplierRepository extends JpaRepository<RFQSupplier, UUID> {
    List<RFQSupplier> findByRfqId(UUID rfqId);

    List<RFQSupplier> findBySupplierId(UUID supplierId);

    Optional<RFQSupplier> findByRfqIdAndSupplierId(UUID rfqId, UUID supplierId);

    List<RFQSupplier> findByTenantId(UUID tenantId);
}
