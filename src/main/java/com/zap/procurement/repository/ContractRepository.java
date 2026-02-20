package com.zap.procurement.repository;

import com.zap.procurement.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findByTenantId(UUID tenantId);

    List<Contract> findBySupplierId(UUID supplierId);

    List<Contract> findByPurchaseOrderId(UUID poId);
}
