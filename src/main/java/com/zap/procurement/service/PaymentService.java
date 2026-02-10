package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional
    public Payment createPayment(Payment payment) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        // Validate tenant exists if necessary, assuming tenantId is valid from context
        // payment.setTenant(tenant); // Tenant entity not directly linked in BaseEntity
        // usually, handled by tenantId
        payment.setTenantId(tenantId);
        payment.setCode(generatePaymentCode(tenantId));
        payment.setStatus(Payment.PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment schedulePayment(java.util.UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.SCHEDULED);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processPayment(java.util.UUID paymentId, String transactionRef) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment.setTransactionReference(transactionRef);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment completePayment(java.util.UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());

        // Update invoice status
        Invoice invoice = payment.getInvoice();
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPayment(payment);
        invoiceRepository.save(invoice);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment failPayment(java.util.UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setNotes((payment.getNotes() != null ? payment.getNotes() + "; " : "") + "Failed: " + reason);

        return paymentRepository.save(payment);
    }

    private String generatePaymentCode(java.util.UUID tenantId) {
        long count = paymentRepository.findByTenantId(tenantId).size();
        return String.format("PAY-%d-%04d", java.time.Year.now().getValue(), count + 1);
    }

    public List<Payment> getPaymentsByTenant(java.util.UUID tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }

    public List<Payment> getPendingPayments(java.util.UUID tenantId) {
        return paymentRepository.findByTenantIdAndStatus(tenantId, Payment.PaymentStatus.PENDING);
    }
}
