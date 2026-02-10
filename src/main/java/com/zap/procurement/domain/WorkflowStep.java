package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps")
@Data
public class WorkflowStep {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false, length = 16)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", nullable = false)
    private WorkflowTemplate workflowTemplate;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalType type = ApprovalType.SEQUENTIAL;

    @Column(name = "tenant_id", nullable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID tenantId;

    @OneToMany(mappedBy = "workflowStep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "workflowStep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowApprover> approvers = new ArrayList<>();

    public enum ApprovalType {
        SEQUENTIAL, PARALLEL
    }
}
