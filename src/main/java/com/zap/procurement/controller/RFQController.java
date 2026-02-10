package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.dto.ProposalComparisonDTO;
import com.zap.procurement.repository.RFQRepository;
import com.zap.procurement.service.RFQService;
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

    @GetMapping
    public ResponseEntity<List<RFQ>> getAllRFQs() {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<RFQ> rfqs = rfqService.getRFQsByTenant(tenantId);
        return ResponseEntity.ok(rfqs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RFQ> getRFQ(@PathVariable UUID id) {
        return rfqRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/from-requisition")
    public ResponseEntity<RFQ> createFromRequisition(
            @RequestParam UUID requisitionId,
            @RequestParam RFQ.RFQType type,
            @RequestParam(required = false, defaultValue = "RFQ") RFQ.ProcessType processType) {

        RFQ rfq = rfqService.createRFQFromRequisition(requisitionId, type, processType);
        return ResponseEntity.ok(rfq);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<RFQ> publishRFQ(@PathVariable UUID id) {
        RFQ rfq = rfqService.publishRFQ(id);
        return ResponseEntity.ok(rfq);
    }

    @GetMapping("/{id}/proposals")
    public ResponseEntity<List<SupplierProposal>> getProposals(@PathVariable UUID id) {
        List<SupplierProposal> proposals = rfqService.getProposalsByRFQ(id);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/{id}/invited-suppliers")
    public ResponseEntity<List<RFQSupplier>> getInvitedSuppliers(@PathVariable UUID id) {
        List<RFQSupplier> invited = rfqService.getInvitedSuppliers(id);
        return ResponseEntity.ok(invited);
    }

    @PostMapping("/proposals")
    public ResponseEntity<SupplierProposal> submitProposal(@RequestBody SupplierProposal proposal) {
        SupplierProposal submitted = rfqService.submitProposal(proposal);
        return ResponseEntity.ok(submitted);
    }

    @PostMapping("/proposals/{id}/evaluate")
    public ResponseEntity<?> evaluateProposal(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> evaluation) {
        BigDecimal technicalScore = new BigDecimal(evaluation.get("technicalScore").toString());
        String notes = (String) evaluation.get("notes");

        rfqService.evaluateProposal(id, technicalScore, notes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comparisons")
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
    public ResponseEntity<Comparison> selectWinner(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID proposalId = UUID.fromString(request.get("proposalId").toString());
        String justification = (String) request.get("justification");

        Comparison comparison = rfqService.selectWinner(id, proposalId, justification);
        return ResponseEntity.ok(comparison);
    }

    @PostMapping("/{id}/invite-supplier")
    public ResponseEntity<RFQSupplier> inviteSupplier(
            @PathVariable UUID id,
            @RequestParam UUID supplierId) {
        RFQSupplier rfqSupplier = rfqService.inviteSupplier(id, supplierId);
        return ResponseEntity.ok(rfqSupplier);
    }

    @DeleteMapping("/{id}/remove-supplier")
    public ResponseEntity<?> removeSupplier(
            @PathVariable UUID id,
            @RequestParam UUID supplierId) {
        rfqService.removeSupplier(id, supplierId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<RFQSupplier>> getInvitedRFQsForSupplier(@PathVariable UUID supplierId) {
        List<RFQSupplier> invited = rfqService.getInvitedRFQs(supplierId);
        return ResponseEntity.ok(invited);
    }

    @GetMapping("/{id}/comparative-map")
    public ResponseEntity<ProposalComparisonDTO> getComparativeMap(@PathVariable UUID id) {
        ProposalComparisonDTO comparison = rfqService.getComparativeMap(id);
        return ResponseEntity.ok(comparison);
    }
}
