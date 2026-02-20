package com.zap.procurement.controller;

import com.zap.procurement.domain.SupplierRegistration;
import com.zap.procurement.service.SupplierRegistrationService;
import com.zap.procurement.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SupplierRegistrationController {

    @Autowired
    private SupplierRegistrationService registrationService;

    // Public endpoint for self-registration
    // This is used by the portal's landing page where suppliers sign up
    @PostMapping("/public/supplier-registrations")
    public ResponseEntity<SupplierRegistration> publicRegister(@RequestBody SupplierRegistration registration) {
        // In a real scenario, we might want to validate the tenantId passed or infer it
        // from the host
        // For now, we assume it's passed or handled by TenantContext if available via
        // header
        if (registration.getTenantId() == null) {
            registration.setTenantId(TenantContext.getCurrentTenant());
        }
        return ResponseEntity.ok(registrationService.submitRegistration(registration));
    }

    // Secured endpoints for admin review
    @GetMapping("/supplier-registrations")
    public ResponseEntity<List<SupplierRegistration>> getRegistrations() {
        return ResponseEntity.ok(registrationService.getRegistrationsByTenant(TenantContext.getCurrentTenant()));
    }

    @GetMapping("/supplier-registrations/pending")
    public ResponseEntity<List<SupplierRegistration>> getPendingRegistrations() {
        return ResponseEntity.ok(registrationService.getPendingRegistrations(TenantContext.getCurrentTenant()));
    }

    @GetMapping("/supplier-registrations/{id}")
    public ResponseEntity<SupplierRegistration> getRegistration(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    @PostMapping("/supplier-registrations/{id}/approve")
    public ResponseEntity<SupplierRegistration> approveRegistration(
            @PathVariable UUID id,
            @RequestHeader("X-User-ID") UUID reviewerId) {
        return ResponseEntity.ok(registrationService.approveRegistration(id, reviewerId));
    }

    @PostMapping("/supplier-registrations/{id}/reject")
    public ResponseEntity<SupplierRegistration> rejectRegistration(
            @PathVariable UUID id,
            @RequestHeader("X-User-ID") UUID reviewerId,
            @RequestBody String reason) {
        return ResponseEntity.ok(registrationService.rejectRegistration(id, reviewerId, reason));
    }

    @PutMapping("/supplier-registrations/{id}/status")
    public ResponseEntity<SupplierRegistration> updateStatus(
            @PathVariable UUID id,
            @RequestParam SupplierRegistration.RegistrationStatus status) {
        return ResponseEntity.ok(registrationService.updateStatus(id, status));
    }
}
