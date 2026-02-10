package com.zap.procurement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @org.springframework.beans.factory.annotation.Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extract tenant from header (set by frontend or subdomain)
            String tenantIdStr = request.getHeader("X-Tenant-ID");

            if (tenantIdStr != null && !tenantIdStr.isEmpty()) {
                try {
                    UUID tenantId = UUID.fromString(tenantIdStr);
                    TenantContext.setCurrentTenant(tenantId);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID format
                }
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String tenantIdFromToken = jwtTokenProvider.extractTenantId(token);
                    if (tenantIdFromToken != null) {
                        TenantContext.setCurrentTenant(UUID.fromString(tenantIdFromToken));
                    }

                    // Extract and set user ID
                    String userIdFromToken = jwtTokenProvider.extractUserId(token);
                    if (userIdFromToken != null) {
                        TenantContext.setCurrentUser(UUID.fromString(userIdFromToken));
                    }
                } catch (Exception e) {
                    // System.out.println("[TenantFilter] Error parsing token: " + e.getMessage());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
