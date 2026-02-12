package com.zap.procurement.repository;

import com.zap.procurement.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findBySlug(String slug);

    Optional<Permission> findBySlugAndTenantId(String slug, UUID tenantId);

    java.util.List<Permission> findByTenantId(UUID tenantId);

    Optional<Permission> findByName(String name);

    List<Permission> findByGroup(String group);
}
