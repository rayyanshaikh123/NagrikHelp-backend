package com.nagrikHelp.service;

import com.nagrikHelp.dto.AuthResponse;
import com.nagrikHelp.dto.CreateAdminRequest;
import com.nagrikHelp.dto.LoginRequest;
import com.nagrikHelp.dto.RegisterRequest;
import com.nagrikHelp.model.Role;
import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone already in use");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CITIZEN)
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getRole(), user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
            );
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getRole(), user.getName(), user.getEmail());
    }

    public User createAdmin(CreateAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone already in use");
        }
        User admin = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();
        return userRepository.save(admin);
    }
}
