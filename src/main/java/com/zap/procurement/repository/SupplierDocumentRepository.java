package com.zap.procurement.repository;

import com.zap.procurement.domain.SupplierDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, UUID> {
    List<SupplierDocument> findBySupplierId(UUID supplierId);
}
