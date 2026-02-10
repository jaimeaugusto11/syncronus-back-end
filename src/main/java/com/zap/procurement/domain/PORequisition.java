package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "po_requisitions")
@Data
@EqualsAndHashCode(callSuper = true)
public class PORequisition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    private Requisition requisition;

    @Column(name = "quantity_fulfilled", precision = 10, scale = 2)
    private BigDecimal quantityFulfilled;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
