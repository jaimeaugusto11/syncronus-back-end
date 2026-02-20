package com.zap.procurement.service;

import com.zap.procurement.domain.*;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.ProposalNegotiationMessageRepository;
import com.zap.procurement.repository.ProposalPriceHistoryRepository;
import com.zap.procurement.repository.SupplierProposalRepository;
import com.zap.procurement.repository.SupplierUserRepository;
import com.zap.procurement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            boolean isFromSupplier, List<String> attachmentUrls) {
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

        ProposalNegotiationMessage saved = messageRepository.save(message);

        if (attachmentUrls != null) {
            for (String url : attachmentUrls) {
                NegotiationAttachment attachment = new NegotiationAttachment();
                attachment.setMessage(saved);
                attachment.setFileUrl(url);
                attachment.setTenantId(proposal.getTenantId());
                // In a real scenario, we'd guess filename/type or pass them from frontend
                saved.getAttachments().add(attachment);
            }
        }

        return messageRepository.save(saved);
    }

    public List<ProposalNegotiationMessage> getRFQGallery(UUID rfqId) {
        // Find all messages for proposals of this RFQ that have attachments
        return messageRepository.findAll().stream()
                .filter(m -> m.getProposal().getRfq().getId().equals(rfqId))
                .filter(m -> m.getAttachments() != null && !m.getAttachments().isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ProposalPriceHistory> getPriceHistory(UUID proposalId) {
        if (proposalId == null)
            throw new IllegalArgumentException("Proposal ID cannot be null");
        return priceHistoryRepository.findByProposalIdOrderByCreatedAtDesc(proposalId);
    }

    @Transactional
    public void recordPriceHistory(SupplierProposal proposal, BigDecimal oldPrice, BigDecimal newPrice,
            UUID changedById,
            String reason, boolean isFromSupplier) {
        ProposalPriceHistory history = new ProposalPriceHistory();
        history.setProposal(proposal);
        history.setOldPrice(oldPrice != null ? oldPrice : BigDecimal.ZERO);
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

        // Maintain consistency with items
        if (proposal.getItems() != null && !proposal.getItems().isEmpty()) {
            if (proposal.getItems().size() == 1) {
                ProposalItem item = proposal.getItems().get(0);
                item.setTotalPrice(newPrice);
                if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    item.setUnitPrice(newPrice.divide(item.getQuantity(), 4, RoundingMode.HALF_UP));
                } else {
                    item.setUnitPrice(newPrice);
                }
            } else if (oldPrice != null && oldPrice.compareTo(BigDecimal.ZERO) > 0) {
                // Proportional update for multiple items
                BigDecimal ratio = newPrice.divide(oldPrice, 10, RoundingMode.HALF_UP);
                for (ProposalItem item : proposal.getItems()) {
                    if (item.getTotalPrice() != null) {
                        item.setTotalPrice(item.getTotalPrice().multiply(ratio).setScale(4, RoundingMode.HALF_UP));
                        if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                            item.setUnitPrice(item.getTotalPrice().divide(item.getQuantity(), 4, RoundingMode.HALF_UP));
                        }
                    }
                }
            }
        }

        recordPriceHistory(proposal, oldPrice, newPrice, changedById, reason, isFromSupplier);

        return proposalRepository.save(proposal);
    }
}
