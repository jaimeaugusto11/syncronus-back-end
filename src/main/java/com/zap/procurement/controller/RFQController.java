package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.dto.ProposalComparisonDTO;
import com.zap.procurement.repository.RFQRepository;
import com.zap.procurement.service.RFQService;
import com.zap.procurement.service.ProposalNegotiationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/rfqs")
@CrossOrigin(origins = "*")
public class RFQController {

    @Autowired
    private RFQService rfqService;

    @Autowired
    private RFQRepository rfqRepository;

    @Autowired
    private ProposalNegotiationService negotiationService;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_RFQS')")
    public ResponseEntity<List<RFQ>> getAllRFQs() {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<RFQ> rfqs = rfqService.getRFQsByTenant(tenantId);
        return ResponseEntity.ok(rfqs);
    }

    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_RFQS')")
    public ResponseEntity<RFQ> getRFQ(@PathVariable UUID id) {
        return rfqRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/from-requisition")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<RFQ> createFromRequisition(
            @RequestParam UUID requisitionId,
            @RequestParam RFQ.RFQType type,
            @RequestParam(required = false, defaultValue = "RFQ") RFQ.ProcessType processType) {

        RFQ rfq = rfqService.createRFQFromRequisition(requisitionId, type, processType);
        return ResponseEntity.ok(rfq);
    }

    @PostMapping("/from-items")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<RFQ> createFromItems(@RequestBody Map<String, Object> request) {
        UUID categoryId = UUID.fromString(request.get("categoryId").toString());
        @SuppressWarnings("unchecked")
        List<String> itemIdsStrings = (List<String>) request.get("itemIds");
        List<UUID> itemIds = itemIdsStrings.stream().map(UUID::fromString).toList();

        @SuppressWarnings("unchecked")
        List<String> supplierIdStrings = request.containsKey("supplierIds")
                ? (List<String>) request.get("supplierIds")
                : null;
        List<UUID> supplierIds = supplierIdStrings != null
                ? supplierIdStrings.stream().map(UUID::fromString).toList()
                : null;
        RFQ.RFQType type = RFQ.RFQType.valueOf(request.get("type").toString());
        RFQ.ProcessType processType = request.containsKey("processType")
                ? RFQ.ProcessType.valueOf(request.get("processType").toString())
                : RFQ.ProcessType.RFQ;

        String title = (String) request.get("title");
        String description = (String) request.get("description");
        java.time.LocalDate closingDate = request.containsKey("closingDate")
                ? java.time.LocalDate.parse(request.get("closingDate").toString())
                : null;
        Integer technicalWeight = request.containsKey("technicalWeight")
                ? Integer.parseInt(request.get("technicalWeight").toString())
                : null;
        Integer financialWeight = request.containsKey("financialWeight")
                ? Integer.parseInt(request.get("financialWeight").toString())
                : null;

        RFQ rfq = rfqService.createRFQForCategory(categoryId, itemIds, type, processType,
                title, description, closingDate, technicalWeight, financialWeight, supplierIds);
        return ResponseEntity.ok(rfq);
    }

    @PostMapping("/{id}/publish")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<RFQ> publishRFQ(@PathVariable UUID id) {
        RFQ rfq = rfqService.publishRFQ(id);
        return ResponseEntity.ok(rfq);
    }

    @PostMapping("/{id}/advance-comparison")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<?> advanceToComparison(@PathVariable UUID id) {
        rfqService.advanceToComparison(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/advance-technical")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<?> advanceToTechnical(@PathVariable UUID id) {
        rfqService.advanceToTechnicalValidation(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/proposals")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_PROPOSALS')")
    public ResponseEntity<List<SupplierProposal>> getProposals(@PathVariable UUID id) {
        List<SupplierProposal> proposals = rfqService.getProposalsByRFQ(id);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/{id}/invited-suppliers")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_RFQS')")
    public ResponseEntity<List<RFQSupplier>> getInvitedSuppliers(@PathVariable UUID id) {
        List<RFQSupplier> invited = rfqService.getInvitedSuppliers(id);
        return ResponseEntity.ok(invited);
    }

    @PostMapping("/proposals")
    public ResponseEntity<SupplierProposal> submitProposal(@RequestBody SupplierProposal proposal) {
        // This endpoint might be used by suppliers. If they are authenticated users
        // with a specific role,
        // we could restricting it. For now, authenticated is enough, or maybe a
        // SUPPLIER permission.
        // Assuming suppliers are users.
        SupplierProposal submitted = rfqService.submitProposal(proposal);
        return ResponseEntity.ok(submitted);
    }

    @PostMapping("/proposals/{id}/evaluate")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<?> evaluateProposal(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> evaluation) {
        BigDecimal technicalScore = new BigDecimal(evaluation.get("technicalScore").toString());
        String notes = (String) evaluation.get("notes");

        rfqService.evaluateProposal(id, technicalScore, notes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comparisons")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<Comparison> createComparison(@RequestBody Map<String, Object> request) {
        // Parse UUIDs from request
        UUID rfqId = UUID.fromString(request.get("rfqId").toString());
        @SuppressWarnings("unchecked")
        List<String> proposalIdStrings = (List<String>) request.get("proposalIds");
        List<UUID> proposalIds = proposalIdStrings.stream().map(UUID::fromString).toList();

        Integer technicalWeight = request.containsKey("technicalWeight")
                ? Integer.parseInt(request.get("technicalWeight").toString())
                : 60;
        Integer financialWeight = request.containsKey("financialWeight")
                ? Integer.parseInt(request.get("financialWeight").toString())
                : 40;

        Comparison comparison = rfqService.createComparison(rfqId, proposalIds, technicalWeight, financialWeight);
        return ResponseEntity.ok(comparison);
    }

    @PostMapping("/comparisons/{id}/select-winner")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<Comparison> selectWinner(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID proposalId = UUID.fromString(request.get("proposalId").toString());
        String justification = (String) request.get("justification");

        Comparison comparison = rfqService.selectWinner(id, proposalId, justification);
        return ResponseEntity.ok(comparison);
    }

    @PostMapping("/{id}/invite-supplier")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<RFQSupplier> inviteSupplier(
            @PathVariable UUID id,
            @RequestParam UUID supplierId) {
        RFQSupplier rfqSupplier = rfqService.inviteSupplier(id, supplierId);
        return ResponseEntity.ok(rfqSupplier);
    }

    @DeleteMapping("/{id}/remove-supplier")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<?> removeSupplier(
            @PathVariable UUID id,
            @RequestParam UUID supplierId) {
        rfqService.removeSupplier(id, supplierId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<RFQSupplier>> getInvitedRFQsForSupplier(@PathVariable UUID supplierId) {
        // Likely purely for suppliers
        List<RFQSupplier> invited = rfqService.getInvitedRFQs(supplierId);
        return ResponseEntity.ok(invited);
    }

    @GetMapping("/{id}/comparative-map")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_RFQS')")
    public ResponseEntity<ProposalComparisonDTO> getComparativeMap(@PathVariable UUID id) {
        ProposalComparisonDTO comparison = rfqService.getComparativeMap(id);
        return ResponseEntity.ok(comparison);
    }

    // ── Negotiation endpoints ────────────────────────────────────────────────

    @GetMapping("/proposals/{proposalId}/messages")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_PROPOSALS')")
    public ResponseEntity<List<ProposalNegotiationMessage>> getProposalMessages(@PathVariable UUID proposalId) {
        return ResponseEntity.ok(negotiationService.getMessages(proposalId));
    }

    @PostMapping("/proposals/{proposalId}/messages")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('MANAGE_RFQS')")
    public ResponseEntity<ProposalNegotiationMessage> sendProposalMessage(
            @PathVariable UUID proposalId,
            @RequestBody Map<String, String> request) {
        String content = request.get("content");
        UUID senderId = UUID.fromString(request.get("senderId"));
        return ResponseEntity.ok(negotiationService.sendMessage(proposalId, content, senderId, false));
    }

    @GetMapping("/proposals/{proposalId}/price-history")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('VIEW_PROPOSALS')")
    public ResponseEntity<List<ProposalPriceHistory>> getProposalPriceHistory(@PathVariable UUID proposalId) {
        return ResponseEntity.ok(negotiationService.getPriceHistory(proposalId));
    }
}
