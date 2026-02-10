package com.zap.procurement.repository;

import com.zap.procurement.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {
    Optional<GoodsReceipt> findByCode(String code);

    List<GoodsReceipt> findByTenantId(UUID tenantId);

    List<GoodsReceipt> findByPurchaseOrderId(UUID purchaseOrderId);
}
