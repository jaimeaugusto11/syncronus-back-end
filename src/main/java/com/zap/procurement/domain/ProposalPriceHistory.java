package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "proposal_price_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProposalPriceHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private SupplierProposal proposal;

    @Column(name = "old_price", nullable = false)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false)
    private BigDecimal newPrice;

    @Column(name = "currency")
    private String currency;

    @Column(name = "changed_by_id", nullable = false)
    private UUID changedById;

    @Column(name = "changed_by_name")
    private String changedByName;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
