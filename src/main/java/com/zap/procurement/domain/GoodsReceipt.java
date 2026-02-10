package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts")
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsReceipt extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code; // GR-2024-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_id", nullable = false)
    private User receivedBy;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "warehouse_location")
    private String warehouseLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false)
    private ReceiptType receiptType;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", nullable = false)
    private QualityStatus qualityStatus = QualityStatus.PENDING;

    @Column(name = "quality_notes", columnDefinition = "TEXT")
    private String qualityNotes;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GRItem> items = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ReceiptType {
        FULL, PARTIAL
    }

    public enum QualityStatus {
        PENDING, APPROVED, REJECTED, CONDITIONAL_APPROVAL
    }
}
