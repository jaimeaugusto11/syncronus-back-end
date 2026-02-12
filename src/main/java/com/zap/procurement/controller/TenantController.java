package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Tenant;
import com.zap.procurement.dto.TenantDTO;
import com.zap.procurement.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@CrossOrigin(origins = "*")
public class TenantController {

    @Autowired
    private TenantRepository tenantRepository;

    @GetMapping("/current")
    public ResponseEntity<TenantDTO> getCurrentTenant() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        return tenantRepository.findById(tenantId)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-subdomain/{subdomain}")
    public ResponseEntity<TenantDTO> getTenantBySubdomain(@PathVariable String subdomain) {
        return tenantRepository.findByCode(subdomain)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<TenantDTO> createTenant(@RequestBody TenantDTO dto) {
        Tenant tenant = new Tenant();
        tenant.setName(dto.getName());
        tenant.setCode(dto.getSubdomain());
        tenant.setLogoUrl(dto.getLogoUrl());
        tenant.setPrimaryColor(dto.getPrimaryColor());
        tenant.setSecondaryColor(dto.getSecondaryColor());
        if (dto.getStatus() != null) {
            tenant.setStatus(Tenant.TenantStatus.valueOf(dto.getStatus()));
        }
        if (dto.getSubscriptionPlan() != null) {
            tenant.setSubscriptionPlan(Tenant.SubscriptionPlan.valueOf(dto.getSubscriptionPlan()));
        }

        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_SETTINGS') or hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<TenantDTO> updateTenant(@PathVariable java.util.UUID id, @RequestBody TenantDTO dto) {
        return tenantRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setCode(dto.getSubdomain());
                    existing.setLogoUrl(dto.getLogoUrl());
                    existing.setPrimaryColor(dto.getPrimaryColor());
                    existing.setSecondaryColor(dto.getSecondaryColor());
                    if (dto.getStatus() != null) {
                        existing.setStatus(Tenant.TenantStatus.valueOf(dto.getStatus()));
                    }
                    if (dto.getSubscriptionPlan() != null) {
                        existing.setSubscriptionPlan(Tenant.SubscriptionPlan.valueOf(dto.getSubscriptionPlan()));
                    }
                    Tenant updated = tenantRepository.save(existing);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private TenantDTO toDTO(Tenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setSubdomain(tenant.getCode()); // Map code to subdomain
        dto.setLogoUrl(tenant.getLogoUrl());
        dto.setPrimaryColor(tenant.getPrimaryColor());
        dto.setSecondaryColor(tenant.getSecondaryColor());
        dto.setStatus(tenant.getStatus().toString());
        if (tenant.getSubscriptionPlan() != null) {
            dto.setSubscriptionPlan(tenant.getSubscriptionPlan().toString());
        }
        return dto;
    }
}
