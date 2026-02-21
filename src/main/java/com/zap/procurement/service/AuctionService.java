package com.zap.procurement.service;

import com.zap.procurement.domain.AuctionBid;
import com.zap.procurement.domain.ProposalItem;
import com.zap.procurement.domain.RFQ;
import com.zap.procurement.domain.RFQItem;
import com.zap.procurement.domain.Supplier;
import com.zap.procurement.domain.SupplierProposal;
import com.zap.procurement.repository.AuctionBidRepository;
import com.zap.procurement.repository.RFQRepository;
import com.zap.procurement.repository.SupplierProposalRepository;
import com.zap.procurement.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private SupplierProposalRepository supplierProposalRepository;

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
            throw new RuntimeException("O leilão já terminou");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado"));

        AuctionBid currentLeader = getLeadingBid(rfqId);

        // Validação: Lance deve ser inferior ao líder atual pelo incremento mínimo
        if (currentLeader != null) {
            BigDecimal minDecrease = rfq.getMinIncrement() != null ? rfq.getMinIncrement() : BigDecimal.ZERO;
            if (amount.compareTo(currentLeader.getBidAmount().subtract(minDecrease)) > 0) {
                throw new RuntimeException("Lance deve ser pelo menos " + minDecrease + " inferior ao lance líder");
            }
        } else if (rfq.getStartingPrice() != null && amount.compareTo(rfq.getStartingPrice()) > 0) {
            throw new RuntimeException("Lance inicial deve ser inferior ao preço teto de " + rfq.getStartingPrice());
        }

        // Remover posição de líder do bid anterior
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
                .orElseThrow(() -> new RuntimeException("RFQ não encontrado"));

        if (rfq.getProcessType() != RFQ.ProcessType.REVERSE_AUCTION) {
            throw new RuntimeException("Apenas processos do tipo Leilão Reverso podem iniciar um leilão.");
        }

        if (rfq.getStatus() != RFQ.RFQStatus.OPEN
                && rfq.getStatus() != RFQ.RFQStatus.PUBLISHED
                && rfq.getStatus() != RFQ.RFQStatus.DRAFT) {
            throw new RuntimeException(
                    "O processo não está num estado válido para iniciar o leilão. Status: " + rfq.getStatus());
        }

        rfq.setStatus(RFQ.RFQStatus.AUCTION_IN_PROGRESS);
        rfq.setAuctionStartTime(LocalDateTime.now());

        int duration = (durationMinutes != null && durationMinutes > 0) ? durationMinutes : 60;
        rfq.setAuctionEndTime(LocalDateTime.now().plusMinutes(duration));

        rfqRepository.save(rfq);
    }

    /**
     * Encerra o leilão reverso, identifica o vencedor (menor lance) e
     * cria automaticamente uma SupplierProposal com o valor do lance vencedor.
     * Transiciona o RFQ para READY_COMPARE para adjudicação normal.
     */
    @Transactional
    public void closeAuction(UUID rfqId) {
        RFQ rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new RuntimeException("RFQ não encontrado"));

        if (rfq.getStatus() != RFQ.RFQStatus.AUCTION_IN_PROGRESS) {
            throw new RuntimeException("O leilão não está em progresso. Status atual: " + rfq.getStatus());
        }

        // Obter o lance vencedor (menor valor)
        AuctionBid winningBid = getLeadingBid(rfqId);

        if (winningBid != null) {
            Supplier winner = winningBid.getSupplier();

            // Criar proposta automática do fornecedor vencedor
            SupplierProposal proposal = new SupplierProposal();
            proposal.setRfq(rfq);
            proposal.setSupplier(winner);
            proposal.setTotalAmount(winningBid.getBidAmount());
            proposal.setCurrency("AOA");
            proposal.setStatus(SupplierProposal.ProposalStatus.SUBMITTED);
            proposal.setSubmittedAt(LocalDateTime.now());
            proposal.setNotes("✅ Proposta gerada automaticamente via Leilão Reverso. " +
                    "Lance vencedor: " + winningBid.getBidAmount() + " AOA por " + winner.getName());
            proposal.setTenantId(rfq.getTenantId());

            // Criar itens de proposta distributindo o valor pelos itens do RFQ
            List<RFQItem> rfqItems = rfq.getItems();
            if (rfqItems != null && !rfqItems.isEmpty()) {
                int itemCount = rfqItems.size();
                for (RFQItem rfqItem : rfqItems) {
                    ProposalItem pItem = new ProposalItem();
                    pItem.setProposal(proposal);
                    pItem.setRfqItem(rfqItem);

                    int qty = rfqItem.getQuantity() != null ? rfqItem.getQuantity().intValue() : 1;
                    pItem.setQuantity(qty);

                    // Distribuir o valor do lance igualmente pelos itens
                    BigDecimal unitPrice = winningBid.getBidAmount()
                            .divide(BigDecimal.valueOf(itemCount), 2, RoundingMode.HALF_UP);
                    pItem.setUnitPrice(unitPrice);
                    pItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)));
                    pItem.setTenantId(rfq.getTenantId());

                    proposal.getItems().add(pItem);
                }
            }

            supplierProposalRepository.save(proposal);
        }

        // Transicionar para READY_COMPARE para adjudicação normal
        rfq.setStatus(RFQ.RFQStatus.READY_COMPARE);
        rfq.setAuctionEndTime(LocalDateTime.now()); // Registar hora real de encerramento
        rfqRepository.save(rfq);
    }
}
