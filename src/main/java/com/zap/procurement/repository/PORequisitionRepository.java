package com.zap.procurement.repository;

import com.zap.procurement.domain.PORequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PORequisitionRepository extends JpaRepository<PORequisition, UUID> {

    // Find all requisitions linked to a PO
    List<PORequisition> findByPurchaseOrderId(UUID poId);

    // Find all POs linked to a requisition
    List<PORequisition> findByRequisitionId(UUID reqId);

    // Check if link already exists
    boolean existsByPurchaseOrderIdAndRequisitionId(UUID poId, UUID reqId);

    // Find with eager loading for performance
    @Query("SELECT pr FROM PORequisition pr " +
            "LEFT JOIN FETCH pr.requisition r " +
            "WHERE pr.purchaseOrder.id = :poId")
    List<PORequisition> findByPOWithRequisitions(@Param("poId") UUID poId);

    @Query("SELECT pr FROM PORequisition pr " +
            "LEFT JOIN FETCH pr.purchaseOrder po " +
            "WHERE pr.requisition.id = :reqId")
    List<PORequisition> findByRequisitionWithPOs(@Param("reqId") UUID reqId);

    // Delete specific link
    void deleteByPurchaseOrderIdAndRequisitionId(UUID poId, UUID reqId);
}
