package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SpendAnalysisDTO {
    private LocalDate period;

    // Total spend
    private BigDecimal totalSpend;
    private BigDecimal capexSpend;
    private BigDecimal opexSpend;

    // By category
    private BigDecimal itSpend;
    private BigDecimal facilitiesSpend;
    private BigDecimal servicesSpend;
    private BigDecimal materialsSpend;
    private BigDecimal otherSpend;

    // Comparison
    private BigDecimal previousPeriodSpend;
    private Double changePercentage;

    // Savings
    private BigDecimal targetSavings;
    private BigDecimal actualSavings;
    private Double savingsRate; // percentage
}
