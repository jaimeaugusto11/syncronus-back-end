package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.SystemConfig;
import com.zap.procurement.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/integrations")
@CrossOrigin(origins = "*")
public class IntegrationController {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @GetMapping
    public ResponseEntity<List<SystemConfig>> getIntegrations() {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<SystemConfig> configs = systemConfigRepository.findByTenantIdAndGroup(tenantId, "INTEGRATION");
        return ResponseEntity.ok(configs);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<SystemConfig>> saveBatch(@RequestBody List<SystemConfigDTO> dtos) {
        UUID tenantId = TenantContext.getCurrentTenant();

        List<SystemConfig> saved = dtos.stream().map(dto -> {
            SystemConfig config = systemConfigRepository.findByTenantIdAndKey(tenantId, dto.getKey())
                    .orElseGet(() -> {
                        SystemConfig c = new SystemConfig();
                        c.setTenantId(tenantId);
                        c.setKey(dto.getKey());
                        c.setGroup("INTEGRATION"); // Enforce group
                        return c;
                    });

            config.setValue(dto.getValue());
            config.setDescription(dto.getDescription());

            return systemConfigRepository.save(config);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(saved);
    }

    // Simple DTO for request body to avoid exposing entity ID if needed
    public static class SystemConfigDTO {
        private String key;
        private String value;
        private String description;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
