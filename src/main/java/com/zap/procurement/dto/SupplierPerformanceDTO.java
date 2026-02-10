package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierPerformanceDTO {
    private java.util.UUID supplierId;
    private String supplierName;
    private String supplierCode;

    // Performance metrics
    private Integer totalOrders;
    private BigDecimal totalSpend;
    private Double onTimeDeliveryRate; // percentage
    private Double qualityScore; // 0-100
    private Double averageLeadTime; // in days

    // Financial metrics
    private BigDecimal averageOrderValue;
    private Integer invoicesSubmitted;
    private Integer invoicesMatched;
    private Double matchSuccessRate; // percentage

    // Compliance
    private Integer lateDeliveries;
    private Integer qualityIssues;
    private String riskRating; // LOW, MEDIUM, HIGH
}
