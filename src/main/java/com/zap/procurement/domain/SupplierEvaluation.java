package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_evaluations")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierEvaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "delivery_score")
    private BigDecimal deliveryScore;

    @Column(name = "quality_score")
    private BigDecimal qualityScore;

    @Column(name = "price_score")
    private BigDecimal priceScore;

    @Column(name = "overall_score")
    private BigDecimal overallScore;

    @Column(name = "evaluation_date")
    private LocalDateTime evaluationDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Manual Getters and Setters
    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getDeliveryScore() {
        return deliveryScore;
    }

    public void setDeliveryScore(BigDecimal deliveryScore) {
        this.deliveryScore = deliveryScore;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public BigDecimal getPriceScore() {
        return priceScore;
    }

    public void setPriceScore(BigDecimal priceScore) {
        this.priceScore = priceScore;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public LocalDateTime getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(LocalDateTime evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
