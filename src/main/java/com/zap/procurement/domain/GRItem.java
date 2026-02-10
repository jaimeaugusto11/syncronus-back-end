package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "gr_items")
@Data
public class GRItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(updatable = false, nullable = false, length = 16)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id", nullable = false)
    private POItem poItem;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "meets_specifications")
    private Boolean meetsSpecifications = true;

    @Column(name = "quality_rating")
    private Integer qualityRating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String notes;
}
