package com.zap.procurement.repository;

import com.zap.procurement.domain.SupplierUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierUserRepository extends JpaRepository<SupplierUser, UUID> {
    Optional<SupplierUser> findByEmail(String email);
}
