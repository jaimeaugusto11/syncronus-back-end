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
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RFQItem extends BaseEntity {

    public RFQItem() {
        super();
    }

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_proposal_item_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "rfqItem", "hibernateLazyInitializer", "handler" })
    private ProposalItem awardedProposalItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RFQItemStatus status = RFQItemStatus.PENDING;

    @Column(name = "lot_number")
    private Integer lotNumber;

    @Column(name = "lot_name")
    private String lotName;

    public enum RFQItemStatus {
        PENDING,
        AWARDED,
        CANCELLED
    }

    // Manual Getters and Setters
    public RFQ getRfq() {
        return rfq;
    }

    public void setRfq(RFQ rfq) {
        this.rfq = rfq;
    }

    public RequisitionItem getRequisitionItem() {
        return requisitionItem;
    }

    public void setRequisitionItem(RequisitionItem requisitionItem) {
        this.requisitionItem = requisitionItem;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
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

    public ProposalItem getAwardedProposalItem() {
        return awardedProposalItem;
    }

    public void setAwardedProposalItem(ProposalItem awardedProposalItem) {
        this.awardedProposalItem = awardedProposalItem;
    }

    public RFQItemStatus getStatus() {
        return status;
    }

    public void setStatus(RFQItemStatus status) {
        this.status = status;
    }

    public Integer getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(Integer lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }
}
