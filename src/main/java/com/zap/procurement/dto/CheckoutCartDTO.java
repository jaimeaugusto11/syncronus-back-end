package com.zap.procurement.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class CheckoutCartDTO {
    private String title;
    private String justification;
    private String priority; // e.g., LOW, MEDIUM, HIGH, URGENT
    private UUID departmentId;
    private UUID budgetLineId;
    private List<CartItemDTO> items;

    @Data
    public static class CartItemDTO {
        private UUID catalogItemId;
        private Double quantity;
    }
}
