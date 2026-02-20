package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfqs")
@Data
@EqualsAndHashCode(callSuper = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RFQ extends BaseEntity {

    @Formula("(SELECT COALESCE(SUM(ri.estimated_price * ri.quantity), 0) FROM rfq_items ri WHERE ri.rfq_id = id)")
    private BigDecimal estimatedValue;

    @Formula("(SELECT COUNT(*) FROM supplier_proposals p WHERE p.rfq_id = id)")
    private Long proposalsCount;

    public RFQ() {
        super();
    }

    @Column(nullable = false, unique = true)
    private String code;

    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id")
    private Requisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RFQStatus status = RFQStatus.DRAFT;

    @Column(name = "technical_weight")
    private Integer technicalWeight = 60;

    @Column(name = "financial_weight")
    private Integer financialWeight = 40;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RFQType type = RFQType.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false)
    private ProcessType processType = ProcessType.RFQ;

    public enum RFQStatus {
        DRAFT, OPEN, PUBLISHED, READY_COMPARE, @Deprecated
        READY_FOR_COMPARISON, TECHNICAL_VALIDATION, PENDING_BAFO, BAFO_OPEN, CLOSED, PARTIALLY_AWARDED, AWARDED,
        CANCELLED
    }

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RFQItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RFQQuestion> questions = new ArrayList<>();

    public enum RFQType {
        PUBLIC, PRIVATE, CLOSED
    }

    public enum ProcessType {
        RFQ, RFP
    }

    // Manual Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Requisition getRequisition() {
        return requisition;
    }

    public void setRequisition(Requisition requisition) {
        this.requisition = requisition;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(LocalDate closingDate) {
        this.closingDate = closingDate;
    }

    public RFQStatus getStatus() {
        return status;
    }

    public void setStatus(RFQStatus status) {
        this.status = status;
    }

    public Integer getTechnicalWeight() {
        return technicalWeight;
    }

    public void setTechnicalWeight(Integer technicalWeight) {
        this.technicalWeight = technicalWeight;
    }

    public Integer getFinancialWeight() {
        return financialWeight;
    }

    public void setFinancialWeight(Integer financialWeight) {
        this.financialWeight = financialWeight;
    }

    public RFQType getType() {
        return type;
    }

    public void setType(RFQType type) {
        this.type = type;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public List<RFQItem> getItems() {
        return items;
    }

    public void setItems(List<RFQItem> items) {
        this.items = items;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public void setEstimatedValue(BigDecimal estimatedValue) {
        this.estimatedValue = estimatedValue;
    }

    public Long getProposalsCount() {
        return proposalsCount;
    }

    public void setProposalsCount(Long proposalsCount) {
        this.proposalsCount = proposalsCount;
    }
}
