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
@Table(name = "purchase_orders")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    // DEPRECATED: Will be removed after migration to po_requisitions table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id")
    private Requisition requisition;

    // NEW: Many-to-many relationship with requisitions
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PORequisition> requisitions = new ArrayList<>();

    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    private String currency = "EUR";

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private POStatus status = POStatus.DRAFT;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<POItem> items = new ArrayList<>();

    @Column(name = "sent_to_supplier_at")
    private LocalDateTime sentToSupplierAt;

    @Column(name = "supplier_confirmed_at")
    private LocalDateTime supplierConfirmedAt;

    public enum POStatus {
        DRAFT, SENT_TO_SUPPLIER, SUPPLIER_CONFIRMED, IN_DELIVERY, COMPLETED, CANCELLED,
        PENDING_APPROVAL, APPROVED, REJECTED, PARTIALLY_RECEIVED
    }

    // Helper methods for requisitions
    public void addRequisition(Requisition req, BigDecimal quantityFulfilled) {
        PORequisition link = new PORequisition();
        link.setPurchaseOrder(this);
        link.setRequisition(req);
        link.setQuantityFulfilled(quantityFulfilled);
        link.setTenantId(this.getTenantId());
        requisitions.add(link);
    }

    public void removeRequisition(Requisition req) {
        requisitions.removeIf(pr -> pr.getRequisition().equals(req));
    }
}
