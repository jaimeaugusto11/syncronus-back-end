package com.zap.procurement.repository;

import com.zap.procurement.domain.SupplierProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierProposalRepository extends JpaRepository<SupplierProposal, UUID> {
    List<SupplierProposal> findByRfqId(UUID rfqId);

    List<SupplierProposal> findBySupplierId(UUID supplierId);

    List<SupplierProposal> findByTenantId(UUID tenantId);
}
