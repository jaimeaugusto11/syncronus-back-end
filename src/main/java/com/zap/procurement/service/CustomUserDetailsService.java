package com.zap.procurement.service;

import com.zap.procurement.domain.User;
import com.zap.procurement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.zap.procurement.repository.SupplierUserRepository supplierUserRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Try to find in standard Users
        java.util.Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (user.getRole() != null) {
                // Add Role as authority (e.g., "ROLE_ADMIN_GERAL")
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getSlug()));

                // Add Permissions as authorities
                if (user.getRole().getPermissions() != null) {
                    authorities.addAll(user.getRole().getPermissions().stream()
                            .map(permission -> new SimpleGrantedAuthority(permission.getSlug()))
                            .collect(Collectors.toList()));
                }
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    authorities);
        }

        // 2. If not found, try Supplier Users
        com.zap.procurement.domain.SupplierUser supplierUser = supplierUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();
        // Map Supplier Role
        if (supplierUser.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + supplierUser.getRole().toString()));
            // Add a generic role for all suppliers if needed
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPPLIER"));
        }

        System.out.println("[Security] Supplier User " + email + " loaded with authorities: " +
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));

        return new org.springframework.security.core.userdetails.User(
                supplierUser.getEmail(),
                supplierUser.getPassword(),
                authorities);
    }
}
