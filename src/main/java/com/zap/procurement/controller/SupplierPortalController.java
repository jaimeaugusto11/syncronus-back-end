package com.zap.procurement.controller;

import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import com.zap.procurement.service.RFQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.zap.procurement.service.PurchaseOrderService;

@RestController
@RequestMapping("/portal")
@CrossOrigin(origins = "*")
public class SupplierPortalController {

    @Autowired
    private RFQService rfqService;

    @Autowired
    private RFQRepository rfqRepository;

    @Autowired
    private SupplierUserRepository supplierUserRepository;

    @Autowired
    private SupplierProposalRepository proposalRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private SupplierCategoryRepository supplierCategoryRepository;

    @Autowired
    private com.zap.procurement.service.ProposalNegotiationService negotiationService;

    @GetMapping("/rfqs")
    public ResponseEntity<List<RFQ>> getRFQs(@RequestHeader("X-Supplier-User-ID") UUID supplierUserId) {
        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        // Only return RFQs where this supplier was invited and not removed
        List<RFQSupplier> invited = rfqService.getInvitedRFQs(user.getSupplier().getId());
        // Get supplier categories
        List<UUID> supplierCategoryIds = supplierCategoryRepository.findBySupplierId(user.getSupplier().getId())
                .stream()
                .map(sc -> sc.getCategory().getId())
                .toList();

        List<RFQ> rfqs = invited.stream()
                .filter(rfqSupplier -> rfqSupplier.getStatus() != RFQSupplier.InvitationStatus.REMOVED)
                .map(RFQSupplier::getRfq)
                .filter(rfq -> rfq.getStatus() == RFQ.RFQStatus.OPEN ||
                        rfq.getStatus() == RFQ.RFQStatus.PUBLISHED)
                .filter(rfq -> {
                    // Only show RFQ if it has at least one matching item or if it's general/manual
                    if (rfq.getItems() == null || rfq.getItems().isEmpty())
                        return true;
                    return rfq.getItems().stream().anyMatch(item -> {
                        if (item.getRequisitionItem() != null && item.getRequisitionItem().getCategory() != null) {
                            return supplierCategoryIds.contains(item.getRequisitionItem().getCategory().getId());
                        }
                        return true; // General item
                    });
                })
                .toList();

        return ResponseEntity.ok(rfqs);
    }

    @GetMapping("/proposals")
    public ResponseEntity<List<SupplierProposal>> getProposals(
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId) {
        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        return ResponseEntity.ok(proposalRepository.findBySupplierId(user.getSupplier().getId()));
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<List<PurchaseOrder>> getPurchaseOrders(
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId) {
        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        return ResponseEntity.ok(purchaseOrderRepository.findBySupplierId(user.getSupplier().getId()));
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> getInvoices(@RequestHeader("X-Supplier-User-ID") UUID supplierUserId) {
        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        return ResponseEntity.ok(invoiceRepository.findBySupplierId(user.getSupplier().getId()));
    }

    @PostMapping("/invoices")
    public ResponseEntity<Invoice> submitInvoice(
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId,
            @RequestBody Invoice invoice) {

        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        // Always force the correct supplier from the authenticated user context
        invoice.setSupplier(user.getSupplier());

        // In a real scenario, we would validate that the supplier matches the
        // authenticated user
        // We'll use the invoiceRepository directly or via a service if available
        invoice.setStatus(Invoice.InvoiceStatus.RECEIVED);
        invoice.setReceivedAt(LocalDateTime.now());

        // Inherit tenant from PO
        if (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findById(invoice.getPurchaseOrder().getId())
                    .orElse(null);
            if (po != null) {
                invoice.setPurchaseOrder(po);
                if (invoice.getTenantId() == null) {
                    invoice.setTenantId(po.getTenantId());
                }
            }
        }

        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoice(invoice);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/proposals")
    public ResponseEntity<SupplierProposal> submitProposal(
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId,
            @RequestBody SupplierProposal proposal) {

        // Defensive: Set supplier from the authenticated user if missing or wrong
        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        // Always force the correct supplier from the authenticated user context
        proposal.setSupplier(user.getSupplier());

        SupplierProposal saved = rfqService.submitProposal(proposal);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/rfqs/{id}")
    public ResponseEntity<RFQ> getRFQDetails(
            @PathVariable UUID id,
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId) {

        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        RFQ rfq = rfqRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        if (rfq.getStatus() == RFQ.RFQStatus.DRAFT) {
            throw new RuntimeException("RFQ is not yet published");
        }

        // Get supplier categories
        List<UUID> supplierCategoryIds = supplierCategoryRepository.findBySupplierId(user.getSupplier().getId())
                .stream()
                .map(sc -> sc.getCategory().getId())
                .toList();

        // Filter items:
        // 1. If item has a category, supplier must have it.
        // 2. If item has NO category, it's visible to everyone invited.
        if (rfq.getItems() != null) {
            List<RFQItem> filteredItems = rfq.getItems().stream()
                    .filter(item -> {
                        if (item.getRequisitionItem() != null && item.getRequisitionItem().getCategory() != null) {
                            return supplierCategoryIds.contains(item.getRequisitionItem().getCategory().getId());
                        }
                        return true; // General/No category items
                    })
                    .toList();
            rfq.setItems(filteredItems);
        }

        return ResponseEntity.ok(rfq);
    }

    @PostMapping("/purchase-orders/{id}/confirm")
    public ResponseEntity<PurchaseOrder> confirmPurchaseOrder(@PathVariable UUID id) {
        PurchaseOrder confirmed = purchaseOrderService.confirmBySupplier(id);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("/proformas")
    public ResponseEntity<Invoice> submitProforma(
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId,
            @RequestBody Invoice invoice) {

        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        // Always force the correct supplier from the authenticated user context
        invoice.setSupplier(user.getSupplier());

        invoice.setProforma(true);
        invoice.setStatus(Invoice.InvoiceStatus.RECEIVED);
        invoice.setReceivedAt(LocalDateTime.now());

        // Inherit tenant from PO
        if (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findById(invoice.getPurchaseOrder().getId())
                    .orElse(null);
            if (po != null) {
                invoice.setPurchaseOrder(po);
                if (invoice.getTenantId() == null) {
                    invoice.setTenantId(po.getTenantId());
                }
            }
        }

        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoice(invoice);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        return ResponseEntity.ok(saved);
    }

    // Endpoint to manage supplier users (For Supplier Admin)
    @PostMapping("/users")
    public ResponseEntity<SupplierUser> createSupplierUser(@RequestBody SupplierUser user) {
        // Only Supplier Admin can create users - logic to be added
        return ResponseEntity.ok(user);
    }

    // ── Profile endpoints ──────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(@RequestHeader("X-Supplier-User-ID") UUID supplierUserId) {
        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        java.util.Map<String, Object> profile = new java.util.LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("mustChangePassword", user.isMustChangePassword());

        // Supplier company info
        Supplier supplier = user.getSupplier();
        if (supplier != null) {
            java.util.Map<String, Object> company = new java.util.LinkedHashMap<>();
            company.put("id", supplier.getId());
            company.put("name", supplier.getName());
            company.put("code", supplier.getCode());
            company.put("email", supplier.getEmail());
            company.put("phone", supplier.getPhone());
            company.put("address", supplier.getAddress());
            company.put("taxId", supplier.getNif());
            company.put("status", supplier.getStatus());
            profile.put("company", company);
        }

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId,
            @RequestBody java.util.Map<String, String> updates) {

        SupplierUser user = supplierUserRepository.findById(supplierUserId)
                .orElseThrow(() -> new RuntimeException("Supplier User not found"));

        if (updates.containsKey("name") && updates.get("name") != null && !updates.get("name").isBlank()) {
            user.setName(updates.get("name"));
        }

        supplierUserRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of("message", "Perfil actualizado com sucesso"));
    }

    // Alias: /portal/invoices/proforma → same as /portal/proformas
    // ── Negotiation endpoints ────────────────────────────────────────────────

    @GetMapping("/proposals/{proposalId}/messages")
    public ResponseEntity<List<ProposalNegotiationMessage>> getProposalMessages(@PathVariable UUID proposalId) {
        return ResponseEntity.ok(negotiationService.getMessages(proposalId));
    }

    @PostMapping("/proposals/{proposalId}/messages")
    public ResponseEntity<ProposalNegotiationMessage> sendProposalMessage(
            @PathVariable UUID proposalId,
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId,
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        return ResponseEntity.ok(negotiationService.sendMessage(proposalId, content, supplierUserId, true));
    }

    @GetMapping("/proposals/{proposalId}/price-history")
    public ResponseEntity<List<ProposalPriceHistory>> getProposalPriceHistory(@PathVariable UUID proposalId) {
        return ResponseEntity.ok(negotiationService.getPriceHistory(proposalId));
    }

    @GetMapping("/proposals/{proposalId}")
    public ResponseEntity<SupplierProposal> getProposalDetails(@PathVariable @NonNull UUID proposalId) {
        return ResponseEntity.ok(proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada")));
    }

    @PutMapping("/proposals/{proposalId}/price")
    public ResponseEntity<SupplierProposal> updateProposalPrice(
            @PathVariable UUID proposalId,
            @RequestHeader("X-Supplier-User-ID") UUID supplierUserId,
            @RequestBody Map<String, Object> request) {

        BigDecimal newPrice = new BigDecimal(request.get("price").toString());
        String reason = (String) request.get("reason");

        return ResponseEntity
                .ok(negotiationService.updateProposalPrice(proposalId, newPrice, supplierUserId, reason, true));
    }
}
