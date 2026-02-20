package com.zap.procurement.service;

import com.zap.procurement.domain.Supplier;
import com.zap.procurement.domain.SupplierRegistration;
import com.zap.procurement.repository.SupplierRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SupplierRegistrationService {

    @Autowired
    private SupplierRegistrationRepository registrationRepository;

    @Autowired
    private SupplierService supplierService;

    public SupplierRegistration submitRegistration(SupplierRegistration registration) {
        registration.setStatus(SupplierRegistration.RegistrationStatus.PENDING);
        return registrationRepository.save(registration);
    }

    public List<SupplierRegistration> getRegistrationsByTenant(UUID tenantId) {
        return registrationRepository.findByTenantId(tenantId);
    }

    public List<SupplierRegistration> getPendingRegistrations(UUID tenantId) {
        return registrationRepository.findByStatusAndTenantId(SupplierRegistration.RegistrationStatus.PENDING,
                tenantId);
    }

    public SupplierRegistration getRegistration(UUID id) {
        return registrationRepository.findById(id).orElse(null);
    }

    @Transactional
    public SupplierRegistration approveRegistration(UUID registrationId, UUID reviewerId) {
        java.util.Objects.requireNonNull(reviewerId, "Reviewer ID cannot be null");
        SupplierRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado"));

        if (registration.getStatus() != SupplierRegistration.RegistrationStatus.PENDING &&
                registration.getStatus() != SupplierRegistration.RegistrationStatus.UNDER_REVIEW) {
            throw new RuntimeException("Apenas registros pendentes ou em revisão podem ser aprovados");
        }

        registration.setStatus(SupplierRegistration.RegistrationStatus.APPROVED);
        registration.setReviewedBy(reviewerId);
        registration.setReviewedAt(LocalDateTime.now());
        SupplierRegistration saved = registrationRepository.save(registration);

        // Convert to Supplier
        Supplier supplier = new Supplier();
        supplier.setName(registration.getCompanyName());
        supplier.setNif(registration.getNif());
        supplier.setEmail(registration.getEmail());
        supplier.setPhone(registration.getPhone());
        supplier.setAddress(registration.getAddress());
        supplier.setContactPerson(registration.getContactPerson());
        supplier.setStatus(Supplier.SupplierStatus.ACTIVE);
        supplier.setTenantId(registration.getTenantId());

        // Set a temp initial password (in a real scenario, we'd send an email to set
        // it)
        // For now, using a pattern or notifying the user
        supplier.setInitialPassword("Syncronus@2026");

        supplierService.createSupplier(supplier);

        return saved;
    }

    @Transactional
    public SupplierRegistration rejectRegistration(UUID registrationId, UUID reviewerId, String reason) {
        java.util.Objects.requireNonNull(reviewerId, "Reviewer ID cannot be null");
        SupplierRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado"));

        registration.setStatus(SupplierRegistration.RegistrationStatus.REJECTED);
        registration.setRejectionReason(reason);
        registration.setReviewedBy(reviewerId);
        registration.setReviewedAt(LocalDateTime.now());

        return registrationRepository.save(registration);
    }

    @Transactional
    public SupplierRegistration updateStatus(UUID registrationId, SupplierRegistration.RegistrationStatus status) {
        SupplierRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado"));

        registration.setStatus(status);
        return registrationRepository.save(registration);
    }
}
