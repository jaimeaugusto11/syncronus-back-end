package com.zap.procurement.repository;

import com.zap.procurement.domain.RFQQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RFQQuestionRepository extends JpaRepository<RFQQuestion, UUID> {
    List<RFQQuestion> findByRfqId(UUID rfqId);
}
