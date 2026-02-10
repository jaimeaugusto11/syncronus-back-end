package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class GoodsReceiptService {

    @Autowired
    private GoodsReceiptRepository grRepository;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Transactional
    public GoodsReceipt createGoodsReceipt(GoodsReceipt gr) {
        java.util.UUID tenantId = TenantContext.getCurrentTenant();
        // Tenant tenant = tenantRepository.findById(tenantId)
        // .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // gr.setTenant(tenant);
        gr.setTenantId(tenantId);
        gr.setCode(generateGRCode(tenantId));
        gr.setReceiptDate(LocalDate.now());

        // Update PO item received quantities
        for (GRItem grItem : gr.getItems()) {
            POItem poItem = grItem.getPoItem();
            int currentReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : 0;
            poItem.setReceivedQuantity(currentReceived + grItem.getQuantityReceived());
        }

        // Update PO status based on receipt type
        PurchaseOrder po = gr.getPurchaseOrder();
        if (gr.getReceiptType() == GoodsReceipt.ReceiptType.FULL) {
            po.setStatus(PurchaseOrder.POStatus.COMPLETED);
        } else {
            po.setStatus(PurchaseOrder.POStatus.PARTIALLY_RECEIVED);
        }
        poRepository.save(po);

        return grRepository.save(gr);
    }

    @Transactional
    public GoodsReceipt approveQuality(java.util.UUID grId) {
        GoodsReceipt gr = grRepository.findById(grId)
                .orElseThrow(() -> new RuntimeException("Goods Receipt not found"));

        gr.setQualityStatus(GoodsReceipt.QualityStatus.APPROVED);
        return grRepository.save(gr);
    }

    @Transactional
    public GoodsReceipt rejectQuality(java.util.UUID grId, String reason) {
        GoodsReceipt gr = grRepository.findById(grId)
                .orElseThrow(() -> new RuntimeException("Goods Receipt not found"));

        gr.setQualityStatus(GoodsReceipt.QualityStatus.REJECTED);
        gr.setQualityNotes(reason);

        return grRepository.save(gr);
    }

    private String generateGRCode(java.util.UUID tenantId) {
        long count = grRepository.findByTenantId(tenantId).size();
        return String.format("GR-%d-%04d", java.time.Year.now().getValue(), count + 1);
    }

    public List<GoodsReceipt> getGRsByTenant(java.util.UUID tenantId) {
        return grRepository.findByTenantId(tenantId);
    }

    public List<GoodsReceipt> getGRsByPO(java.util.UUID poId) {
        return grRepository.findByPurchaseOrderId(poId);
    }
}
