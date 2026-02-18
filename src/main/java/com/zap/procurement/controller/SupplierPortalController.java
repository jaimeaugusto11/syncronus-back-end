package com.zap.procurement.controller;

import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import com.zap.procurement.service.RFQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
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
}
