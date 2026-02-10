package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.SystemConfig;
import com.zap.procurement.repository.SystemConfigRepository;
import com.zap.procurement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/configs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository systemConfigRepository;

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<SystemConfig>> getConfigs(@RequestParam(required = false) String group) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (group != null && !group.isEmpty()) {
            return ResponseEntity.ok(systemConfigRepository.findByTenantIdAndGroup(tenantId, group));
        }
        return ResponseEntity.ok(systemConfigRepository.findByTenantId(tenantId));
    }

    @PostMapping
    public ResponseEntity<SystemConfig> saveConfig(@RequestBody ConfigDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();

        SystemConfig config = systemConfigRepository.findByTenantIdAndKey(tenantId, dto.getKey())
                .orElse(new SystemConfig());

        String oldValue = config.getValue();
        config.setKey(dto.getKey());
        config.setValue(dto.getValue());
        config.setGroup(dto.getGroup());
        config.setDescription(dto.getDescription());
        config.setTenantId(tenantId);

        SystemConfig saved = systemConfigRepository.save(config);
        auditLogService.log("SYSTEM_CONFIG", saved.getId(), "UPDATE", "Updated config: " + saved.getKey(),
                oldValue,
                saved.getValue());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<SystemConfig>> saveConfigs(@RequestBody List<ConfigDTO> dtos) {
        UUID tenantId = TenantContext.getCurrentTenant();
        for (ConfigDTO dto : dtos) {
            SystemConfig config = systemConfigRepository.findByTenantIdAndKey(tenantId, dto.getKey())
                    .orElse(new SystemConfig());

            String oldValue = config.getValue();
            config.setKey(dto.getKey());
            config.setValue(dto.getValue());
            config.setGroup(dto.getGroup());
            config.setDescription(dto.getDescription());
            config.setTenantId(tenantId);

            SystemConfig saved = systemConfigRepository.save(config);
            auditLogService.log("SYSTEM_CONFIG", saved.getId(), "UPDATE",
                    "Updated config (batch): " + saved.getKey(),
                    oldValue, saved.getValue());
        }
        return ResponseEntity.ok(systemConfigRepository.findByTenantId(tenantId));
    }

    public static class ConfigDTO {
        private String key;
        private String value;
        private String group;
        private String description;

        // Getters and Setters
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
