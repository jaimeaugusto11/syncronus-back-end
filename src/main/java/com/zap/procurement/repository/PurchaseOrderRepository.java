package com.zap.procurement.repository;

import com.zap.procurement.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    Optional<PurchaseOrder> findByCode(String code);

    List<PurchaseOrder> findByTenantId(UUID tenantId);

    List<PurchaseOrder> findBySupplierId(UUID supplierId);
}
