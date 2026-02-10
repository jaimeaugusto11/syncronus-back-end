package com.zap.procurement.repository;

import com.zap.procurement.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByCode(String code);

    List<Payment> findByTenantId(UUID tenantId);

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByTenantIdAndStatus(UUID tenantId, Payment.PaymentStatus status);
}
