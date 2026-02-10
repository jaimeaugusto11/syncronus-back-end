package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Invoice;
import com.zap.procurement.repository.InvoiceRepository;
import com.zap.procurement.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<Invoice> invoices = invoiceService.getInvoicesByTenant(tenantId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable java.util.UUID id) {
        return invoiceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        Invoice created = invoiceService.createInvoice(invoice);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/3-way-match")
    public ResponseEntity<Invoice> perform3WayMatch(@PathVariable java.util.UUID id) {
        Invoice invoice = invoiceService.perform3WayMatch(id);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Invoice> approveInvoice(@PathVariable java.util.UUID id) {
        Invoice invoice = invoiceService.approveInvoice(id);
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Invoice> rejectInvoice(
            @PathVariable java.util.UUID id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        Invoice invoice = invoiceService.rejectInvoice(id, reason);
        return ResponseEntity.ok(invoice);
    }
}
