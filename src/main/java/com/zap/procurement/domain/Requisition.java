package com.zap.procurement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requisitions")
@Data
@EqualsAndHashCode(callSuper = true)
public class Requisition extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type")
    private Department.BudgetType budgetType;

    @Column(name = "cost_center")
    private String costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_line_id")
    private BudgetLine budgetLine;

    @Column(name = "is_extraordinary")
    private boolean extraordinary = false;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequisitionStatus status = RequisitionStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "needed_by")
    private LocalDate neededBy;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequisitionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequisitionApproval> approvals = new ArrayList<>();

    // Purchase orders generated from this requisition
    @JsonIgnore
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL)
    private List<PORequisition> purchaseOrders = new ArrayList<>();

    // Manual Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Department.BudgetType getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(Department.BudgetType budgetType) {
        this.budgetType = budgetType;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public BudgetLine getBudgetLine() {
        return budgetLine;
    }

    public void setBudgetLine(BudgetLine budgetLine) {
        this.budgetLine = budgetLine;
    }

    public boolean isExtraordinary() {
        return extraordinary;
    }

    public void setExtraordinary(boolean extraordinary) {
        this.extraordinary = extraordinary;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public RequisitionStatus getStatus() {
        return status;
    }

    public void setStatus(RequisitionStatus status) {
        this.status = status;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getNeededBy() {
        return neededBy;
    }

    public void setNeededBy(LocalDate neededBy) {
        this.neededBy = neededBy;
    }

    public List<RequisitionItem> getItems() {
        return items;
    }

    public void setItems(List<RequisitionItem> items) {
        this.items = items;
    }

    public List<RequisitionApproval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<RequisitionApproval> approvals) {
        this.approvals = approvals;
    }

    public List<PORequisition> getPurchaseOrders() {
        return purchaseOrders;
    }

    public void setPurchaseOrders(List<PORequisition> purchaseOrders) {
        this.purchaseOrders = purchaseOrders;
    }

    public enum RequisitionStatus {
        DRAFT,
        PENDING_APPROVAL,
        DEPT_HEAD_APPROVAL,
        DEPT_DIRECTOR_APPROVAL,
        GENERAL_DIRECTOR_APPROVAL,
        APPROVED,
        REJECTED,
        CONVERTED_TO_PO, // Matched CONVERTIDA_EM_PO requirement
        IN_PROCUREMENT,
        COMPLETED,
        CANCELLED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
