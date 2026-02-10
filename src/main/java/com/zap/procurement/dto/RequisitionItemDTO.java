package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RequisitionItemDTO {
    private UUID id;
    private String description;
    private String specifications;
    private Integer quantity;
    private String unit;
    private BigDecimal estimatedUnitPrice;
    private BigDecimal estimatedTotalPrice;
    private String notes;
    private UUID categoryId;
    private String categoryName;
    private UUID preferredSupplierId;
    private String preferredSupplierName;
}
