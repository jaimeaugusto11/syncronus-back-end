package com.zap.procurement.repository;

import com.zap.procurement.domain.RequisitionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequisitionApprovalRepository extends JpaRepository<RequisitionApproval, UUID> {
    List<RequisitionApproval> findByRequisitionId(UUID requisitionId);

    List<RequisitionApproval> findByApproverIdAndStatus(UUID approverId, RequisitionApproval.ApprovalStatus status);
}
