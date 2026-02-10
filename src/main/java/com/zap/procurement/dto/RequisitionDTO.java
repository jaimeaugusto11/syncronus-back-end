package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class RequisitionDTO {
    private UUID id;
    private String title;
    private String code;
    private UUID requesterId;
    private String requesterName;
    private UUID departmentId;
    private String departmentName;
    private String budgetType;
    private String costCenter;
    private BigDecimal totalAmount;
    private String status;
    private String justification;
    private String priority;
    private UUID budgetLineId;
    private String budgetLineCode;
    private boolean extraordinary;
    private java.time.LocalDate neededBy;
    private String createdAt;
    private List<RequisitionItemDTO> items;
}
