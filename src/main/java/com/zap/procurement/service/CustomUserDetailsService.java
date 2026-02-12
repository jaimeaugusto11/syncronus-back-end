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

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null) {
            // Add Role as authority (e.g., "ROLE_ADMIN_GERAL")
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getSlug())); // Using slug as it's
                                                                                             // cleaner

            // Add Permissions as authorities
            if (user.getRole().getPermissions() != null) {
                authorities.addAll(user.getRole().getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.getSlug()))
                        .collect(Collectors.toList()));
            }
        }

        System.out.println("[Security] User " + email + " loaded with authorities: " +
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities);
    }
}
