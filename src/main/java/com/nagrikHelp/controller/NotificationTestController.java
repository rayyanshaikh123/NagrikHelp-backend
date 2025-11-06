package com.nagrikHelp.controller;

import com.nagrikHelp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationTestController {

    private final NotificationService notificationService;

    @PostMapping("/test")
    public ResponseEntity<?> testNotify(@RequestBody Map<String, Object> body) {
        String name = (String) body.getOrDefault("name", "User");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String title = (String) body.getOrDefault("title", "Sample issue");
        String status = (String) body.getOrDefault("status", "Updated");
        String message = (String) body.getOrDefault("message", "Your reported issue has an update.");

    // create a DB notification record and attempt to send channels
    notificationService.createManualNotification(email, null, "MANUAL", message);
    // direct immediate send
    notificationService.notifyUser(name, email, phone, title, status, message);

        var statusMap = notificationService.providerStatus();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "emailProvided", email != null,
                "smsProvided", phone != null,
                "emailProviderEnabled", statusMap.getOrDefault("emailProvider", false),
                "mailSenderPresent", statusMap.getOrDefault("mailSender", false),
                "smsProviderEnabled", statusMap.getOrDefault("smsProvider", false)
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        var statusMap = notificationService.providerStatus();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "emailProviderEnabled", statusMap.getOrDefault("emailProvider", false),
                "mailSenderPresent", statusMap.getOrDefault("mailSender", false),
                "smsProviderEnabled", statusMap.getOrDefault("smsProvider", false)
        ));
    }
}
