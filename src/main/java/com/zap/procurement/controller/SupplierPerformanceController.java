package com.zap.procurement.controller;

import com.zap.procurement.domain.SupplierEvaluation;
import com.zap.procurement.service.PerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierPerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAnyAuthority('VIEW_SUPPLIERS', 'ADMIN_ACCESS')")
    public SupplierEvaluation getLatestPerformance(@PathVariable UUID id) {
        return performanceService.getLatestEvaluation(id);
    }

    @GetMapping("/{id}/performance/history")
    @PreAuthorize("hasAnyAuthority('VIEW_SUPPLIERS', 'ADMIN_ACCESS')")
    public List<SupplierEvaluation> getPerformanceHistory(@PathVariable UUID id) {
        return performanceService.getEvaluations(id);
    }

    @PostMapping("/{id}/performance/recalculate")
    @PreAuthorize("hasAnyAuthority('MANAGE_SUPPLIERS', 'ADMIN_ACCESS')")
    public SupplierEvaluation recalculatePerformance(@PathVariable UUID id) {
        return performanceService.calculateAndSavePerformance(id);
    }
}
