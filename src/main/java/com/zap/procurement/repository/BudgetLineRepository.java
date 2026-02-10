package com.zap.procurement.repository;

import com.zap.procurement.domain.BudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetLineRepository extends JpaRepository<BudgetLine, UUID> {
    List<BudgetLine> findByTenantId(UUID tenantId);

    List<BudgetLine> findByTenantIdAndDepartmentId(UUID tenantId, UUID departmentId);

    java.util.Optional<BudgetLine> findByCodeAndTenantId(String code, UUID tenantId);
}
