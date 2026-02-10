package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.POApproval;
import com.zap.procurement.domain.PurchaseOrder;
import com.zap.procurement.repository.PurchaseOrderRepository;
import com.zap.procurement.repository.UserRepository;
import com.zap.procurement.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService poService;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPOs() {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<PurchaseOrder> pos = poService.getPOsByTenant(tenantId);
        return ResponseEntity.ok(pos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPO(@PathVariable UUID id) {
        return poRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPO(
            @RequestBody PurchaseOrder po,
            @RequestParam UUID createdByUserId) {
        var user = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        PurchaseOrder created = poService.createPO(po, user);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrder> updatePO(
            @PathVariable UUID id,
            @RequestBody PurchaseOrder po) {
        PurchaseOrder updated = poService.updatePO(id, po);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePO(@PathVariable UUID id) {
        poService.deletePO(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/from-proposal")
    public ResponseEntity<PurchaseOrder> createFromProposal(
            @RequestParam UUID proposalId,
            @RequestParam UUID createdByUserId) {
        var user = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        PurchaseOrder po = poService.createPOFromProposal(proposalId, user);
        return ResponseEntity.ok(po);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PurchaseOrder> submitForApproval(@PathVariable UUID id) {
        PurchaseOrder po = poService.submitForApproval(id);
        return ResponseEntity.ok(po);
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<List<POApproval>> getPendingApprovals(@RequestParam UUID userId) {
        List<POApproval> approvals = poService.getPendingApprovals(userId);
        return ResponseEntity.ok(approvals);
    }

    @PostMapping("/approvals/{id}/action")
    public ResponseEntity<?> processApproval(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String action = request.get("action");
        String comments = request.get("comments");

        poService.processApproval(id, action, comments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/send-to-supplier")
    public ResponseEntity<PurchaseOrder> sendToSupplier(@PathVariable UUID id) {
        PurchaseOrder po = poService.sendToSupplier(id);
        return ResponseEntity.ok(po);
    }

    @PostMapping("/{id}/supplier-confirm")
    public ResponseEntity<PurchaseOrder> confirmBySupplier(@PathVariable UUID id) {
        PurchaseOrder po = poService.confirmBySupplier(id);
        return ResponseEntity.ok(po);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrder> cancelPO(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        PurchaseOrder po = poService.cancelPO(id, reason);
        return ResponseEntity.ok(po);
    }

    @PostMapping("/{id}/requisitions")
    public ResponseEntity<?> linkRequisitions(
            @PathVariable UUID id,
            @RequestBody List<com.zap.procurement.dto.RequisitionLinkDTO> links) {
        poService.linkRequisitionsToPO(id, links);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/requisitions/{requisitionId}")
    public ResponseEntity<?> unlinkRequisition(
            @PathVariable UUID id,
            @PathVariable UUID requisitionId) {
        poService.unlinkRequisition(id, requisitionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/requisitions")
    public ResponseEntity<List<com.zap.procurement.dto.RequisitionSummaryDTO>> getLinkedRequisitions(
            @PathVariable UUID id) {
        return ResponseEntity.ok(poService.getRequisitionsForPO(id));
    }
}
