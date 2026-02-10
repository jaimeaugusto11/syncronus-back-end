package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.dto.*;
import com.zap.procurement.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboard() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        DashboardDTO dashboard = analyticsService.getDashboard(tenantId);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/supplier-performance")
    public ResponseEntity<List<SupplierPerformanceDTO>> getSupplierPerformance() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<SupplierPerformanceDTO> performance = analyticsService.getSupplierPerformance(tenantId);
        return ResponseEntity.ok(performance);
    }

    @GetMapping("/spend-analysis")
    public ResponseEntity<SpendAnalysisDTO> getSpendAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        SpendAnalysisDTO analysis = analyticsService.getSpendAnalysis(tenantId, startDate, endDate);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/process-metrics")
    public ResponseEntity<Map<String, Object>> getProcessMetrics() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> metrics = analyticsService.getProcessMetrics(tenantId);
        return ResponseEntity.ok(metrics);
    }
}
