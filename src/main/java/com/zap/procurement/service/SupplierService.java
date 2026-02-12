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
    private UserRepository userRepository;

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

        // Validate email uniqueness
        if (userRepository.existsByEmail(supplier.getEmail()) ||
                supplierUserRepository.existsByEmail(supplier.getEmail())) {
            throw new RuntimeException("Este email já está em uso no sistema.");
        }

        supplier.setTenantId(tenantId);
        if (supplier.getCode() == null || supplier.getCode().isEmpty()) {
            supplier.setCode(generateSupplierCode(tenantId));
        }

        // Link categories if provided
        if (supplier.getCategories() != null) {
            for (SupplierCategory sc : supplier.getCategories()) {
                sc.setSupplier(supplier);
                if (sc.getTenantId() == null) {
                    sc.setTenantId(tenantId);
                }
            }
        }

        // Link documents if provided
        if (supplier.getDocuments() != null) {
            for (SupplierDocument doc : supplier.getDocuments()) {
                doc.setSupplier(supplier);
                if (doc.getTenantId() == null) {
                    doc.setTenantId(tenantId);
                }
            }
        }

        Supplier saved = supplierRepository.save(supplier);

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
                    if (supplierDetails.getCategories() != null) {
                        existing.getCategories().clear();
                        for (SupplierCategory sc : supplierDetails.getCategories()) {
                            UUID catId = sc.getCategory().getId();
                            Category category = categoryRepository.findById(catId)
                                    .orElseThrow(() -> new RuntimeException("Category not found: " + catId));
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

        boolean modified = false;
        for (com.zap.procurement.dto.CategoryAssignmentDTO dto : categories) {
            // Check if already exists to avoid duplicates
            if (supplier.getCategories().stream()
                    .anyMatch(sc -> sc.getCategory().getId().equals(dto.getCategoryId()))) {
                continue;
            }

            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + dto.getCategoryId()));

            supplier.addCategory(category, dto.getIsPrimary());
            modified = true;
        }

        if (modified) {
            return supplierRepository.save(supplier);
        }

        return supplier;
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
