package com.stockexchange.security;

import com.stockexchange.entity.User;
import com.stockexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    // Spring Security calls loadUserByUsername() automatically during
    // authentication. This bridges User entity with Spring Security.

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));

        // Convert our Role enum into a Spring Security GrantedAuthority.
        // SimpleGrantedAuthority("ROLE_USER") or SimpleGrantedAuthority("ROLE_ADMIN")
        // This is what @PreAuthorize("hasRole('ADMIN')") checks against.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),        // enabled — inactive users cannot authenticate
                true,                   // accountNonExpired
                true,                   // credentialsNonExpired
                true,                   // accountNonLocked
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}