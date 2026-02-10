package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_lines")
@Data
@EqualsAndHashCode(callSuper = true)
public class BudgetLine extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String equipmentList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    private boolean active = true;

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(spentAmount);
    }
}
