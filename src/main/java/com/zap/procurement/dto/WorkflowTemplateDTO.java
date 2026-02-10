package com.zap.procurement.dto;

import java.util.List;
import java.util.UUID;

public class WorkflowTemplateDTO {
    private UUID id;
    private String name;
    private String type;
    private Boolean isActive;
    private List<WorkflowStepDTO> steps;

    public WorkflowTemplateDTO() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<WorkflowStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStepDTO> steps) {
        this.steps = steps;
    }
}
