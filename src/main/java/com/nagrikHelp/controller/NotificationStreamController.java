package com.nagrikHelp.controller;

import com.nagrikHelp.model.Role;
import com.nagrikHelp.service.JwtService;
import com.nagrikHelp.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/citizen/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final JwtService jwtService;
    private final NotificationStreamService streamService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@RequestParam("token") String token) {
        try {
            if (token == null || token.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String email = jwtService.extractUsername(token);
            if (!jwtService.isTokenValid(token, email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Role role = jwtService.extractRole(token);
            if (role == null || role != Role.CITIZEN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            SseEmitter emitter = streamService.addEmitter(email);
            return ResponseEntity.ok(emitter);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
