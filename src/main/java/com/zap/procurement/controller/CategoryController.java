package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Category;
import com.zap.procurement.repository.CategoryRepository;
import com.zap.procurement.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<com.zap.procurement.dto.CategoryDTO>> getAllCategories() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(categoryRepository.findByTenantIdAndParentIsNull(tenantId).stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.CategoryDTO> createCategory(
            @RequestBody com.zap.procurement.dto.CategoryDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();

        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setTenantId(tenantId);
        category.setActive(dto.getActive() != null ? dto.getActive() : true);

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            if (!parent.getTenantId().equals(tenantId)) {
                throw new RuntimeException("Unauthorized access to parent category");
            }
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        auditLogService.log("CATEGORY", saved.getId(), "CREATE", "Created category: " + saved.getName());
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<com.zap.procurement.dto.CategoryDTO> updateCategory(@PathVariable UUID id,
            @RequestBody com.zap.procurement.dto.CategoryDTO dto) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to category");
        }

        String oldValue = "Name: " + category.getName() + ", Description: " + category.getDescription() + ", Active: "
                + category.isActive();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setDescription(dto.getDescription());
        category.setActive(dto.getActive() != null ? dto.getActive() : category.isActive());

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            if (!parent.getTenantId().equals(tenantId)) {
                throw new RuntimeException("Unauthorized access to parent category");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        String newValue = "Name: " + saved.getName() + ", Description: " + saved.getDescription() + ", Active: "
                + saved.isActive();
        auditLogService.log("CATEGORY", saved.getId(), "UPDATE", "Updated category: " + saved.getName(), oldValue,
                newValue);
        return ResponseEntity.ok(toDTO(saved));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to category");
        }

        categoryRepository.delete(category);
        auditLogService.log("CATEGORY", id, "DELETE", "Deleted category: " + category.getName());
        return ResponseEntity.noContent().build();
    }

    private com.zap.procurement.dto.CategoryDTO toDTO(Category category) {
        com.zap.procurement.dto.CategoryDTO dto = new com.zap.procurement.dto.CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setDescription(category.getDescription());
        dto.setActive(category.isActive());
        if (category.getParent() != null) {
            dto.setParentId(category.getParent().getId());
        }
        return dto;
    }
}
