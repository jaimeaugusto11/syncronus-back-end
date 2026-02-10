package com.zap.procurement.repository;

import com.zap.procurement.domain.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    List<EmailTemplate> findByTenantId(UUID tenantId);

    Optional<EmailTemplate> findByTenantIdAndSlug(UUID tenantId, String slug);
}
