package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "po_requisitions")
@Data
@EqualsAndHashCode(callSuper = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PORequisition extends BaseEntity {

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requisition_id", nullable = false)
    private Requisition requisition;

    @Column(name = "quantity_fulfilled", precision = 10, scale = 2)
    private BigDecimal quantityFulfilled;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Manual Getters and Setters
    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Requisition getRequisition() {
        return requisition;
    }

    public void setRequisition(Requisition requisition) {
        this.requisition = requisition;
    }

    public BigDecimal getQuantityFulfilled() {
        return quantityFulfilled;
    }

    public void setQuantityFulfilled(BigDecimal quantityFulfilled) {
        this.quantityFulfilled = quantityFulfilled;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
