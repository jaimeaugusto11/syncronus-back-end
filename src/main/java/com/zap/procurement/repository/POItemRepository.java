package com.zap.procurement.repository;

import com.zap.procurement.domain.POItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface POItemRepository extends JpaRepository<POItem, UUID> {
    Optional<POItem> findBySourceRfqItemId(UUID sourceRfqItemId);
}
