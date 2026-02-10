package com.zap.procurement.controller;

import com.zap.procurement.config.JwtTokenProvider;
import com.zap.procurement.domain.User;
import com.zap.procurement.dto.*;
import com.zap.procurement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Find user by email
        // Note: For multi-tenancy, we might need tenant context or check all tenants
        // Here we assume email is unique globally or we pick the first one
        User user = userService.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Credenciais inválidas");
        }

        // Create UserDetails for JWT generation
        org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>());

        String token = jwtTokenProvider.generateToken(userDetails, user.getTenantId(), user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, user.getTenantId(), user.getId());

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        if (user.getRole() != null) {
            userDTO.setRole(user.getRole().getName());
            userDTO.setPermissions(user.getRole().getPermissions().stream()
                    .map(com.zap.procurement.domain.Permission::getSlug)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        if (user.getDepartment() != null) {
            userDTO.setDepartmentId(user.getDepartment().getId());
            userDTO.setDepartmentName(user.getDepartment().getName());
        }
        userDTO.setTenantId(user.getTenantId());

        return ResponseEntity.ok(new LoginResponse(token, refreshToken, userDTO));
    }
}
