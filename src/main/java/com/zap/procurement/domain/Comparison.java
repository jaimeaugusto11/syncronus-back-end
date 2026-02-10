package com.zap.procurement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comparisons")
@Data
@EqualsAndHashCode(callSuper = true)
public class Comparison extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToMany
    @JoinTable(name = "comparison_proposals", joinColumns = @JoinColumn(name = "comparison_id"), inverseJoinColumns = @JoinColumn(name = "proposal_id"))
    private List<SupplierProposal> proposals = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_proposal_id")
    private SupplierProposal selectedProposal;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComparisonStatus status = ComparisonStatus.DRAFT;

    @Column(name = "technical_weight")
    private Integer technicalWeight = 60;

    @Column(name = "financial_weight")
    private Integer financialWeight = 40;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum ComparisonStatus {
        DRAFT, IN_REVIEW, COMPLETED, ADJUDICATED
    }
}
