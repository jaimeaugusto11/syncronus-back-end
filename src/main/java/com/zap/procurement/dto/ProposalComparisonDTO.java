package com.zap.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ProposalComparisonDTO {
    private UUID rfqId;
    private String rfqCode;
    private String rfqTitle;
    private String rfqStatus;
    private List<SupplierSummaryDTO> summaries;
    private List<ItemComparisonDTO> items;
    private String AIAnalysis;

    @Data
    @Builder
    public static class SupplierSummaryDTO {
        private UUID proposalId;
        private UUID supplierId;
        private String supplierName;
        private String supplierCode;
        private BigDecimal totalAmount;
        private String currency;
        private Integer deliveryDays;
        private String paymentTerms;
        private BigDecimal technicalScore;
        private BigDecimal financialScore;
        private BigDecimal finalScore;
        private String status;
        private boolean isCheapest;
        private boolean isFastest;
        private boolean isBestScored;
    }

    @Data
    @Builder
    public static class ItemComparisonDTO {
        private UUID rfqItemId;
        private String productName;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private List<SupplierItemPriceDTO> prices;
        private BigDecimal targetPrice;
    }

    @Data
    @Builder
    public static class SupplierItemPriceDTO {
        private UUID supplierId;
        private String supplierName;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private boolean isLowest;
    }
}
