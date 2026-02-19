package com.zap.procurement.service;

import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProposalNegotiationService {

    @Autowired
    private SupplierProposalRepository proposalRepository;

    @Autowired
    private ProposalNegotiationMessageRepository messageRepository;

    @Autowired
    private ProposalPriceHistoryRepository priceHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierUserRepository supplierUserRepository;

    public List<ProposalNegotiationMessage> getMessages(UUID proposalId) {
        if (proposalId == null)
            throw new IllegalArgumentException("Proposal ID cannot be null");
        return messageRepository.findByProposalIdOrderByCreatedAtAsc(proposalId);
    }

    @Transactional
    public ProposalNegotiationMessage sendMessage(UUID proposalId, String content, UUID senderId,
            boolean isFromSupplier) {
        SupplierProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        ProposalNegotiationMessage message = new ProposalNegotiationMessage();
        message.setProposal(proposal);
        message.setContent(content);
        message.setSenderId(senderId);
        message.setFromSupplier(isFromSupplier);
        message.setTenantId(proposal.getTenantId());

        if (isFromSupplier) {
            SupplierUser supplierUser = supplierUserRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Utilizador fornecedor não encontrado"));
            message.setSenderName(supplierUser.getName());
        } else {
            User user = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
            message.setSenderName(user.getName());
        }

        return messageRepository.save(message);
    }

    public List<ProposalPriceHistory> getPriceHistory(UUID proposalId) {
        if (proposalId == null)
            throw new IllegalArgumentException("Proposal ID cannot be null");
        return priceHistoryRepository.findByProposalIdOrderByCreatedAtDesc(proposalId);
    }

    @Transactional
    public SupplierProposal updateProposalPrice(UUID proposalId, BigDecimal newPrice, UUID changedById, String reason,
            boolean isFromSupplier) {
        if (proposalId == null)
            throw new IllegalArgumentException("Proposal ID cannot be null");
        SupplierProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada"));

        BigDecimal oldPrice = proposal.getTotalAmount();
        proposal.setTotalAmount(newPrice);

        ProposalPriceHistory history = new ProposalPriceHistory();
        history.setProposal(proposal);
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setCurrency(proposal.getCurrency());
        history.setChangedById(changedById);
        history.setReason(reason);
        history.setTenantId(proposal.getTenantId());

        if (isFromSupplier) {
            SupplierUser supplierUser = supplierUserRepository.findById(changedById)
                    .orElseThrow(() -> new RuntimeException("Utilizador fornecedor não encontrado"));
            history.setChangedByName(supplierUser.getName());
        } else {
            User user = userRepository.findById(changedById)
                    .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
            history.setChangedByName(user.getName());
        }

        priceHistoryRepository.save(history);
        return proposalRepository.save(proposal);
    }
}
