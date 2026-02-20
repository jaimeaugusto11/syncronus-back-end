package com.zap.procurement.service;

import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PerformanceService {

    @Autowired
    private SupplierEvaluationRepository evaluationRepository;

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private GoodsReceiptRepository grRepository;

    @Autowired
    private com.zap.procurement.repository.SupplierRepository supplierRepository;

    @Transactional
    public SupplierEvaluation calculateAndSavePerformance(UUID supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado"));

        List<PurchaseOrder> pos = poRepository.findBySupplierId(supplierId);

        BigDecimal deliveryScore = calculateDeliveryScore(pos);
        BigDecimal qualityScore = calculateQualityScore(pos);
        BigDecimal priceScore = calculatePriceScore(pos); // Simplified

        // Weighting: Delivery 40%, Quality 40%, Price 20%
        BigDecimal overallScore = deliveryScore.multiply(new BigDecimal("0.4"))
                .add(qualityScore.multiply(new BigDecimal("0.4")))
                .add(priceScore.multiply(new BigDecimal("0.2")))
                .setScale(2, RoundingMode.HALF_UP);

        SupplierEvaluation eval = new SupplierEvaluation();
        eval.setSupplier(supplier);
        eval.setEvaluationDate(LocalDateTime.now());
        eval.setDeliveryScore(deliveryScore);
        eval.setQualityScore(qualityScore);
        eval.setPriceScore(priceScore);
        eval.setOverallScore(overallScore);
        eval.setNotes("Avaliação automática baseada no histórico de transações.");
        eval.setTenantId(supplier.getTenantId());

        return evaluationRepository.save(eval);
    }

    private BigDecimal calculateDeliveryScore(List<PurchaseOrder> pos) {
        if (pos.isEmpty())
            return new BigDecimal("100");

        double totalScore = 0;
        int count = 0;

        for (PurchaseOrder po : pos) {
            if (po.getStatus() == PurchaseOrder.POStatus.COMPLETED
                    || po.getStatus() == PurchaseOrder.POStatus.PARTIALLY_RECEIVED) {
                List<GoodsReceipt> receipts = grRepository.findByPurchaseOrderId(po.getId());
                if (receipts.isEmpty())
                    continue;

                // Find the latest receipt date
                LocalDateTime latestReceipt = receipts.stream()
                        .map(r -> r.getReceiptDate().atStartOfDay())
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

                long delayDays = 0;
                if (po.getExpectedDeliveryDate() != null) {
                    delayDays = ChronoUnit.DAYS.between(po.getExpectedDeliveryDate().atStartOfDay(), latestReceipt);
                }

                double poScore = 100;
                if (delayDays > 0) {
                    poScore = Math.max(0, 100 - (delayDays * 5)); // -5 points per day delay
                }
                totalScore += poScore;
                count++;
            }
        }

        if (count == 0)
            return new BigDecimal("100");
        return new BigDecimal(totalScore / count).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateQualityScore(List<PurchaseOrder> pos) {
        if (pos.isEmpty())
            return new BigDecimal("100");

        double totalScore = 0;
        int count = 0;

        for (PurchaseOrder po : pos) {
            List<GoodsReceipt> receipts = grRepository.findByPurchaseOrderId(po.getId());
            for (GoodsReceipt gr : receipts) {
                if (gr.getQualityStatus() == GoodsReceipt.QualityStatus.APPROVED) {
                    totalScore += 100;
                } else if (gr.getQualityStatus() == GoodsReceipt.QualityStatus.CONDITIONAL_APPROVAL) {
                    totalScore += 70;
                } else if (gr.getQualityStatus() == GoodsReceipt.QualityStatus.REJECTED) {
                    totalScore += 0;
                } else {
                    continue; // Skip pending
                }
                count++;
            }
        }

        if (count == 0)
            return new BigDecimal("100");
        return new BigDecimal(totalScore / count).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePriceScore(List<PurchaseOrder> pos) {
        // Price score logic: comparison with estimated price in RFQ
        // For simplicity, if we don't have detailed RFQ estimates history, we'll return
        // 90
        return new BigDecimal("90.00");
    }

    public List<SupplierEvaluation> getEvaluations(UUID supplierId) {
        return evaluationRepository.findBySupplierIdOrderByEvaluationDateDesc(supplierId);
    }

    public SupplierEvaluation getLatestEvaluation(UUID supplierId) {
        List<SupplierEvaluation> evals = evaluationRepository.findBySupplierIdOrderByEvaluationDateDesc(supplierId);
        return evals.isEmpty() ? null : evals.get(0);
    }
}
