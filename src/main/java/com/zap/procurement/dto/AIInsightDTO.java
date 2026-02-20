package com.zap.procurement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIInsightDTO {
    private String title;
    private String description;
    private InsightType type;
    private ImpactLevel impact;
    private BigDecimal estimatedSavings;
    private String recommendation;

    public enum InsightType {
        CONSOLIDATION, SUPPLIER_SWITCH, CONTRACT_COMPLIANCE, MARKET_TREND
    }

    public enum ImpactLevel {
        HIGH, MEDIUM, LOW
    }
}
