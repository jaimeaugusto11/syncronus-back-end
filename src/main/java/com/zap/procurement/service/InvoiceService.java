package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private GoodsReceiptRepository goodsReceiptRepository;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        invoice.setTenantId(tenantId);
        invoice.setCode(generateInvoiceCode(tenantId));
        invoice.setStatus(Invoice.InvoiceStatus.RECEIVED);
        invoice.setMatchStatus(Invoice.MatchStatus.PENDING);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice perform3WayMatch(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        PurchaseOrder po = invoice.getPurchaseOrder();
        List<GoodsReceipt> goodsReceipts = goodsReceiptRepository.findByPurchaseOrderId(po.getId());

        if (goodsReceipts.isEmpty()) {
            invoice.setMatchStatus(Invoice.MatchStatus.PENDING);
            invoice.setMatchNotes("No goods receipt found for this PO");
            invoice.setStatus(Invoice.InvoiceStatus.UNDER_REVIEW);
            return invoiceRepository.save(invoice);
        }

        // Perform 3-way match
        MatchResult result = performMatch(invoice, po, goodsReceipts);

        invoice.setMatchStatus(result.matchStatus);
        invoice.setMatchNotes(result.notes);

        if (result.matchStatus == Invoice.MatchStatus.MATCHED) {
            invoice.setStatus(Invoice.InvoiceStatus.MATCHED);
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.MISMATCH);
        }

        return invoiceRepository.save(invoice);
    }

    private MatchResult performMatch(Invoice invoice, PurchaseOrder po, List<GoodsReceipt> goodsReceipts) {
        MatchResult result = new MatchResult();
        List<String> issues = new ArrayList<>();

        // 1. Check total amount
        BigDecimal tolerance = new BigDecimal("0.01"); // 1 cent tolerance
        BigDecimal priceDiff = invoice.getTotalAmount().subtract(po.getTotalAmount()).abs();

        if (priceDiff.compareTo(tolerance) > 0) {
            issues.add("Price mismatch: Invoice=" + invoice.getTotalAmount() + ", PO=" + po.getTotalAmount());
            result.matchStatus = Invoice.MatchStatus.PRICE_MISMATCH;
        }

        // 2. Check quantities (compare invoice items with GR items)
        for (InvoiceItem invItem : invoice.getItems()) {
            POItem poItem = invItem.getPoItem();
            if (poItem == null) {
                issues.add("Invoice item '" + invItem.getDescription() + "' not found in PO");
                result.matchStatus = Invoice.MatchStatus.ITEM_MISMATCH;
                continue;
            }

            // Find total received quantity from all GRs
            int totalReceived = goodsReceipts.stream()
                    .flatMap(gr -> gr.getItems().stream())
                    .filter(grItem -> grItem.getPoItem().getId().equals(poItem.getId()))
                    .mapToInt(GRItem::getQuantityReceived)
                    .sum();

            if (invItem.getQuantity() > totalReceived) {
                issues.add("Quantity mismatch for '" + invItem.getDescription() +
                        "': Invoice=" + invItem.getQuantity() + ", Received=" + totalReceived);
                if (result.matchStatus == null || result.matchStatus == Invoice.MatchStatus.PENDING) {
                    result.matchStatus = Invoice.MatchStatus.QUANTITY_MISMATCH;
                } else {
                    result.matchStatus = Invoice.MatchStatus.MULTIPLE_ISSUES;
                }
            }

            // Check unit price
            BigDecimal unitPriceDiff = invItem.getUnitPrice().subtract(poItem.getUnitPrice()).abs();
            if (unitPriceDiff.compareTo(tolerance) > 0) {
                issues.add("Unit price mismatch for '" + invItem.getDescription() +
                        "': Invoice=" + invItem.getUnitPrice() + ", PO=" + poItem.getUnitPrice());
                if (result.matchStatus == null || result.matchStatus == Invoice.MatchStatus.PENDING) {
                    result.matchStatus = Invoice.MatchStatus.PRICE_MISMATCH;
                } else {
                    result.matchStatus = Invoice.MatchStatus.MULTIPLE_ISSUES;
                }
            }
        }

        // 3. If no issues found, mark as matched
        if (issues.isEmpty()) {
            result.matchStatus = Invoice.MatchStatus.MATCHED;
            result.notes = "3-way match successful: PO + GR + Invoice all match";
        } else {
            result.notes = String.join("; ", issues);
        }

        return result;
    }

    @Transactional
    public Invoice approveInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
        invoice.setApprovedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice rejectInvoice(UUID invoiceId, String reason) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setStatus(Invoice.InvoiceStatus.REJECTED);
        invoice.setMatchNotes((invoice.getMatchNotes() != null ? invoice.getMatchNotes() + "; " : "") +
                "Rejected: " + reason);

        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceCode(UUID tenantId) {
        long count = invoiceRepository.findByTenantId(tenantId).size();
        return String.format("INV-%d-%04d", java.time.Year.now().getValue(), count + 1);
    }

    public List<Invoice> getInvoicesByTenant(UUID tenantId) {
        return invoiceRepository.findByTenantId(tenantId);
    }

    private static class MatchResult {
        Invoice.MatchStatus matchStatus = Invoice.MatchStatus.PENDING;
        String notes = "";
    }
}
