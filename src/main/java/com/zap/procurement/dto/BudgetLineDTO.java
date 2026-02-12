package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BudgetLineDTO {
    private UUID id;
    private String code;
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private String equipmentList;
    private UUID departmentId;
    private String departmentName;
    private Boolean active;
}
