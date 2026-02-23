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
    private boolean isSealed;

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
        private UUID poId;
        private String poCode;
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
        private Integer lotNumber;
        private String lotName;
    }

    @Data
    @Builder
    public static class SupplierItemPriceDTO {
        private UUID supplierId;
        private UUID proposalItemId;
        private String supplierName;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private boolean isLowest;
        private UUID poId;
        private String poCode;
        private String status;
    }
}
