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
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
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
    public ResponseEntity<List<SystemConfig>> saveBatch(@RequestBody List<com.zap.procurement.dto.ConfigDTO> dtos) {
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
}
