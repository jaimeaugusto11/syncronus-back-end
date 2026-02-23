package com.zap.procurement.controller;

import com.zap.procurement.domain.CatalogItem;
import com.zap.procurement.repository.CatalogItemRepository;
import com.zap.procurement.dto.CheckoutCartDTO;
import com.zap.procurement.domain.Requisition;
import com.zap.procurement.service.RequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
@CrossOrigin(origins = "http://localhost:3000")
public class CatalogController {

    @Autowired
    private CatalogItemRepository catalogItemRepository;

    @GetMapping
    public ResponseEntity<List<CatalogItem>> getAllItems() {
        return ResponseEntity.ok(catalogItemRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogItem> getItemById(@PathVariable UUID id) {
        return catalogItemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Autowired
    private RequisitionService requisitionService;

    @PostMapping("/checkout")
    public ResponseEntity<Requisition> checkout(@RequestBody CheckoutCartDTO checkoutDTO) {
        return ResponseEntity.ok(requisitionService.createFromCatalog(checkoutDTO));
    }
}
