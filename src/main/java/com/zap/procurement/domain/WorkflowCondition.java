package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "workflow_conditions")
@Data
public class WorkflowCondition {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false, length = 16)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_id", nullable = false)
    private WorkflowStep workflowStep;

    @Column(nullable = false)
    private String field;

    @Column(nullable = false)
    private String operator;

    @Column(nullable = false)
    private String value;

    @Column(name = "tenant_id", nullable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID tenantId;
}
