package com.zap.procurement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "requisition_items")
@Data
@EqualsAndHashCode(callSuper = true)
public class RequisitionItem extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    private Requisition requisition;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String unit;

    @Column(name = "estimated_price")
    private BigDecimal estimatedPrice;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_supplier_id")
    private Supplier preferredSupplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequisitionItemStatus status = RequisitionItemStatus.DRAFT;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = RequisitionItemStatus.DRAFT;
        }
    }

    public enum RequisitionItemStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        IN_SOURCING,
        QUOTED,
        AWARDED,
        PO_CREATED,
        COMPLETED,
        CANCELLED
    }

    // Manual Getters and Setters
    public Requisition getRequisition() {
        return requisition;
    }

    public void setRequisition(Requisition requisition) {
        this.requisition = requisition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Supplier getPreferredSupplier() {
        return preferredSupplier;
    }

    public void setPreferredSupplier(Supplier preferredSupplier) {
        this.preferredSupplier = preferredSupplier;
    }

    public RequisitionItemStatus getStatus() {
        return status;
    }

    public void setStatus(RequisitionItemStatus status) {
        this.status = status;
    }
}
