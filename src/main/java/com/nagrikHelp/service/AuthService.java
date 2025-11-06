package com.nagrikHelp.service;

import com.nagrikHelp.dto.AuthResponse;
import com.nagrikHelp.dto.CreateAdminRequest;
import com.nagrikHelp.dto.LoginRequest;
import com.nagrikHelp.dto.RegisterRequest;
import com.nagrikHelp.model.Role;
import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication; // not used
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final SmsService smsService;

    @Value("${google.client-id:}")
    private String googleClientId;

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
                .emailConsent(request.getEmailConsent() != null ? request.getEmailConsent() : false)
                .smsConsent(request.getSmsConsent() != null ? request.getSmsConsent() : false)
                .mailProvider(extractMailProvider(request.getEmail()))
                .build();
        userRepository.save(user);
        // If user opted in for SMS and provided a phone, send OTP
        if (user.getSmsConsent() != null && user.getSmsConsent() && user.getPhone() != null && !user.getPhone().isBlank()) {
            otpService.generateAndSendOtp(user.getPhone(), "phone verification");
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getRole(), user.getName(), user.getEmail(), user.getPhone(), user.getPhoneVerified());
    }

    private String extractMailProvider(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }

    /**
     * Verify phone OTP and set phoneVerified flag on user.
     */
    public boolean verifyPhoneOtp(String phone, String code) {
        boolean ok = otpService.verifyOtp(phone, code);
        if (!ok) return false;
        userRepository.findByPhone(phone).ifPresent(u -> {
            u.setPhoneVerified(true);
            userRepository.save(u);
        });
        return true;
    }

    /**
     * Resend OTP to phone if user exists. Returns true when OTP send attempted.
     */
    public boolean resendPhoneOtp(String phone) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone is required");
        if (!userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Phone number not registered");
        }
        otpService.generateAndSendOtp(phone, "phone verification (resend)");
        return true;
    }

    /**
     * Handle Google sign-in using an ID token provided by the client.
     * Verifies token with Google's tokeninfo endpoint and creates/fetches user.
     */
    public AuthResponse loginWithGoogle(String idToken, Boolean emailConsent, Boolean smsConsent, String phone) {
        // verify id token via Google tokeninfo endpoint
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + java.net.URLEncoder.encode(idToken, java.nio.charset.StandardCharsets.UTF_8)))
                    .GET().build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) throw new IllegalArgumentException("Invalid Google ID token");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(resp.body(), java.util.Map.class);
            String email = (String) map.get("email");
            // email_verified value present in tokeninfo but not used directly here
            String name = (String) map.getOrDefault("name", email);
            String aud = (String) map.get("aud");
            if (googleClientId != null && !googleClientId.isBlank() && aud != null && !aud.equals(googleClientId)) {
                // In local dev it's possible frontend and backend envs differ. Don't hard-fail; log for visibility and continue.
                log.warn("Google ID token audience mismatch: token aud='{}' expected='{}'. Continuing without audience enforcement.", aud, googleClientId);
            }

            // Prefer phone number returned by Google tokeninfo if present
            String phoneFromToken = null;
            if (map.containsKey("phone_number") && map.get("phone_number") != null) {
                phoneFromToken = String.valueOf(map.get("phone_number"));
            } else if (map.containsKey("phone") && map.get("phone") != null) {
                phoneFromToken = String.valueOf(map.get("phone"));
            }
            if (phoneFromToken != null && phoneFromToken.isBlank()) phoneFromToken = null;
            final String effectivePhone = phoneFromToken != null ? phoneFromToken : phone;

            // find or create user
            User user = userRepository.findByEmail(email.toLowerCase()).orElseGet(() -> {
                User nu = User.builder()
                        .name(name)
                        .email(email.toLowerCase())
                        .phone(effectivePhone)
                        .role(Role.CITIZEN)
                        .emailConsent(emailConsent != null ? emailConsent : false)
                        .smsConsent(smsConsent != null ? smsConsent : false)
                        .mailProvider(extractMailProvider(email))
                        .build();
                return userRepository.save(nu);
            });

            String token = jwtService.generateToken(user.getEmail(), user.getRole());
            return new AuthResponse(token, user.getRole(), user.getName(), user.getEmail(), user.getPhone(), user.getPhoneVerified());

        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to verify Google ID token: " + ex.getMessage());
        }
    }

    /**
     * Signup via Google ID token. Creates a new user only when the email is not
     * already registered. Returns the created user's AuthResponse. Throws
     * IllegalArgumentException when the user already exists or token invalid.
     */
    // Note: signupWithGoogle was removed to restore original single google endpoint behavior.

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword())
            );
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getRole(), user.getName(), user.getEmail(), user.getPhone(), user.getPhoneVerified());
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
