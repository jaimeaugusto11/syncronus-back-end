package com.zap.procurement.service;

import com.zap.procurement.domain.RFQ;
import com.zap.procurement.domain.RFQItem;
import com.zap.procurement.domain.Requisition;
import com.zap.procurement.domain.RequisitionItem;
import com.zap.procurement.domain.RequisitionItem.RequisitionItemStatus;
import com.zap.procurement.repository.RFQRepository;
import com.zap.procurement.repository.RequisitionItemRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataMigrationService implements ApplicationRunner {

    @Autowired
    private RequisitionItemRepository requisitionItemRepository;

    @Autowired
    private RFQRepository rfqRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("Applying Schema Fix for RFQ Status...");
        try {
            entityManager.createNativeQuery("ALTER TABLE rfqs MODIFY COLUMN status VARCHAR(50)").executeUpdate();
            System.out.println("Schema Fix Applied Successfully.");
        } catch (Exception e) {
            System.err.println("Failed to apply schema fix (might already be fixed): " + e.getMessage());
        }

        System.out.println("Starting Data Migration for RequisitionItem Status...");
        migrateRequisitionItemStatus();
        migrateRFQStatuses();
        migrateRFQCategories();
        System.out.println("Data Migration Completed.");
    }

    public void migrateData() {
        // Kept for backward compatibility or testing, but logic moved to run()
    }

    private void migrateRequisitionItemStatus() {
        List<RequisitionItem> items = requisitionItemRepository.findAll(); // Optimisation: findByStatus(null) if
                                                                           // possible, but status is not null in DB
                                                                           // usually due to default, need to check if
                                                                           // we can rely on Java null or if we need to
                                                                           // check if it's DRAFT when parent is
                                                                           // APPROVED

        for (RequisitionItem item : items) {
            // Logic to derive status from parent Requisition if item status is DRAFT
            // (default)
            // We assume if item is DRAFT but parent is further ahead, we should sync it.
            // OR if we just added the column, it might be DRAFT by default.

            Requisition parent = item.getRequisition();
            if (parent == null)
                continue;

            // Only update if item is DRAFT (meaning seemingly untouched/newly migrated)
            if (item.getStatus() == RequisitionItemStatus.DRAFT) {
                switch (parent.getStatus()) {
                    case PENDING_APPROVAL:
                    case DEPT_HEAD_APPROVAL:
                    case DEPT_DIRECTOR_APPROVAL:
                    case GENERAL_DIRECTOR_APPROVAL:
                        item.setStatus(RequisitionItemStatus.PENDING_APPROVAL);
                        break;
                    case APPROVED:
                        item.setStatus(RequisitionItemStatus.APPROVED);
                        break;
                    case IN_PROCUREMENT:
                        item.setStatus(RequisitionItemStatus.IN_SOURCING);
                        break;
                    case CONVERTED_TO_PO:
                        item.setStatus(RequisitionItemStatus.PO_CREATED); // Or COMPLETED
                        break;
                    case COMPLETED:
                        item.setStatus(RequisitionItemStatus.COMPLETED);
                        break;
                    case CANCELLED:
                    case REJECTED:
                        item.setStatus(RequisitionItemStatus.CANCELLED); // Or keep DRAFT? Better CANCELLED.
                        break;
                    default:
                        // Keep DRAFT
                        break;
                }
                requisitionItemRepository.save(item);
            }
        }
    }

    private void migrateRFQStatuses() {
        List<RFQ> rfqs = rfqRepository.findAll();
        for (RFQ rfq : rfqs) {
            if (rfq.getStatus() == RFQ.RFQStatus.READY_FOR_COMPARISON) {
                rfq.setStatus(RFQ.RFQStatus.READY_COMPARE);
                rfqRepository.save(rfq);
            }
        }
    }

    private void migrateRFQCategories() {
        List<RFQ> rfqs = rfqRepository.findAll();
        for (RFQ rfq : rfqs) {
            if (rfq.getCategory() == null) {
                // Try to infer category from items
                if (rfq.getItems() != null && !rfq.getItems().isEmpty()) {
                    RFQItem firstItem = rfq.getItems().get(0);
                    if (firstItem.getRequisitionItem() != null
                            && firstItem.getRequisitionItem().getCategory() != null) {
                        rfq.setCategory(firstItem.getRequisitionItem().getCategory());
                        rfqRepository.save(rfq);
                    }
                } else if (rfq.getRequisition() != null) {
                    // Fallback to Requisition items if RFQ items empty (rare)
                    if (rfq.getRequisition().getItems() != null && !rfq.getRequisition().getItems().isEmpty()) {
                        RequisitionItem firstItem = rfq.getRequisition().getItems().get(0);
                        if (firstItem.getCategory() != null) {
                            rfq.setCategory(firstItem.getCategory());
                            rfqRepository.save(rfq);
                        }
                    }
                }
            }
        }
    }
}
