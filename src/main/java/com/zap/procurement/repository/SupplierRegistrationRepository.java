package com.zap.procurement.repository;

import com.zap.procurement.domain.SupplierRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRegistrationRepository extends JpaRepository<SupplierRegistration, UUID> {
    List<SupplierRegistration> findByTenantId(UUID tenantId);

    List<SupplierRegistration> findByStatusAndTenantId(SupplierRegistration.RegistrationStatus status, UUID tenantId);
}
