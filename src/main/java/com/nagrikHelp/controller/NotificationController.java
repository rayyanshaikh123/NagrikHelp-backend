package com.nagrikHelp.controller;

import com.nagrikHelp.dto.NotificationDto;
import com.nagrikHelp.model.UserNotification;
import com.nagrikHelp.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citizen/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final UserNotificationRepository notificationRepository;

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal UserDetails user,
                                      @RequestParam(value = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly) {
        String email = user.getUsername();
        List<UserNotification> list = unreadOnly ?
                notificationRepository.findByUserEmailAndReadIsFalseOrderByCreatedAtDesc(email) :
                notificationRepository.findTop50ByUserEmailOrderByCreatedAtDesc(email);
        return list.stream().map(NotificationDto::from).collect(Collectors.toList());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserDetails user) {
        long c = notificationRepository.countByUserEmailAndReadIsFalse(user.getUsername());
        return Map.of("unread", c);
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markRead(@AuthenticationPrincipal UserDetails user, @RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().body("ids required");
        String email = user.getUsername();
        List<UserNotification> toUpdate = notificationRepository.findAllById(ids).stream()
                .filter(n -> email.equalsIgnoreCase(n.getUserEmail()) && !n.isRead())
                .toList();
        if (!toUpdate.isEmpty()) {
            toUpdate.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(toUpdate);
        }
        return ResponseEntity.ok(Map.of("updated", toUpdate.size()));
    }
}
