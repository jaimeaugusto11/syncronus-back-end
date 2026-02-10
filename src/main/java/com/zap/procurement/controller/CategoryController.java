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
    public ResponseEntity<List<Category>> getAllCategories() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return ResponseEntity.ok(categoryRepository.findByTenantIdAndParentIsNull(tenantId));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody CategoryDTO dto) {
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
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        auditLogService.log("CATEGORY", saved.getId(), "CREATE", "Created category: " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable UUID id, @RequestBody CategoryDTO dto) {
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
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to category");
        }

        categoryRepository.deleteById(id);
        auditLogService.log("CATEGORY", id, "DELETE", "Deleted category");
        return ResponseEntity.noContent().build();
    }

    public static class CategoryDTO {
        private String name;
        private String icon;
        private String description;
        private UUID parentId;
        private Boolean active;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public UUID getParentId() {
            return parentId;
        }

        public void setParentId(UUID parentId) {
            this.parentId = parentId;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }
}
