package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.EmailTemplate;
import com.zap.procurement.repository.EmailTemplateRepository;
import com.zap.procurement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/email-templates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateRepository emailTemplateRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<EmailTemplate>> getAllTemplates() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(emailTemplateRepository.findByTenantId(tenantId));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<EmailTemplate> getTemplate(@PathVariable String slug) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return emailTemplateRepository.findByTenantIdAndSlug(tenantId, slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmailTemplate> saveTemplate(@RequestBody EmailTemplate template) {
        UUID tenantId = TenantContext.getCurrentTenant();
        template.setTenantId(tenantId);
        EmailTemplate saved = emailTemplateRepository.save(template);
        auditLogService.log("EMAIL_TEMPLATE", saved.getId(), "CREATE", "Created email template: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> updateTemplate(@PathVariable UUID id, @RequestBody EmailTemplate template) {
        UUID tenantId = TenantContext.getCurrentTenant();
        EmailTemplate existing = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (!existing.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to email template");
        }

        existing.setName(template.getName());
        existing.setSubject(template.getSubject());
        existing.setContent(template.getContent());
        existing.setDescription(template.getDescription());
        existing.setActive(template.isActive());

        EmailTemplate saved = emailTemplateRepository.save(existing);
        auditLogService.log("EMAIL_TEMPLATE", saved.getId(), "UPDATE", "Updated email template: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        EmailTemplate existing = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        if (!existing.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to email template");
        }

        emailTemplateRepository.deleteById(id);
        auditLogService.log("EMAIL_TEMPLATE", id, "DELETE", "Deleted email template: " + existing.getName());
        return ResponseEntity.noContent().build();
    }
}
