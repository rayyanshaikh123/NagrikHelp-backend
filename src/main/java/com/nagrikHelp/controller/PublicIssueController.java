package com.nagrikHelp.controller;

import com.nagrikHelp.dto.FollowRequest;
import com.nagrikHelp.dto.IssueResponse;
import com.nagrikHelp.dto.PublicIssueResponse;
import com.nagrikHelp.dto.ShareSmsRequest;
import com.nagrikHelp.service.IssueService;
import com.nagrikHelp.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/public/issues")
@RequiredArgsConstructor
public class PublicIssueController {

    private final IssueService issueService;
    private final NotificationService notificationService;
    private static final Pattern E164 = Pattern.compile("^\\+?[1-9]\\d{7,14}$");

    @GetMapping("/{token}")
    public ResponseEntity<PublicIssueResponse> getByShareToken(@PathVariable("token") String token,
                                                               @RequestParam(value = "email", required = false) String email,
                                                               @RequestParam(value = "phone", required = false) String phone) {
        return issueService.getIssueByShareToken(token, email, phone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{token}/follow")
    public ResponseEntity<IssueResponse> follow(@PathVariable("token") String token, @RequestBody FollowRequest req) {
        if (req == null || ((req.getPhone()==null||req.getPhone().isBlank()) && (req.getEmail()==null||req.getEmail().isBlank()) && (req.getWebhookUrl()==null||req.getWebhookUrl().isBlank()))) {
            return ResponseEntity.badRequest().build();
        }
        return issueService.followByShareToken(token, req)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{token}/unfollow")
    public ResponseEntity<IssueResponse> unfollow(@PathVariable("token") String token, @RequestBody FollowRequest req) {
        if (req == null || ((req.getPhone()==null||req.getPhone().isBlank()) && (req.getEmail()==null||req.getEmail().isBlank()) && (req.getWebhookUrl()==null||req.getWebhookUrl().isBlank()))) {
            return ResponseEntity.badRequest().build();
        }
        return issueService.unfollowByShareToken(token, req)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{token}/share/sms")
    public ResponseEntity<?> shareViaSms(@PathVariable("token") String token,
                                         @RequestBody ShareSmsRequest req,
                                         HttpServletRequest http) {
        if (req == null || req.getPhone() == null || req.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body("phone required");
        }
        if (!E164.matcher(req.getPhone().trim()).matches()) {
            return ResponseEntity.badRequest().body("invalid phone format");
        }
        return issueService.getIssueByShareToken(token).map(pub -> {
            // Construct link (derive base URL from request)
            String scheme = http.getScheme();
            String host = http.getServerName();
            int port = http.getServerPort();
            String portPart = (port == 80 || port == 443) ? "" : ":" + port;
            String link = scheme + "://" + host + portPart + "/share/" + token;
            String baseMsg = (req.getMessage() != null && !req.getMessage().isBlank()) ? req.getMessage().trim() : ("Issue: " + truncate(pub.getTitle(), 60));
            String sms = baseMsg + " " + link;
            // Send SMS
            notificationService.sendSms(req.getPhone().trim(), sms);
            boolean subscribed = false;
            if (req.isSubscribe()) {
                issueService.followByShareToken(token, buildFollow(req.getPhone().trim()));
                subscribed = true;
            }
            return ResponseEntity.accepted().body(java.util.Map.of(
                    "sent", true,
                    "subscribed", subscribed,
                    "phone", req.getPhone().trim()
            ));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private FollowRequest buildFollow(String phone) {
        FollowRequest fr = new FollowRequest();
        fr.setPhone(phone);
        return fr;
    }

    private String truncate(String v, int max) { return v == null ? "" : (v.length() <= max ? v : v.substring(0, max - 3) + "..."); }
}
