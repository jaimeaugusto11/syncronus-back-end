package com.zap.procurement.repository;

import com.zap.procurement.domain.RFQItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RFQItemRepository extends JpaRepository<RFQItem, UUID> {
}
