package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.dto.WorkflowApproverDTO;
import com.zap.procurement.dto.WorkflowStepDTO;
import com.zap.procurement.dto.WorkflowTemplateDTO;
import com.zap.procurement.repository.RoleRepository;
import com.zap.procurement.repository.UserRepository;
import com.zap.procurement.repository.WorkflowTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class WorkflowService {

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Create new workflow with validations
     */
    @Transactional
    public WorkflowTemplate createWorkflow(WorkflowTemplateDTO dto) {
        log.info("Creating new workflow: {}", dto.getName());

        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        // Validate basic fields
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Workflow name is required");
        }

        if (dto.getType() == null) {
            throw new RuntimeException("Workflow type is required");
        }

        // Validate steps if provided
        if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
            validateSteps(dto.getSteps());
            validateApprovers(dto.getSteps());
        }

        // Create template
        WorkflowTemplate template = new WorkflowTemplate();
        template.setName(dto.getName());
        template.setType(WorkflowTemplate.WorkflowType.valueOf(dto.getType()));
        template.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        template.setTenantId(tenantId);

        // Add steps if provided
        if (dto.getSteps() != null) {
            for (WorkflowStepDTO stepDto : dto.getSteps()) {
                WorkflowStep step = createStepFromDTO(stepDto, template, tenantId);
                template.getSteps().add(step);
            }
        }

        WorkflowTemplate saved = workflowTemplateRepository.save(template);
        log.info("Workflow created successfully: {}", saved.getId());
        return saved;
    }

    /**
     * Update workflow with comprehensive validations
     */
    @Transactional
    public WorkflowTemplate updateWorkflow(UUID id, WorkflowTemplateDTO dto) {
        log.info("Updating workflow: {}", id);

        // Load with steps (avoiding MultipleBagFetchException)
        WorkflowTemplate template = workflowTemplateRepository
                .findByIdWithSteps(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));

        // Initialize approvers to avoid lazy loading
        template.getSteps().forEach(step -> step.getApprovers().size());

        // Check if workflow can be edited
        if (template.getIsActive() && isInUse(template)) {
            throw new RuntimeException(
                    "Cannot edit active workflow that is currently in use. Deactivate it first or create a new version.");
        }

        // Validate new data
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Workflow name is required");
        }

        if (dto.getSteps() != null) {
            validateSteps(dto.getSteps());
            validateApprovers(dto.getSteps());
        }

        UUID tenantId = TenantContext.getCurrentTenant();

        // Update basic fields
        template.setName(dto.getName());
        template.setType(WorkflowTemplate.WorkflowType.valueOf(dto.getType()));
        template.setIsActive(dto.getIsActive());

        // Clear old steps (orphanRemoval = true will delete them)
        template.getSteps().clear();
        workflowTemplateRepository.flush(); // Force delete before adding new ones

        // Add new steps
        if (dto.getSteps() != null) {
            for (WorkflowStepDTO stepDto : dto.getSteps()) {
                WorkflowStep step = createStepFromDTO(stepDto, template, tenantId);
                template.getSteps().add(step);
            }
        }

        WorkflowTemplate saved = workflowTemplateRepository.save(template);
        log.info("Workflow updated successfully: {}", id);
        return saved;
    }

    /**
     * Validate workflow steps
     */
    private void validateSteps(List<WorkflowStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new RuntimeException("Workflow must have at least one step");
        }

        Set<Integer> orders = new HashSet<>();
        for (WorkflowStepDTO step : steps) {
            // Validate step order
            if (step.getStepOrder() == null || step.getStepOrder() < 1) {
                throw new RuntimeException("Invalid step order. Must be >= 1");
            }

            // Check for duplicate orders
            if (!orders.add(step.getStepOrder())) {
                throw new RuntimeException("Duplicate step order: " + step.getStepOrder());
            }

            // Validate step name
            if (step.getName() == null || step.getName().trim().isEmpty()) {
                throw new RuntimeException("Step name is required for step order " + step.getStepOrder());
            }
        }

        // Ensure orders are sequential (1, 2, 3, ...)
        List<Integer> sortedOrders = new ArrayList<>(orders);
        Collections.sort(sortedOrders);
        for (int i = 0; i < sortedOrders.size(); i++) {
            if (sortedOrders.get(i) != i + 1) {
                throw new RuntimeException("Step orders must be sequential starting from 1. Missing order: " + (i + 1));
            }
        }
    }

    /**
     * Validate approvers in steps
     */
    private void validateApprovers(List<WorkflowStepDTO> steps) {
        for (WorkflowStepDTO step : steps) {
            if (step.getApprovers() == null || step.getApprovers().isEmpty()) {
                throw new RuntimeException("Step '" + step.getName() + "' must have at least one approver");
            }

            for (WorkflowApproverDTO approver : step.getApprovers()) {
                if (approver.getApproverType() == null || approver.getApproverType().trim().isEmpty()) {
                    throw new RuntimeException("Approver type is required in step '" + step.getName() + "'");
                }

                String type = approver.getApproverType().toUpperCase();

                // Validate USER type
                if ("USER".equals(type)) {
                    if (approver.getValue() == null || approver.getValue().trim().isEmpty()) {
                        throw new RuntimeException(
                                "User ID is required for USER approver in step '" + step.getName() + "'");
                    }

                    try {
                        UUID userId = UUID.fromString(approver.getValue());
                        if (!userRepository.existsById(userId)) {
                            throw new RuntimeException(
                                    "User not found: " + approver.getValue() + " in step '" + step.getName() + "'");
                        }
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(
                                "Invalid User ID format: " + approver.getValue() + " in step '" + step.getName() + "'");
                    }
                }

                // Validate ROLE type
                if ("ROLE".equals(type)) {
                    if (approver.getValue() == null || approver.getValue().trim().isEmpty()) {
                        throw new RuntimeException(
                                "Role ID or slug is required for ROLE approver in step '" + step.getName() + "'");
                    }

                    // Try to parse as UUID first, if that fails, treat as slug
                    try {
                        UUID roleId = UUID.fromString(approver.getValue());
                        if (!roleRepository.existsById(roleId)) {
                            throw new RuntimeException(
                                    "Role not found: " + approver.getValue() + " in step '" + step.getName() + "'");
                        }
                    } catch (IllegalArgumentException e) {
                        // Not a UUID, try as slug
                        if (!roleRepository.existsBySlug(approver.getValue())) {
                            throw new RuntimeException(
                                    "Role not found with slug: " + approver.getValue() + " in step '" + step.getName()
                                            + "'");
                        }
                    }
                }

                // DEPT_HEAD doesn't need value validation
            }
        }
    }

    /**
     * Create WorkflowStep from DTO
     */
    private WorkflowStep createStepFromDTO(WorkflowStepDTO stepDto, WorkflowTemplate template, UUID tenantId) {
        WorkflowStep step = new WorkflowStep();
        step.setName(stepDto.getName());
        step.setStepOrder(stepDto.getStepOrder());
        step.setWorkflowTemplate(template);
        step.setTenantId(tenantId);

        // Add approvers
        if (stepDto.getApprovers() != null) {
            for (WorkflowApproverDTO appDto : stepDto.getApprovers()) {
                WorkflowApprover approver = createApproverFromDTO(appDto, step, tenantId);
                step.getApprovers().add(approver);
            }
        }

        return step;
    }

    /**
     * Create WorkflowApprover from DTO
     */
    private WorkflowApprover createApproverFromDTO(WorkflowApproverDTO dto, WorkflowStep step, UUID tenantId) {
        WorkflowApprover approver = new WorkflowApprover();
        approver.setWorkflowStep(step);
        approver.setTenantId(tenantId);

        String type = dto.getApproverType().toUpperCase();

        switch (type) {
            case "USER":
                UUID userId = UUID.fromString(dto.getValue());
                userRepository.findById(userId).ifPresent(approver::setUser);
                break;
            case "ROLE":
                // Try to parse as UUID first, if that fails, treat as slug
                try {
                    UUID roleId = UUID.fromString(dto.getValue());
                    roleRepository.findById(roleId).ifPresent(approver::setRoleRequired);
                } catch (IllegalArgumentException e) {
                    // Not a UUID, lookup by slug
                    roleRepository.findBySlug(dto.getValue()).ifPresent(approver::setRoleRequired);
                }
                break;
            case "DEPT_HEAD":
                approver.setIsDepartmentHead(true);
                break;
            default:
                throw new RuntimeException("Unknown approver type: " + type);
        }

        return approver;
    }

    /**
     * Check if workflow is currently in use
     * Returns true if there are active requisitions/POs using this workflow
     */
    private boolean isInUse(WorkflowTemplate template) {
        // TODO: Implement check for active requisitions/POs using this workflow
        // For now, return false to allow editing
        // In production, query RequisitionApproval and POApproval tables
        return false;
    }

    /**
     * Get workflow by ID with all relationships loaded
     */
    @Transactional(readOnly = true)
    public Optional<WorkflowTemplate> getWorkflowById(UUID id) {
        Optional<WorkflowTemplate> result = workflowTemplateRepository.findByIdWithSteps(id);
        // Initialize approvers to avoid lazy loading
        result.ifPresent(w -> w.getSteps().forEach(step -> step.getApprovers().size()));
        return result;
    }

    /**
     * Get all workflows for current tenant
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplate> getAllWorkflows() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }
        List<WorkflowTemplate> workflows = workflowTemplateRepository.findAllByTenantIdWithSteps(tenantId);
        // Initialize approvers to avoid lazy loading
        workflows.forEach(w -> w.getSteps().forEach(step -> step.getApprovers().size()));
        return workflows;
    }

    /**
     * Delete workflow
     */
    @Transactional
    public void deleteWorkflow(UUID id) {
        log.info("Deleting workflow: {}", id);

        WorkflowTemplate template = workflowTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));

        if (template.getIsActive() && isInUse(template)) {
            throw new RuntimeException("Cannot delete active workflow that is currently in use");
        }

        workflowTemplateRepository.delete(template);
        log.info("Workflow deleted successfully: {}", id);
    }
}
