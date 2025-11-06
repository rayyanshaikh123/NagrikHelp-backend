package com.nagrikHelp.service;

import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    // Spring Security's User constructor requires non-null, non-empty username and password.
    // Some users (e.g. created via Google sign-in) may not have a password hash set.
    // Provide a safe placeholder password when passwordHash is missing to avoid IllegalArgumentException.
    String pwd = user.getPasswordHash();
    if (pwd == null || pwd.isBlank()) {
        // placeholder value that is non-empty; it's not used for JWT-based auth
        pwd = "[NO_PASSWORD]";
    }
    String username = user.getEmail() != null ? user.getEmail() : "[no-email]";
    return new org.springframework.security.core.userdetails.User(
        username,
        pwd,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    );
    }
}
