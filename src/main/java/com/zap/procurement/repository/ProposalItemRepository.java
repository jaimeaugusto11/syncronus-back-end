package com.zap.procurement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.zap.procurement.domain.ProposalItem;
import java.util.UUID;

@Repository
public interface ProposalItemRepository extends JpaRepository<ProposalItem, UUID> {
}
