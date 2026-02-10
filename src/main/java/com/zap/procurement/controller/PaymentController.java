package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Payment;
import com.zap.procurement.repository.PaymentRepository;
import com.zap.procurement.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Payment> payments = paymentService.getPaymentsByTenant(tenantId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Payment>> getPendingPayments() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Payment> payments = paymentService.getPendingPayments(tenantId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable java.util.UUID id) {
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        Payment created = paymentService.createPayment(payment);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<Payment> schedulePayment(@PathVariable java.util.UUID id) {
        Payment payment = paymentService.schedulePayment(id);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Payment> processPayment(
            @PathVariable java.util.UUID id,
            @RequestBody Map<String, String> request) {
        String transactionRef = request.get("transactionReference");
        Payment payment = paymentService.processPayment(id, transactionRef);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Payment> completePayment(@PathVariable java.util.UUID id) {
        Payment payment = paymentService.completePayment(id);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<Payment> failPayment(
            @PathVariable java.util.UUID id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        Payment payment = paymentService.failPayment(id, reason);
        return ResponseEntity.ok(payment);
    }
}
