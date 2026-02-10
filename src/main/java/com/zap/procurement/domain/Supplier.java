package com.zap.procurement.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Supplier extends BaseEntity {

    @Transient
    private String initialPassword;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String nif;

    @Column(nullable = false)
    private String email;

    @Column(name = "contact_person")
    private String contactPerson;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_rating")
    private RiskRating riskRating;

    // Bank details
    @Column(name = "bank_name")
    private String bankName;

    private String iban;
    private String swift;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    // Categories relationship
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierCategory> categories = new ArrayList<>();

    // Documents relationship
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierDocument> documents = new ArrayList<>();

    public enum SupplierStatus {
        ACTIVE, INACTIVE, SUSPENDED, BLACKLISTED
    }

    public enum RiskRating {
        LOW, MEDIUM, HIGH
    }

    // Helper methods for categories
    public void addCategory(Category category, Boolean isPrimary) {
        SupplierCategory sc = new SupplierCategory();
        sc.setSupplier(this);
        sc.setCategory(category);
        sc.setIsPrimary(isPrimary);
        sc.setTenantId(this.getTenantId());
        categories.add(sc);
    }

    public void removeCategory(Category category) {
        categories.removeIf(sc -> sc.getCategory().equals(category));
    }
}
