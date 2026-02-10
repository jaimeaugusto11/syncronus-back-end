package com.zap.procurement.repository;

import com.zap.procurement.domain.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {
}
