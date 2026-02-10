package com.zap.procurement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "rfq_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class RFQItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_item_id")
    private RequisitionItem requisitionItem;

    @Column(nullable = false)
    private String description;

    @Column(length = 2000)
    private String specifications;

    @Column(nullable = false)
    private BigDecimal quantity;

    private String unit;

    @Column(name = "estimated_price")
    private BigDecimal estimatedPrice;

    @Column(length = 1000)
    private String notes;
}
