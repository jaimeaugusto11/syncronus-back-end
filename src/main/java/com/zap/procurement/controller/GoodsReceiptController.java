package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.GoodsReceipt;
import com.zap.procurement.repository.GoodsReceiptRepository;
import com.zap.procurement.service.GoodsReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goods-receipts")
@CrossOrigin(origins = "*")
public class GoodsReceiptController {

    @Autowired
    private GoodsReceiptService grService;

    @Autowired
    private GoodsReceiptRepository grRepository;

    @GetMapping
    public ResponseEntity<List<GoodsReceipt>> getAllGRs() {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        List<GoodsReceipt> grs = grService.getGRsByTenant(tenantId);
        return ResponseEntity.ok(grs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceipt> getGR(@PathVariable java.util.UUID id) {
        return grRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-po/{poId}")
    public ResponseEntity<List<GoodsReceipt>> getGRsByPO(@PathVariable java.util.UUID poId) {
        List<GoodsReceipt> grs = grService.getGRsByPO(poId);
        return ResponseEntity.ok(grs);
    }

    @PostMapping
    public ResponseEntity<GoodsReceipt> createGR(@RequestBody GoodsReceipt gr) {
        GoodsReceipt created = grService.createGoodsReceipt(gr);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/approve-quality")
    public ResponseEntity<GoodsReceipt> approveQuality(@PathVariable java.util.UUID id) {
        GoodsReceipt gr = grService.approveQuality(id);
        return ResponseEntity.ok(gr);
    }

    @PostMapping("/{id}/reject-quality")
    public ResponseEntity<GoodsReceipt> rejectQuality(
            @PathVariable java.util.UUID id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        GoodsReceipt gr = grService.rejectQuality(id, reason);
        return ResponseEntity.ok(gr);
    }
}
