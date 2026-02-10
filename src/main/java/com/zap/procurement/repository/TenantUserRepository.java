package com.zap.procurement.repository;

import com.zap.procurement.domain.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    List<TenantUser> findByUserId(UUID userId);

    List<TenantUser> findByTenantId(UUID tenantId);

    Optional<TenantUser> findByTenantIdAndUserId(UUID tenantId, UUID userId);
}
