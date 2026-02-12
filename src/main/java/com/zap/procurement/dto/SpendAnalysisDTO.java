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

    // Dynamic spend breakdown
    private java.util.Map<String, BigDecimal> spendByCategory;
    private java.util.Map<String, BigDecimal> spendByDepartment;

    // Comparison
    private BigDecimal previousPeriodSpend;
    private Double changePercentage;

    // Savings
    private BigDecimal targetSavings;
    private BigDecimal actualSavings;
    private Double savingsRate; // percentage
}
