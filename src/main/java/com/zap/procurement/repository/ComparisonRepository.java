package com.zap.procurement.repository;

import com.zap.procurement.domain.Comparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComparisonRepository extends JpaRepository<Comparison, UUID> {
    List<Comparison> findByRfqId(UUID rfqId);

    List<Comparison> findByTenantId(UUID tenantId);
}
