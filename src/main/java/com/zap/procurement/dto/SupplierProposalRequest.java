package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SupplierProposalRequest {
    private UUID rfqId;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate deliveryDate;
    private String paymentTerms;
    private String notes;
    private String proposalNumber;
    private String documentUrl;
    private String proformaUrl;
    private List<ProposalItemRequest> items;

    @Data
    public static class ProposalItemRequest {
        private UUID rfqItemId;
        private String quantity; // Frontend sends string "10.00"
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String description;
        private String comments;
    }
}
