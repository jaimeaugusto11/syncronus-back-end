package com.zap.procurement.service;

import com.zap.procurement.domain.AuctionBid;
import com.zap.procurement.domain.RFQ;
import com.zap.procurement.domain.Supplier;
import com.zap.procurement.repository.AuctionBidRepository;
import com.zap.procurement.repository.RFQRepository;
import com.zap.procurement.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuctionService {

    @Autowired
    private AuctionBidRepository auctionBidRepository;

    @Autowired
    private RFQRepository rfqRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    public List<AuctionBid> getBidsForAuction(UUID rfqId) {
        return auctionBidRepository.findByRfqIdOrderByBidAmountAscBidTimeDesc(rfqId);
    }

    public AuctionBid getLeadingBid(UUID rfqId) {
        return auctionBidRepository.findFirstByRfqIdOrderByBidAmountAscBidTimeDesc(rfqId);
    }

    @Transactional
    public AuctionBid placeBid(UUID rfqId, UUID supplierId, BigDecimal amount) {
        RFQ rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        if (rfq.getStatus() != RFQ.RFQStatus.AUCTION_IN_PROGRESS) {
            throw new RuntimeException("Auction is not in progress");
        }

        if (rfq.getAuctionEndTime() != null && LocalDateTime.now().isAfter(rfq.getAuctionEndTime())) {
            throw new RuntimeException("Auction has ended");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        AuctionBid currentLeader = getLeadingBid(rfqId);

        // Validation: Bid must be lower than current leader
        if (currentLeader != null) {
            BigDecimal minDecrease = rfq.getMinIncrement() != null ? rfq.getMinIncrement() : BigDecimal.ZERO;
            if (amount.compareTo(currentLeader.getBidAmount().subtract(minDecrease)) > 0) {
                throw new RuntimeException("Bid must be at least " + minDecrease + " lower than current leader");
            }
        } else if (rfq.getStartingPrice() != null && amount.compareTo(rfq.getStartingPrice()) > 0) {
            throw new RuntimeException("Initial bid must be lower than starting price");
        }

        // Reset previous lead
        if (currentLeader != null) {
            currentLeader.setIsLeading(false);
            auctionBidRepository.save(currentLeader);
        }

        AuctionBid bid = new AuctionBid();
        bid.setRfq(rfq);
        bid.setSupplier(supplier);
        bid.setBidAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bid.setIsLeading(true);
        bid.setTenantId(rfq.getTenantId());

        return auctionBidRepository.save(bid);
    }

    @Transactional
    public void startAuction(UUID rfqId, Integer durationMinutes) {
        RFQ rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ not found"));

        rfq.setStatus(RFQ.RFQStatus.AUCTION_IN_PROGRESS);
        rfq.setAuctionStartTime(LocalDateTime.now());
        if (durationMinutes != null) {
            rfq.setAuctionEndTime(LocalDateTime.now().plusMinutes(durationMinutes));
        }
        rfqRepository.save(rfq);
    }
}
