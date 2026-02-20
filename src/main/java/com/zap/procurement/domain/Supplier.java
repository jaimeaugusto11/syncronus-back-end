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

    // Manual Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus status) {
        this.status = status;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(RiskRating riskRating) {
        this.riskRating = riskRating;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getSwift() {
        return swift;
    }

    public void setSwift(String swift) {
        this.swift = swift;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public List<SupplierCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<SupplierCategory> categories) {
        this.categories = categories;
    }

    public List<SupplierDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<SupplierDocument> documents) {
        this.documents = documents;
    }
}
