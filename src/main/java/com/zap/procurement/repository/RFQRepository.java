package com.zap.procurement.repository;

import com.zap.procurement.domain.RFQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RFQRepository extends JpaRepository<RFQ, UUID> {
    Optional<RFQ> findByCode(String code);

    List<RFQ> findByTenantId(UUID tenantId);

    List<RFQ> findByRequisitionId(UUID requisitionId);

    List<RFQ> findByTenantIdAndStatus(UUID tenantId, RFQ.RFQStatus status);
}
