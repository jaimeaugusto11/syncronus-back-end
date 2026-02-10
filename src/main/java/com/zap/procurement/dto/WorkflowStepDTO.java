package com.zap.procurement.dto;

import java.util.List;
import java.util.UUID;

public class WorkflowStepDTO {
    private UUID id;
    private Integer stepOrder;
    private String name;
    private List<WorkflowApproverDTO> approvers;

    public WorkflowStepDTO() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WorkflowApproverDTO> getApprovers() {
        return approvers;
    }

    public void setApprovers(List<WorkflowApproverDTO> approvers) {
        this.approvers = approvers;
    }
}
