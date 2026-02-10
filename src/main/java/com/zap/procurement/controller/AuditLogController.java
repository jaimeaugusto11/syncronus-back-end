package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.AuditLog;
import com.zap.procurement.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {

        UUID tenantId = TenantContext.getCurrentTenant();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());

        if (userId != null) {
            return ResponseEntity.ok(auditLogRepository.findByTenantIdAndPerformedById(tenantId, userId, pageRequest));
        }

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(
                    auditLogRepository.findByTenantIdAndTimestampBetween(tenantId, startDate, endDate, pageRequest));
        }

        if (entity != null && !entity.isEmpty()) {
            return ResponseEntity.ok(auditLogRepository.findByTenantIdAndEntity(tenantId, entity, pageRequest));
        }

        if (action != null && !action.isEmpty()) {
            return ResponseEntity.ok(auditLogRepository.findByTenantIdAndAction(tenantId, action, pageRequest));
        }

        return ResponseEntity.ok(auditLogRepository.findByTenantId(tenantId, pageRequest));
    }
}
