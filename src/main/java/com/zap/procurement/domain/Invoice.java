package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "supplier_invoice_number", nullable = false)
    private String supplierInvoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    private String currency = "EUR";

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status")
    private MatchStatus matchStatus = MatchStatus.PENDING;

    @Column(name = "match_notes", columnDefinition = "TEXT")
    private String matchNotes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    @Column(name = "is_proforma")
    private boolean proforma = false;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public enum InvoiceStatus {
        RECEIVED, UNDER_REVIEW, MATCHED, MISMATCH, APPROVED, REJECTED, PAID
    }

    public enum MatchStatus {
        PENDING, MATCHED, QUANTITY_MISMATCH, PRICE_MISMATCH, ITEM_MISMATCH, MULTIPLE_ISSUES
    }
}
