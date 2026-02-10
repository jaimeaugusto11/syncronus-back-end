package com.zap.procurement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_suppliers")
@Data
@EqualsAndHashCode(callSuper = true)
public class RFQSupplier extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status = InvitationStatus.INVITED;

    public enum InvitationStatus {
        INVITED,
        VIEWED,
        PROPOSAL_SUBMITTED,
        DECLINED,
        REMOVED
    }
}
