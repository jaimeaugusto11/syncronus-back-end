package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PurchaseOrderSummaryDTO {
    private UUID id;
    private String code;
    private String status;
    private String supplierName;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
}
