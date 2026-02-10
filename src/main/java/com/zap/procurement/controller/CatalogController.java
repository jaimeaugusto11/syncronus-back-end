package com.zap.procurement.controller;

import com.zap.procurement.domain.CatalogItem;
import com.zap.procurement.repository.CatalogItemRepository;
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

    @PostMapping
    public ResponseEntity<CatalogItem> createItem(@RequestBody CatalogItem item) {
        return ResponseEntity.ok(catalogItemRepository.save(item));
    }
}
