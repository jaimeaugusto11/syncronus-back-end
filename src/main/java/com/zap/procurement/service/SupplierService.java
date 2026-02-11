package com.zap.procurement.service;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.*;
import com.zap.procurement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SupplierUserRepository supplierUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private SupplierCategoryRepository supplierCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierDocumentRepository supplierDocumentRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    public List<Supplier> getAllSuppliers() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return supplierRepository.findByTenantId(tenantId);
    }

    public List<Supplier> getActiveSuppliers() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return supplierRepository.findByTenantId(tenantId).stream()
                .filter(s -> s.getStatus() == Supplier.SupplierStatus.ACTIVE)
                .toList();
    }

    public Supplier getSupplier(UUID id) {
        return supplierRepository.findById(id)
                .orElse(null);
    }

    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        UUID tenantId = TenantContext.getCurrentTenant();

        supplier.setTenantId(tenantId);
        if (supplier.getCode() == null || supplier.getCode().isEmpty()) {
            supplier.setCode(generateSupplierCode(tenantId));
        }
        Supplier saved = supplierRepository.save(supplier);

        // Process categories if provided
        if (supplier.getCategories() != null && !supplier.getCategories().isEmpty()) {
            for (com.zap.procurement.domain.SupplierCategory sc : supplier.getCategories()) {
                sc.setSupplier(saved);
                supplierCategoryRepository.save(sc);
            }
        }

        // Create initial SupplierUser
        if (supplier.getInitialPassword() != null && !supplier.getInitialPassword().isEmpty()) {
            SupplierUser user = new SupplierUser();
            user.setSupplier(saved);
            user.setName(saved.getName());
            user.setEmail(saved.getEmail());
            user.setPassword(passwordEncoder.encode(supplier.getInitialPassword()));
            user.setRole(SupplierUser.SupplierRole.ADMIN_SUPPLIER);
            user.setMustChangePassword(true);
            user.setActive(true);
            supplierUserRepository.save(user);
        }

        return saved;
    }

    @Transactional
    public Supplier updateSupplier(UUID id, Supplier supplierDetails) {
        return supplierRepository.findById(id)
                .map(existing -> {
                    // Update basic fields
                    existing.setName(supplierDetails.getName());
                    existing.setContactPerson(supplierDetails.getContactPerson());
                    existing.setNif(supplierDetails.getNif());
                    existing.setEmail(supplierDetails.getEmail());
                    existing.setPhone(supplierDetails.getPhone());
                    existing.setAddress(supplierDetails.getAddress());
                    existing.setStatus(supplierDetails.getStatus());
                    existing.setRiskRating(supplierDetails.getRiskRating());
                    existing.setBankName(supplierDetails.getBankName());
                    existing.setIban(supplierDetails.getIban());
                    existing.setSwift(supplierDetails.getSwift());
                    existing.setWebsite(supplierDetails.getWebsite());
                    existing.setDescription(supplierDetails.getDescription());
                    existing.setLogoUrl(supplierDetails.getLogoUrl());

                    // Bulk update categories if provided
                    if (supplierDetails.getCategories() != null && !supplierDetails.getCategories().isEmpty()) {
                        existing.getCategories().clear();
                        for (SupplierCategory sc : supplierDetails.getCategories()) {
                            Category category = categoryRepository.findById(sc.getCategory().getId())
                                    .orElseThrow(() -> new RuntimeException("Category not found"));
                            existing.addCategory(category, sc.getIsPrimary());
                        }
                    }

                    return supplierRepository.save(existing);
                })
                .orElse(null);
    }

    @Transactional
    public SupplierDocument addDocument(UUID supplierId, SupplierDocument doc) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        doc.setSupplier(supplier);
        doc.setTenantId(supplier.getTenantId());
        return supplierDocumentRepository.save(doc);
    }

    @Transactional
    public void removeDocument(UUID documentId) {
        supplierDocumentRepository.deleteById(documentId);
    }

    public List<SupplierDocument> getSupplierDocuments(UUID supplierId) {
        return supplierDocumentRepository.findBySupplierId(supplierId);
    }

    @Transactional
    public Supplier addCategoriesToSupplier(UUID supplierId,
            List<com.zap.procurement.dto.CategoryAssignmentDTO> categories) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        for (com.zap.procurement.dto.CategoryAssignmentDTO dto : categories) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));

            if (supplierCategoryRepository.existsBySupplierIdAndCategoryId(supplierId, dto.getCategoryId())) {
                continue;
            }

            supplier.addCategory(category, dto.getIsPrimary());
        }

        supplierRepository.save(supplier);
        supplierRepository.flush();

        // Reload with EntityGraph to ensure categories are loaded
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found after save"));
    }

    @Transactional
    public void removeCategoryFromSupplier(UUID supplierId, UUID categoryId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        supplier.removeCategory(categoryRepository.getReferenceById(categoryId));
        supplierRepository.save(supplier);
    }

    public List<com.zap.procurement.dto.CategoryAssignmentDTO> getSupplierCategories(UUID supplierId) {
        return supplierCategoryRepository.findBySupplierWithCategories(supplierId).stream()
                .map(sc -> {
                    com.zap.procurement.dto.CategoryAssignmentDTO dto = new com.zap.procurement.dto.CategoryAssignmentDTO();
                    dto.setCategoryId(sc.getCategory().getId());
                    dto.setIsPrimary(sc.getIsPrimary());
                    dto.setRating(sc.getRating());
                    // dto.setCategoryName(sc.getCategory().getName()); // If we add this to DTO
                    return dto;
                })
                .toList();
    }

    public List<com.zap.procurement.dto.SupplierSuggestionDTO> suggestSuppliersForCategory(UUID categoryId) {
        UUID tenantId = TenantContext.getCurrentTenant();
        List<SupplierCategory> scs = supplierCategoryRepository.findActiveSuppliersForCategory(categoryId, tenantId);

        return scs.stream()
                .map(sc -> {
                    com.zap.procurement.dto.SupplierSuggestionDTO dto = new com.zap.procurement.dto.SupplierSuggestionDTO();
                    dto.setId(sc.getSupplier().getId());
                    dto.setName(sc.getSupplier().getName());
                    dto.setEmail(sc.getSupplier().getEmail());
                    dto.setContactPerson(sc.getSupplier().getContactPerson());
                    dto.setIsPrimaryCategory(sc.getIsPrimary());
                    dto.setRating(sc.getRating());
                    dto.setRiskRating(sc.getSupplier().getRiskRating());
                    return dto;
                })
                .toList();
    }

    private String generateSupplierCode(UUID tenantId) {
        long count = supplierRepository.findByTenantId(tenantId).size();
        return String.format("SUP-%d-%05d", java.time.Year.now().getValue(), count + 1);
    }
}
