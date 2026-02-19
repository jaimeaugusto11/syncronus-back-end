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
public class SupplierProposal extends BaseEntity {

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
}
