package com.zap.procurement.domain;

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
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL)
    private List<PORequisition> purchaseOrders = new ArrayList<>();

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
