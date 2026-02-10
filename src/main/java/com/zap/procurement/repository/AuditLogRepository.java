package com.zap.procurement.repository;

import com.zap.procurement.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntity(UUID tenantId, String entity, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(UUID tenantId, String action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndPerformedById(UUID tenantId, UUID performedById, Pageable pageable);

    Page<AuditLog> findByTenantIdAndTimestampBetween(UUID tenantId, java.time.LocalDateTime start,
            java.time.LocalDateTime end, Pageable pageable);
}
