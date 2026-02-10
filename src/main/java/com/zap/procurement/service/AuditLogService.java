package com.zap.procurement.service;

import com.zap.procurement.domain.AuditLog;
import com.zap.procurement.repository.AuditLogRepository;
import com.zap.procurement.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entity, UUID entityId, String action, String details) {
        log(entity, entityId, action, details, null, null);
    }

    public void log(String entity, UUID entityId, String action, String details, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.getCurrentTenant());
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        // performedById could be retrieved from SecurityContext if available
        auditLogRepository.save(log);
    }
}
