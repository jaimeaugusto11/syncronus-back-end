package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "supplier_proposals")
@Data
@EqualsAndHashCode(callSuper = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class SupplierProposal extends BaseEntity {

    public SupplierProposal() {
        super();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    private String currency = "EUR";

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(name = "technical_score")
    private BigDecimal technicalScore;

    @Column(name = "financial_score")
    private BigDecimal financialScore;

    @Column(name = "final_score")
    private BigDecimal finalScore;

    @Column(name = "evaluation_notes", columnDefinition = "TEXT")
    private String evaluationNotes;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RFQQuestionResponse> questionResponses = new ArrayList<>();

    @Column(name = "proposal_number")
    private String proposalNumber;

    @Column(name = "proforma_url")
    private String proformaUrl;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalNegotiationMessage> negotiationMessages = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalPriceHistory> priceHistory = new ArrayList<>();

    public enum ProposalStatus {
        DRAFT, SUBMITTED, UNDER_EVALUATION, ACCEPTED, REJECTED, WITHDRAWN
    }

    // Manual Getters and Setters
    public RFQ getRfq() {
        return rfq;
    }

    public void setRfq(RFQ rfq) {
        this.rfq = rfq;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public BigDecimal getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(BigDecimal technicalScore) {
        this.technicalScore = technicalScore;
    }

    public BigDecimal getFinancialScore() {
        return financialScore;
    }

    public void setFinancialScore(BigDecimal financialScore) {
        this.financialScore = financialScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getEvaluationNotes() {
        return evaluationNotes;
    }

    public void setEvaluationNotes(String evaluationNotes) {
        this.evaluationNotes = evaluationNotes;
    }

    public List<ProposalItem> getItems() {
        return items;
    }

    public void setItems(List<ProposalItem> items) {
        this.items = items;
    }

    public String getProposalNumber() {
        return proposalNumber;
    }

    public void setProposalNumber(String proposalNumber) {
        this.proposalNumber = proposalNumber;
    }

    public String getProformaUrl() {
        return proformaUrl;
    }

    public void setProformaUrl(String proformaUrl) {
        this.proformaUrl = proformaUrl;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public List<ProposalNegotiationMessage> getNegotiationMessages() {
        return negotiationMessages;
    }

    public void setNegotiationMessages(List<ProposalNegotiationMessage> negotiationMessages) {
        this.negotiationMessages = negotiationMessages;
    }

    public List<ProposalPriceHistory> getPriceHistory() {
        return priceHistory;
    }

    public void setPriceHistory(List<ProposalPriceHistory> priceHistory) {
        this.priceHistory = priceHistory;
    }
}
