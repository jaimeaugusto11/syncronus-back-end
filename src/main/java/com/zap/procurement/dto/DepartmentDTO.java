package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DepartmentDTO {
    private UUID id;
    private String name;
    private String description;
    private String costCenter;
    private String budgetType;
    private UUID parentId;

    // Head info
    private UUID headId;
    private String headName;
    private String headAvatarUrl;

    // Calculated
    private long memberCount;
    private boolean active;
}
