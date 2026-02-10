package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.dto.AdminDashboardDTO;
import com.zap.procurement.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        UUID tenantId = TenantContext.getCurrentTenant();
        AdminDashboardDTO stats = adminService.getDashboardStats(tenantId);
        return ResponseEntity.ok(stats);
    }
}
