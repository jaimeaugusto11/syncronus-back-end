package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CategoryAssignmentDTO {
    private UUID categoryId;
    private Boolean isPrimary;
    private BigDecimal rating;
    private String notes;
}
