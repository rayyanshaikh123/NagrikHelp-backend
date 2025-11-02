package com.nagrikHelp.controller;

import com.nagrikHelp.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
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
}
