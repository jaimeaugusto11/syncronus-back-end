package com.zap.procurement.dto;

import com.zap.procurement.domain.Supplier;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SupplierSuggestionDTO {
    private UUID id;
    private String name;
    private String email;
    private String contactPerson;
    private Boolean isPrimaryCategory;
    private BigDecimal rating;
    private Supplier.RiskRating riskRating;
    private Boolean isPreferred;
}
