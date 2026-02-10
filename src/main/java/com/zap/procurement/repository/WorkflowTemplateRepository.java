package com.zap.procurement.repository;

import com.zap.procurement.domain.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

        /**
         * Find workflows by tenant ID (backward compatibility)
         */
        List<WorkflowTemplate> findByTenantId(UUID tenantId);

        /**
         * Find workflow with steps (first query to avoid MultipleBagFetchException)
         */
        @Query("SELECT DISTINCT w FROM WorkflowTemplate w " +
                        "LEFT JOIN FETCH w.steps " +
                        "WHERE w.id = :id")
        Optional<WorkflowTemplate> findByIdWithSteps(@Param("id") UUID id);

        /**
         * Find all workflows for a tenant with steps (first query)
         */
        @Query("SELECT DISTINCT w FROM WorkflowTemplate w " +
                        "LEFT JOIN FETCH w.steps " +
                        "WHERE w.tenantId = :tenantId " +
                        "ORDER BY w.name ASC")
        List<WorkflowTemplate> findAllByTenantIdWithSteps(@Param("tenantId") UUID tenantId);

        /**
         * Find active workflows by type
         */
        @Query("SELECT w FROM WorkflowTemplate w " +
                        "WHERE w.tenantId = :tenantId " +
                        "AND w.type = :type " +
                        "AND w.isActive = true")
        List<WorkflowTemplate> findActiveByTenantIdAndType(
                        @Param("tenantId") UUID tenantId,
                        @Param("type") WorkflowTemplate.WorkflowType type);
}
