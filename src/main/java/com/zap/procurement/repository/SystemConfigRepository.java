package com.zap.procurement.repository;

import com.zap.procurement.domain.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {
    List<SystemConfig> findByTenantId(UUID tenantId);

    Optional<SystemConfig> findByTenantIdAndKey(UUID tenantId, String key);

    List<SystemConfig> findByTenantIdAndGroup(UUID tenantId, String group);
}
