package com.nagrikHelp.controller;

import com.nagrikHelp.dto.AuthResponse;
import com.nagrikHelp.dto.LoginRequest;
import com.nagrikHelp.dto.RegisterRequest;
import com.nagrikHelp.dto.GoogleAuthRequest;
import com.nagrikHelp.dto.VerifyPhoneRequest;
import com.nagrikHelp.dto.ResendOtpRequest;
import com.nagrikHelp.dto.GoogleAuthRequest;
import com.nagrikHelp.dto.VerifyPhoneRequest;
import com.nagrikHelp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignIn(@RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.loginWithGoogle(request.getIdToken(), request.getEmailConsent(), request.getSmsConsent(), request.getPhone());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody VerifyPhoneRequest request) {
        boolean ok = authService.verifyPhoneOtp(request.getPhone(), request.getCode());
        if (ok) return ResponseEntity.ok().body(java.util.Collections.singletonMap("ok", true));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("ok", false));
    }

    

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        try {
            authService.resendPhoneOtp(request.getPhone());
            return ResponseEntity.ok(java.util.Collections.singletonMap("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("ok", false));
        } catch (com.nagrikHelp.service.OtpRateLimitException ex) {
            long retry = ex.getRetryAfterSeconds();
            return ResponseEntity.status(429).header("Retry-After", String.valueOf(retry)).body(java.util.Map.of("ok", false, "retryAfter", retry));
        }
    }
}
