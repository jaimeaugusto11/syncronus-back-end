package com.zap.procurement.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PurchaseOrder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    // Many-to-many relationship with requisitions
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PORequisition> requisitions = new ArrayList<>();

    // Relationship with RFQ for tracking source
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "order_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
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

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<POItem> items = new ArrayList<>();

    @Column(name = "sent_to_supplier_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime sentToSupplierAt;

    @Column(name = "supplier_confirmed_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime supplierConfirmedAt;

    public enum POStatus {
        DRAFT, SENT_TO_SUPPLIER, SUPPLIER_CONFIRMED, IN_DELIVERY, COMPLETED, CANCELLED,
        PENDING_APPROVAL, APPROVED, REJECTED, PARTIALLY_RECEIVED
    }

    // Manual Getters and Setters to avoid Lombok issues in this environment
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<PORequisition> getRequisitions() {
        return requisitions;
    }

    public void setRequisitions(List<PORequisition> requisitions) {
        this.requisitions = requisitions;
    }

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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
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

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public POStatus getStatus() {
        return status;
    }

    public void setStatus(POStatus status) {
        this.status = status;
    }

    public List<POItem> getItems() {
        return items;
    }

    public void setItems(List<POItem> items) {
        this.items = items;
    }

    public LocalDateTime getSentToSupplierAt() {
        return sentToSupplierAt;
    }

    public void setSentToSupplierAt(LocalDateTime sentToSupplierAt) {
        this.sentToSupplierAt = sentToSupplierAt;
    }

    public LocalDateTime getSupplierConfirmedAt() {
        return supplierConfirmedAt;
    }

    public void setSupplierConfirmedAt(LocalDateTime supplierConfirmedAt) {
        this.supplierConfirmedAt = supplierConfirmedAt;
    }

    // Helper methods for requisitions
    public void addRequisition(Requisition req, BigDecimal quantityFulfilled) {
        if (requisitions == null)
            requisitions = new ArrayList<>();
        PORequisition link = new PORequisition();
        link.setPurchaseOrder(this);
        link.setRequisition(req);
        link.setQuantityFulfilled(quantityFulfilled);
        link.setTenantId(this.getTenantId());
        requisitions.add(link);
    }

    public void removeRequisition(Requisition req) {
        if (requisitions != null) {
            requisitions.removeIf(pr -> pr.getRequisition() != null && pr.getRequisition().getId().equals(req.getId()));
        }
    }
}
