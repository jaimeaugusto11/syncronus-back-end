package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RequisitionLinkDTO {
    private UUID requisitionId;
    private BigDecimal quantityFulfilled;
    private String notes;
}
