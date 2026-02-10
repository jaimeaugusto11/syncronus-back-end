package com.zap.procurement.repository;

import com.zap.procurement.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByCode(String code);

    List<Invoice> findByTenantId(UUID tenantId);

    List<Invoice> findByPurchaseOrderId(UUID purchaseOrderId);

    List<Invoice> findBySupplierId(UUID supplierId);

    List<Invoice> findByTenantIdAndStatus(UUID tenantId, Invoice.InvoiceStatus status);
}
