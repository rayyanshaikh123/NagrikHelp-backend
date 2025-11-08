package com.nagrikHelp.controller;

import com.nagrikHelp.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @DeleteMapping
    public ResponseEntity<?> deleteSelf(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        boolean deleted = accountService.deleteAccount(user.getUsername());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        var dto = accountService.getProfile(user.getUsername());
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@AuthenticationPrincipal UserDetails user, @RequestBody com.nagrikHelp.dto.UpdateAccountRequest req) {
        if (user == null) return ResponseEntity.status(401).build();
        var dto = accountService.updateProfile(user.getUsername(), req);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me/send-email-verification")
    public ResponseEntity<?> sendEmailVerification(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        accountService.sendEmailVerification(user.getUsername());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/me/verify-email")
    public ResponseEntity<?> verifyEmailCode(@AuthenticationPrincipal UserDetails user, @RequestBody com.nagrikHelp.dto.EmailVerifyRequest req) {
        if (user == null) return ResponseEntity.status(401).build();
        boolean ok = accountService.verifyEmailCode(user.getUsername(), req.getCode());
        if (!ok) return ResponseEntity.status(400).body(java.util.Collections.singletonMap("verified", false));
        return ResponseEntity.ok(java.util.Collections.singletonMap("verified", true));
    }
}
