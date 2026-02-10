package com.zap.procurement.controller;

import com.zap.procurement.domain.Supplier;
import com.zap.procurement.domain.SupplierDocument;
import com.zap.procurement.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Supplier>> getActiveSuppliers() {
        List<Supplier> suppliers = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplier(@PathVariable UUID id) {
        Supplier supplier = supplierService.getSupplier(id);
        return supplier != null ? ResponseEntity.ok(supplier) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        Supplier saved = supplierService.createSupplier(supplier);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable UUID id, @RequestBody Supplier supplier) {
        Supplier updated = supplierService.updateSupplier(id, supplier);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/categories")
    public ResponseEntity<?> addCategories(
            @PathVariable UUID id,
            @RequestBody List<com.zap.procurement.dto.CategoryAssignmentDTO> categories) {
        supplierService.addCategoriesToSupplier(id, categories);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/categories/{categoryId}")
    public ResponseEntity<?> removeCategory(
            @PathVariable UUID id,
            @PathVariable UUID categoryId) {
        supplierService.removeCategoryFromSupplier(id, categoryId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/categories")
    public ResponseEntity<List<com.zap.procurement.dto.CategoryAssignmentDTO>> getSupplierCategories(
            @PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getSupplierCategories(id));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<com.zap.procurement.dto.SupplierSuggestionDTO>> suggestSuppliers(
            @RequestParam UUID categoryId) {
        return ResponseEntity.ok(supplierService.suggestSuppliersForCategory(categoryId));
    }

    // Document management endpoints
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<SupplierDocument>> getDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getSupplierDocuments(id));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<SupplierDocument> addDocument(
            @PathVariable UUID id,
            @RequestBody SupplierDocument document) {
        return ResponseEntity.ok(supplierService.addDocument(id, document));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> removeDocument(@PathVariable UUID documentId) {
        supplierService.removeDocument(documentId);
        return ResponseEntity.ok().build();
    }
}
