package com.zap.procurement.repository;

import com.zap.procurement.domain.SupplierEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierEvaluationRepository extends JpaRepository<SupplierEvaluation, UUID> {
    List<SupplierEvaluation> findBySupplierId(UUID supplierId);

    List<SupplierEvaluation> findBySupplierIdOrderByEvaluationDateDesc(UUID supplierId);
}
