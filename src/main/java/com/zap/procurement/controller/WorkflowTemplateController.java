package com.zap.procurement.controller;

import com.zap.procurement.domain.WorkflowTemplate;
import com.zap.procurement.dto.WorkflowTemplateDTO;
import com.zap.procurement.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/workflows")
@CrossOrigin(origins = "*")
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
public class WorkflowTemplateController {

    @Autowired
    private WorkflowService workflowService;

    @GetMapping
    public ResponseEntity<List<WorkflowTemplateDTO>> getAllWorkflows() {
        try {
            List<WorkflowTemplate> workflows = workflowService.getAllWorkflows();
            return ResponseEntity.ok(workflows.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to get workflows", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkflowById(@PathVariable UUID id) {
        try {
            return workflowService.getWorkflowById(id)
                    .map(w -> ResponseEntity.ok(toDTO(w)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createWorkflow(@RequestBody WorkflowTemplateDTO dto) {
        try {
            WorkflowTemplate created = workflowService.createWorkflow(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
        } catch (RuntimeException e) {
            log.error("Failed to create workflow", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkflow(@PathVariable UUID id, @RequestBody WorkflowTemplateDTO dto) {
        try {
            WorkflowTemplate updated = workflowService.updateWorkflow(id, dto);
            return ResponseEntity.ok(toDTO(updated));
        } catch (RuntimeException e) {
            log.error("Failed to update workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkflow(@PathVariable UUID id) {
        try {
            workflowService.deleteWorkflow(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete workflow: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Convert WorkflowTemplate entity to DTO
     */
    private WorkflowTemplateDTO toDTO(WorkflowTemplate template) {
        WorkflowTemplateDTO dto = new WorkflowTemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setType(template.getType().name());
        dto.setIsActive(template.getIsActive());

        if (template.getSteps() != null) {
            dto.setSteps(template.getSteps().stream().map(step -> {
                var stepDto = new com.zap.procurement.dto.WorkflowStepDTO();
                stepDto.setId(step.getId());
                stepDto.setName(step.getName());
                stepDto.setStepOrder(step.getStepOrder());

                if (step.getApprovers() != null) {
                    stepDto.setApprovers(step.getApprovers().stream().map(approver -> {
                        var appDto = new com.zap.procurement.dto.WorkflowApproverDTO();
                        appDto.setId(approver.getId());

                        if (approver.getUser() != null) {
                            appDto.setApproverType("USER");
                            appDto.setValue(approver.getUser().getId().toString());
                            appDto.setUserName(approver.getUser().getName());
                            appDto.setUserId(approver.getUser().getId());
                        } else if (approver.getRoleRequired() != null) {
                            appDto.setApproverType("ROLE");
                            appDto.setValue(approver.getRoleRequired().getId().toString());
                            appDto.setRoleName(approver.getRoleRequired().getName());
                        } else if (Boolean.TRUE.equals(approver.getIsDepartmentHead())) {
                            appDto.setApproverType("DEPT_HEAD");
                            appDto.setValue("DEPT_HEAD");
                        }

                        return appDto;
                    }).collect(Collectors.toList()));
                }

                return stepDto;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
}
