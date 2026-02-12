package com.zap.procurement.repository;

import com.zap.procurement.domain.RequisitionItem;
import com.zap.procurement.domain.RequisitionItem.RequisitionItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, UUID> {

    List<RequisitionItem> findByRequisitionId(UUID requisitionId);

    @Query("SELECT ri FROM RequisitionItem ri WHERE ri.category.id = :categoryId AND ri.status = :status AND ri.requisition.status = 'APPROVED'")
    List<RequisitionItem> findApprovedItemsByCategory(@Param("categoryId") UUID categoryId,
            @Param("status") RequisitionItemStatus status);

    List<RequisitionItem> findByStatus(RequisitionItemStatus status);
}
