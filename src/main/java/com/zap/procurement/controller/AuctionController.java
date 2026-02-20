package com.zap.procurement.controller;

import com.zap.procurement.domain.AuctionBid;
import com.zap.procurement.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auctions")
@CrossOrigin(origins = "*")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @GetMapping("/{rfqId}/bids")
    public ResponseEntity<List<AuctionBid>> getBids(@PathVariable UUID rfqId) {
        return ResponseEntity.ok(auctionService.getBidsForAuction(rfqId));
    }

    @GetMapping("/{rfqId}/leading")
    public ResponseEntity<AuctionBid> getLeadingBid(@PathVariable UUID rfqId) {
        return ResponseEntity.ok(auctionService.getLeadingBid(rfqId));
    }

    @PostMapping("/{rfqId}/bid")
    public ResponseEntity<AuctionBid> placeBid(
            @PathVariable UUID rfqId,
            @RequestParam UUID supplierId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(auctionService.placeBid(rfqId, supplierId, amount));
    }

    @PostMapping("/{rfqId}/start")
    public ResponseEntity<Void> startAuction(
            @PathVariable UUID rfqId,
            @RequestParam(required = false) Integer durationMinutes) {
        auctionService.startAuction(rfqId, durationMinutes);
        return ResponseEntity.ok().build();
    }
}
