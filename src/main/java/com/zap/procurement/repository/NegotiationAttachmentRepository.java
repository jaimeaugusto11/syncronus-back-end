package com.zap.procurement.repository;

import com.zap.procurement.domain.NegotiationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface NegotiationAttachmentRepository extends JpaRepository<NegotiationAttachment, UUID> {
    List<NegotiationAttachment> findByMessageId(UUID messageId);
}
