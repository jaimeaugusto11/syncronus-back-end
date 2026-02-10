package com.zap.procurement.repository;

import com.zap.procurement.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByTenantId(UUID tenantId);

    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    Optional<Role> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
