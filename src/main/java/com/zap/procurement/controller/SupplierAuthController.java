package com.zap.procurement.controller;

import com.zap.procurement.config.JwtTokenProvider;
import com.zap.procurement.domain.SupplierUser;
import com.zap.procurement.dto.*;
import com.zap.procurement.repository.SupplierUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController
@RequestMapping("/auth/supplier")
@CrossOrigin(origins = "*")
public class SupplierAuthController {

    @Autowired
    private SupplierUserRepository supplierUserRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        SupplierUser user = supplierUserRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid credentials");
        }

        if (!user.isActive()) {
            return ResponseEntity.badRequest().body("User is inactive");
        }

        org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>());

        UUID tenantId = user.getSupplier().getTenantId();
        String token = jwtTokenProvider.generateToken(userDetails, tenantId, user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, tenantId, user.getId());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole("SUPPLIER_" + user.getRole().toString());
        userDTO.setTenantId(tenantId);
        userDTO.setSupplierId(user.getSupplier().getId());

        return ResponseEntity.ok(new SupplierLoginResponse(token, refreshToken, userDTO, user.isMustChangePassword()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        SupplierUser user = supplierUserRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        supplierUserRepository.save(user);

        return ResponseEntity.ok("Password updated successfully");
    }
}
