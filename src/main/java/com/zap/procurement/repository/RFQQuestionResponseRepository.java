package com.zap.procurement.repository;

import com.zap.procurement.domain.RFQQuestionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RFQQuestionResponseRepository extends JpaRepository<RFQQuestionResponse, UUID> {
    List<RFQQuestionResponse> findByProposalId(UUID proposalId);
}
