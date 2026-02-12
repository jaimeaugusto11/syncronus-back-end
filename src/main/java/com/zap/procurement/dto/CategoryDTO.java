package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CategoryDTO {
    private UUID id;
    private String name;
    private String icon;
    private String description;
    private UUID parentId;
    private Boolean active;
}
