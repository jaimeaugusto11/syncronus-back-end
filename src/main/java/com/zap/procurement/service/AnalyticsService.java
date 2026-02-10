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
import java.time.temporal.ChronoUnit;
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

                List<PurchaseOrder> activePOs = poRepository.findByTenantId(tenantId).stream()
                                .filter(po -> po.getStatus() != PurchaseOrder.POStatus.COMPLETED &&
                                                po.getStatus() != PurchaseOrder.POStatus.CANCELLED)
                                .collect(Collectors.toList());
                dashboard.setActivePOs(activePOs.size());

                // Financial metrics
                BigDecimal totalSpend = poRepository.findByTenantId(tenantId).stream()
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

                // Spend by category (simplified - would need category field in requisition)
                Map<String, BigDecimal> spendByCategory = new HashMap<>();
                spendByCategory.put("IT", new BigDecimal("50000"));
                spendByCategory.put("Facilities", new BigDecimal("30000"));
                spendByCategory.put("Services", new BigDecimal("20000"));
                dashboard.setSpendByCategory(spendByCategory);

                // Recent activity
                LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                long requisitionsThisMonth = allRequisitions.stream()
                                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(monthStart))
                                .count();
                dashboard.setRequisitionsThisMonth((int) requisitionsThisMonth);

                return dashboard;
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

                // CAPEX vs OPEX (would need budget type in PO or link to requisition)
                dto.setCapexSpend(totalSpend.multiply(new BigDecimal("0.4"))); // 40% example
                dto.setOpexSpend(totalSpend.multiply(new BigDecimal("0.6"))); // 60% example

                // Category breakdown (simplified)
                dto.setItSpend(totalSpend.multiply(new BigDecimal("0.3")));
                dto.setFacilitiesSpend(totalSpend.multiply(new BigDecimal("0.25")));
                dto.setServicesSpend(totalSpend.multiply(new BigDecimal("0.25")));
                dto.setMaterialsSpend(totalSpend.multiply(new BigDecimal("0.15")));
                dto.setOtherSpend(totalSpend.multiply(new BigDecimal("0.05")));

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
