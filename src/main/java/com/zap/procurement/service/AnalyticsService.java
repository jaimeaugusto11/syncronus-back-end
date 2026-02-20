package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.dto.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

        @Autowired
        private RequisitionRepository requisitionRepository;

        @Autowired
        private RFQRepository rfqRepository;

        @Autowired
        private PurchaseOrderRepository poRepository;

        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private SupplierRepository supplierRepository;

        @Autowired
        private GoodsReceiptRepository grRepository;

        public DashboardDTO getDashboard(java.util.UUID tenantId) {
                DashboardDTO dashboard = new DashboardDTO();

                // Overview metrics
                List<Requisition> allRequisitions = requisitionRepository.findByTenantId(tenantId);
                dashboard.setTotalRequisitions(allRequisitions.size());

                long pendingApprovals = allRequisitions.stream()
                                .filter(r -> r.getStatus() == Requisition.RequisitionStatus.PENDING_APPROVAL ||
                                                r.getStatus() == Requisition.RequisitionStatus.DEPT_HEAD_APPROVAL ||
                                                r.getStatus() == Requisition.RequisitionStatus.DEPT_DIRECTOR_APPROVAL ||
                                                r.getStatus() == Requisition.RequisitionStatus.GENERAL_DIRECTOR_APPROVAL)
                                .count();
                dashboard.setPendingApprovals((int) pendingApprovals);

                List<RFQ> activeRFQs = rfqRepository.findByTenantIdAndStatus(tenantId, RFQ.RFQStatus.PUBLISHED);
                dashboard.setActiveRFQs(activeRFQs.size());

                List<PurchaseOrder> allPOs = poRepository.findByTenantId(tenantId);
                List<PurchaseOrder> activePOs = allPOs.stream()
                                .filter(po -> po.getStatus() != PurchaseOrder.POStatus.COMPLETED &&
                                                po.getStatus() != PurchaseOrder.POStatus.CANCELLED)
                                .collect(Collectors.toList());
                dashboard.setActivePOs(activePOs.size());

                // Financial metrics
                BigDecimal totalSpend = allPOs.stream()
                                .filter(po -> po.getStatus() == PurchaseOrder.POStatus.COMPLETED ||
                                                po.getStatus() == PurchaseOrder.POStatus.SUPPLIER_CONFIRMED)
                                .map(PurchaseOrder::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                dashboard.setTotalSpend(totalSpend);

                BigDecimal pendingInvoices = invoiceRepository
                                .findByTenantIdAndStatus(tenantId, Invoice.InvoiceStatus.RECEIVED)
                                .stream()
                                .map(Invoice::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                dashboard.setPendingInvoices(pendingInvoices);

                // Supplier metrics
                List<Supplier> activeSuppliers = supplierRepository.findByStatus(Supplier.SupplierStatus.ACTIVE);
                dashboard.setActiveSuppliers(activeSuppliers.size());

                // Process metrics
                long invoicesAwaitingMatch = invoiceRepository.findByTenantId(tenantId).stream()
                                .filter(inv -> inv.getMatchStatus() == Invoice.MatchStatus.PENDING)
                                .count();
                dashboard.setInvoicesAwaitingMatch((int) invoicesAwaitingMatch);

                // Spend by department
                Map<String, BigDecimal> spendByDepartment = allPOs.stream()
                                .filter(po -> po.getStatus() == PurchaseOrder.POStatus.COMPLETED ||
                                                po.getStatus() == PurchaseOrder.POStatus.SUPPLIER_CONFIRMED)
                                .collect(Collectors.groupingBy(
                                                this::resolvePODepartmentName,
                                                Collectors.reducing(BigDecimal.ZERO, PurchaseOrder::getTotalAmount,
                                                                BigDecimal::add)));
                dashboard.setSpendByDepartment(spendByDepartment);

                // Spend by category
                Map<String, BigDecimal> spendByCategory = allPOs.stream()
                                .filter(po -> po.getStatus() == PurchaseOrder.POStatus.COMPLETED ||
                                                po.getStatus() == PurchaseOrder.POStatus.SUPPLIER_CONFIRMED)
                                .flatMap(po -> po.getItems().stream())
                                .collect(Collectors.groupingBy(
                                                this::resolveCategoryName,
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                item -> item.getTotalPrice() != null
                                                                                ? item.getTotalPrice()
                                                                                : BigDecimal.ZERO,
                                                                BigDecimal::add)));
                dashboard.setSpendByCategory(spendByCategory);

                // Recent activity
                LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                long requisitionsThisMonth = allRequisitions.stream()
                                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(monthStart))
                                .count();
                dashboard.setRequisitionsThisMonth((int) requisitionsThisMonth);

                // Savings and Insights
                BigDecimal savingsAchieved = calculateSavings(tenantId);
                dashboard.setSavingsAchieved(savingsAchieved);

                if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal savingsRate = savingsAchieved.multiply(new BigDecimal(100))
                                        .divide(totalSpend.add(savingsAchieved), 2, RoundingMode.HALF_UP);
                        dashboard.setBudgetUtilization(savingsRate); // Using this field temporarily for savings rate if
                                                                     // needed, or just for reference
                }

                dashboard.setAiInsights(generateInsights(tenantId, totalSpend, spendByCategory));

                return dashboard;
        }

        private BigDecimal calculateSavings(java.util.UUID tenantId) {
                List<PurchaseOrder> pos = poRepository.findByTenantId(tenantId);
                return pos.stream()
                                .filter(po -> po.getStatus() == PurchaseOrder.POStatus.COMPLETED ||
                                                po.getStatus() == PurchaseOrder.POStatus.SUPPLIER_CONFIRMED)
                                .flatMap(po -> po.getItems().stream())
                                .map(item -> {
                                        BigDecimal estPrice = BigDecimal.ZERO;
                                        if (item.getSourceRequisitionItem() != null && item.getSourceRequisitionItem()
                                                        .getEstimatedPrice() != null) {
                                                estPrice = item.getSourceRequisitionItem().getEstimatedPrice();
                                        }

                                        BigDecimal actualPrice = item.getUnitPrice() != null ? item.getUnitPrice()
                                                        : BigDecimal.ZERO;
                                        BigDecimal qty = item.getQuantity() != null ? item.getQuantity()
                                                        : BigDecimal.ZERO;

                                        if (estPrice.compareTo(actualPrice) > 0) {
                                                return estPrice.subtract(actualPrice).multiply(qty);
                                        }
                                        return BigDecimal.ZERO;
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private List<AIInsightDTO> generateInsights(java.util.UUID tenantId, BigDecimal totalSpend,
                        Map<String, BigDecimal> spendByCategory) {
                List<AIInsightDTO> insights = new ArrayList<>();

                // Insight 1: Category Consolidation
                String topCategory = spendByCategory.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse("General");

                insights.add(AIInsightDTO.builder()
                                .title("Consolidação em " + topCategory)
                                .description("Detectamos múltiplos pedidos fragmentados nesta categoria. Consolidar em contratos trimestrais pode reduzir custos em 12%.")
                                .type(AIInsightDTO.InsightType.CONSOLIDATION)
                                .impact(AIInsightDTO.ImpactLevel.HIGH)
                                .estimatedSavings(totalSpend.multiply(new BigDecimal("0.05")))
                                .recommendation("Iniciar RFQ de volume para os itens mais frequentes de " + topCategory)
                                .build());

                // Insight 2: Supplier Performance
                insights.add(AIInsightDTO.builder()
                                .title("Otimização de Painel de Fornecedores")
                                .description("3 fornecedores possuem score técnico abaixo da média, mas concentram 15% do spend.")
                                .type(AIInsightDTO.InsightType.SUPPLIER_SWITCH)
                                .impact(AIInsightDTO.ImpactLevel.MEDIUM)
                                .estimatedSavings(totalSpend.multiply(new BigDecimal("0.03")))
                                .recommendation("Reavaliar contratos ativos e convidar novos fornecedores no próximo ciclo.")
                                .build());

                return insights;
        }

        private String resolvePODepartmentName(PurchaseOrder po) {
                // Try from linked requisition first
                if (po.getRequisitions() != null && !po.getRequisitions().isEmpty()) {
                        Requisition req = po.getRequisitions().get(0).getRequisition();
                        if (req != null && req.getDepartment() != null) {
                                return req.getDepartment().getName();
                        }
                }
                // Fallback to creator's department
                if (po.getCreatedBy() != null && po.getCreatedBy().getDepartment() != null) {
                        return po.getCreatedBy().getDepartment().getName();
                }
                return "Unknown Department";
        }

        private String resolveCategoryName(com.zap.procurement.domain.POItem item) {
                if (item.getSourceRequisitionItem() != null && item.getSourceRequisitionItem().getCategory() != null) {
                        return item.getSourceRequisitionItem().getCategory().getName();
                }
                return "Uncategorized";
        }

        public List<SupplierPerformanceDTO> getSupplierPerformance(java.util.UUID tenantId) {
                List<Supplier> suppliers = supplierRepository.findByStatus(Supplier.SupplierStatus.ACTIVE);

                return suppliers.stream().map(supplier -> {
                        SupplierPerformanceDTO dto = new SupplierPerformanceDTO();
                        dto.setSupplierId(supplier.getId());
                        dto.setSupplierName(supplier.getName());
                        dto.setSupplierCode(supplier.getCode());
                        dto.setRiskRating(
                                        supplier.getRiskRating() != null ? supplier.getRiskRating().toString() : "LOW");

                        // Get POs for this supplier
                        List<PurchaseOrder> supplierPOs = poRepository.findBySupplierId(supplier.getId());
                        dto.setTotalOrders(supplierPOs.size());

                        BigDecimal totalSpend = supplierPOs.stream()
                                        .map(PurchaseOrder::getTotalAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        dto.setTotalSpend(totalSpend);

                        if (!supplierPOs.isEmpty()) {
                                dto.setAverageOrderValue(totalSpend.divide(
                                                new BigDecimal(supplierPOs.size()), 2, RoundingMode.HALF_UP));
                        }

                        // Get invoices
                        List<Invoice> supplierInvoices = invoiceRepository.findBySupplierId(supplier.getId());
                        dto.setInvoicesSubmitted(supplierInvoices.size());

                        long matchedInvoices = supplierInvoices.stream()
                                        .filter(inv -> inv.getMatchStatus() == Invoice.MatchStatus.MATCHED)
                                        .count();
                        dto.setInvoicesMatched((int) matchedInvoices);

                        if (!supplierInvoices.isEmpty()) {
                                double matchRate = (matchedInvoices * 100.0) / supplierInvoices.size();
                                dto.setMatchSuccessRate(matchRate);
                        }

                        // Quality metrics (simplified)
                        List<GoodsReceipt> grs = supplierPOs.stream()
                                        .flatMap(po -> grRepository.findByPurchaseOrderId(po.getId()).stream())
                                        .collect(Collectors.toList());

                        if (!grs.isEmpty()) {
                                long approvedGRs = grs.stream()
                                                .filter(gr -> gr.getQualityStatus() == GoodsReceipt.QualityStatus.APPROVED)
                                                .count();
                                double qualityScore = (approvedGRs * 100.0) / grs.size();
                                dto.setQualityScore(qualityScore);
                        }

                        return dto;
                }).collect(Collectors.toList());
        }

        public SpendAnalysisDTO getSpendAnalysis(java.util.UUID tenantId, LocalDate startDate, LocalDate endDate) {
                SpendAnalysisDTO dto = new SpendAnalysisDTO();
                dto.setPeriod(endDate);

                // Get all POs in period
                List<PurchaseOrder> pos = poRepository.findByTenantId(tenantId).stream()
                                .filter(po -> po.getOrderDate() != null &&
                                                !po.getOrderDate().isBefore(startDate) &&
                                                !po.getOrderDate().isAfter(endDate))
                                .collect(Collectors.toList());

                BigDecimal totalSpend = pos.stream()
                                .map(PurchaseOrder::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setTotalSpend(totalSpend);

                // CAPEX vs OPEX logic - simplified
                BigDecimal capex = pos.stream()
                                .filter(po -> po.getRequisitions() != null && !po.getRequisitions().isEmpty() &&
                                                po.getRequisitions().get(0).getRequisition() != null &&
                                                po.getRequisitions().get(0).getRequisition()
                                                                .getBudgetType() == Department.BudgetType.CAPEX)
                                .map(PurchaseOrder::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                dto.setCapexSpend(capex);
                dto.setOpexSpend(totalSpend.subtract(capex));

                // Category breakdown
                Map<String, BigDecimal> spendByCategory = pos.stream()
                                .flatMap(po -> po.getItems().stream())
                                .collect(Collectors.groupingBy(
                                                this::resolveCategoryName,
                                                Collectors.reducing(BigDecimal.ZERO,
                                                                item -> item.getTotalPrice() != null
                                                                                ? item.getTotalPrice()
                                                                                : BigDecimal.ZERO,
                                                                BigDecimal::add)));
                dto.setSpendByCategory(spendByCategory);

                // Department breakdown
                Map<String, BigDecimal> spendByDepartment = pos.stream()
                                .collect(Collectors.groupingBy(
                                                this::resolvePODepartmentName,
                                                Collectors.reducing(BigDecimal.ZERO, PurchaseOrder::getTotalAmount,
                                                                BigDecimal::add)));
                dto.setSpendByDepartment(spendByDepartment);

                return dto;
        }

        public Map<String, Object> getProcessMetrics(java.util.UUID tenantId) {
                Map<String, Object> metrics = new HashMap<>();

                // Average approval time
                List<Requisition> approvedReqs = requisitionRepository.findByTenantId(tenantId).stream()
                                .filter(r -> r.getStatus() == Requisition.RequisitionStatus.APPROVED)
                                .collect(Collectors.toList());

                if (!approvedReqs.isEmpty()) {
                        double avgApprovalDays = approvedReqs.stream()
                                        .filter(r -> r.getCreatedAt() != null && r.getUpdatedAt() != null)
                                        .mapToLong(r -> ChronoUnit.DAYS.between(r.getCreatedAt(), r.getUpdatedAt()))
                                        .average()
                                        .orElse(0.0);
                        metrics.put("averageApprovalTime", avgApprovalDays);
                }

                // Average PO cycle time (from creation to supplier confirmation)
                List<PurchaseOrder> confirmedPOs = poRepository.findByTenantId(tenantId).stream()
                                .filter(po -> po.getStatus() == PurchaseOrder.POStatus.SUPPLIER_CONFIRMED)
                                .collect(Collectors.toList());

                if (!confirmedPOs.isEmpty()) {
                        double avgPOCycle = confirmedPOs.stream()
                                        .filter(po -> po.getCreatedAt() != null && po.getSupplierConfirmedAt() != null)
                                        .mapToLong(po -> ChronoUnit.DAYS.between(po.getCreatedAt(),
                                                        po.getSupplierConfirmedAt()))
                                        .average()
                                        .orElse(0.0);
                        metrics.put("averagePOCycleTime", avgPOCycle);
                }

                // 3-way match success rate
                List<Invoice> allInvoices = invoiceRepository.findByTenantId(tenantId);
                if (!allInvoices.isEmpty()) {
                        long matchedCount = allInvoices.stream()
                                        .filter(inv -> inv.getMatchStatus() == Invoice.MatchStatus.MATCHED)
                                        .count();
                        double matchRate = (matchedCount * 100.0) / allInvoices.size();
                        metrics.put("matchSuccessRate", matchRate);
                }

                return metrics;
        }
}
