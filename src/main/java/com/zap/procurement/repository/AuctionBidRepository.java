package com.zap.procurement.repository;

import com.zap.procurement.domain.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, UUID> {
    List<AuctionBid> findByRfqIdOrderByBidAmountAscBidTimeDesc(UUID rfqId);

    List<AuctionBid> findByRfqIdAndSupplierId(UUID rfqId, UUID supplierId);

    // Find leading bid for an RFQ
    AuctionBid findFirstByRfqIdOrderByBidAmountAscBidTimeDesc(UUID rfqId);
}
