package com.zap.procurement.repository;

import com.zap.procurement.domain.POApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface POApprovalRepository extends JpaRepository<POApproval, UUID> {
    List<POApproval> findByPurchaseOrderId(UUID purchaseOrderId);

    List<POApproval> findByApproverIdAndStatus(UUID approverId, POApproval.ApprovalStatus status);
}
