package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RequisitionSummaryDTO {
    private UUID id;
    private String code;
    private String title;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal quantityFulfilled;
    private String requesterName;
    private String departmentName;
}
