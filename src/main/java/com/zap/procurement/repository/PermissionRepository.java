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

    List<Permission> findByGroup(String group);
}
