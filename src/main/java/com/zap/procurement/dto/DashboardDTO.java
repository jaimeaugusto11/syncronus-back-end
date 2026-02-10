package com.zap.procurement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DashboardDTO {
    // Overview metrics
    private Integer totalRequisitions;
    private Integer pendingApprovals;
    private Integer activeRFQs;
    private Integer activePOs;

    // Financial metrics
    private BigDecimal totalSpend;
    private BigDecimal pendingInvoices;
    private BigDecimal savingsAchieved;
    private BigDecimal budgetUtilization;

    // Supplier metrics
    private Integer activeSuppliers;
    private BigDecimal averageSupplierRating;

    // Process metrics
    private Double averageApprovalTime; // in days
    private Double averagePOCycleTime; // in days
    private Integer invoicesAwaitingMatch;

    // Spend by category
    private Map<String, BigDecimal> spendByCategory;

    // Spend by department
    private Map<String, BigDecimal> spendByDepartment;

    // Recent activity counts
    private Integer requisitionsThisMonth;
    private Integer rfqsThisMonth;
    private Integer posThisMonth;
}
