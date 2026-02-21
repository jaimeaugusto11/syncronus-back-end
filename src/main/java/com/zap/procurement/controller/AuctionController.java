package com.zap.procurement.controller;

import com.zap.procurement.domain.AuctionBid;
import com.zap.procurement.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auctions")
@CrossOrigin(origins = "*")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    /**
     * Lista todos os lances de um leilão (acessível a gestores e fornecedores)
     */
    @GetMapping("/{rfqId}/bids")
    public ResponseEntity<List<AuctionBid>> getBids(@PathVariable UUID rfqId) {
        return ResponseEntity.ok(auctionService.getBidsForAuction(rfqId));
    }

    /**
     * Retorna o lance lider (menor valor) do leilão
     */
    @GetMapping("/{rfqId}/leading")
    public ResponseEntity<AuctionBid> getLeadingBid(@PathVariable UUID rfqId) {
        return ResponseEntity.ok(auctionService.getLeadingBid(rfqId));
    }

    /**
     * Fornecedor efectua um lance no leilão
     * Acesso: Fornecedores autenticados via portal de fornecedor
     */
    @PostMapping("/{rfqId}/bid")
    public ResponseEntity<?> placeBid(
            @PathVariable UUID rfqId,
            @RequestParam UUID supplierId,
            @RequestParam BigDecimal amount) {
        try {
            AuctionBid bid = auctionService.placeBid(rfqId, supplierId, amount);
            return ResponseEntity.ok(bid);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Gestor inicia o leilão reverso
     * Requer permissão: MANAGE_RFQS
     */
    @PostMapping("/{rfqId}/start")
    @PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<?> startAuction(
            @PathVariable UUID rfqId,
            @RequestParam(required = false) Integer durationMinutes) {
        try {
            auctionService.startAuction(rfqId, durationMinutes);
            return ResponseEntity.ok(Map.of("message", "Leilão iniciado com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Gestor encerra o leilão reverso manualmente.
     * O vencedor (menor lance) tem proposta criada automaticamente
     * e o RFQ avança para READY_COMPARE.
     * Requer permissão: MANAGE_RFQS
     */
    @PostMapping("/{rfqId}/close")
    @PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<?> closeAuction(@PathVariable UUID rfqId) {
        try {
            auctionService.closeAuction(rfqId);
            return ResponseEntity
                    .ok(Map.of("message", "Leilão encerrado. Proposta do vencedor criada automaticamente."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
