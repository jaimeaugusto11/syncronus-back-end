package com.zap.procurement.repository;

import com.zap.procurement.domain.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequisitionRepository extends JpaRepository<Requisition, UUID> {
    Optional<Requisition> findByCode(String code);

    List<Requisition> findByTenantId(UUID tenantId);

    List<Requisition> findByRequesterId(UUID requesterId);

    List<Requisition> findByDepartmentId(UUID departmentId);
}
