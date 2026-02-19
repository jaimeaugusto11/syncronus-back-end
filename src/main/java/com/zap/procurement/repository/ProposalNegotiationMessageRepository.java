package com.zap.procurement.repository;

import com.zap.procurement.domain.ProposalNegotiationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalNegotiationMessageRepository extends JpaRepository<ProposalNegotiationMessage, UUID> {
    List<ProposalNegotiationMessage> findByProposalIdOrderByCreatedAtAsc(UUID proposalId);
}
