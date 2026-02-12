package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfqs")
@Data
@EqualsAndHashCode(callSuper = true)
public class RFQ extends BaseEntity {

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
    @Column(nullable = false)
    private RFQStatus status = RFQStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RFQType type = RFQType.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false)
    private ProcessType processType = ProcessType.RFQ;

    public enum RFQStatus {
        DRAFT, OPEN, PUBLISHED, CLOSED, AWARDED, CANCELLED
    }

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RFQItem> items = new ArrayList<>();

    public enum RFQType {
        PUBLIC, PRIVATE, CLOSED
    }

    public enum ProcessType {
        RFQ, RFP
    }
}
