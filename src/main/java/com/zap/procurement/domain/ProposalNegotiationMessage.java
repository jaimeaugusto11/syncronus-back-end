package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "proposal_negotiation_messages")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProposalNegotiationMessage extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private SupplierProposal proposal;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name")
    private String senderName;

    @com.fasterxml.jackson.annotation.JsonProperty("isFromSupplier")
    @Column(name = "is_from_supplier", nullable = false)
    private boolean isFromSupplier;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NegotiationAttachment> attachments = new ArrayList<>();

    public ProposalNegotiationMessage() {
        super();
    }
}
