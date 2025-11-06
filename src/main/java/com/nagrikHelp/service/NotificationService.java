package com.nagrikHelp.service;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.UserNotification;
import com.nagrikHelp.repository.UserNotificationRepository;
import com.nagrikHelp.repository.UserRepository;
import com.nagrikHelp.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;
    @Autowired(required = false)
    private EmailService emailService;
    @Autowired(required = false)
    private SmsService smsService;
    @Autowired(required = false)
    private UserNotificationRepository notificationRepository;
    @Autowired(required = false)
    private UserRepository userRepository;
    @Autowired(required = false)
    private NotificationStreamService notificationStreamService; // SSE broadcaster

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // -------------------- Core channel helpers --------------------
    private void sendEmail(String to, String subject, String text) {
        if (to == null || to.isBlank()) return;
        try {
            if (emailService != null && emailService.isEnabled()) {
                emailService.sendEmail(to, subject, EmailService.buildEmailBody(null, subject, "", text));
                return;
            }
            if (mailSender == null) {
                log.info("No mail sender configured; skipping email to {}", to);
                return; // no sender available
            }
            SimpleMailMessage sm = new SimpleMailMessage();
            sm.setTo(to.trim());
            sm.setSubject(subject);
            sm.setText(text);
            mailSender.send(sm);
        } catch (Exception e) {
            log.warn("Email send failed to {}: {}", to, e.getMessage());
        }
    }

    // -------------------- Public API --------------------
    /**
     * Send immediate notification to a specific user (email + SMS if available)
     */
    public void notifyUser(String recipientName,
                           String recipientEmail,
                           String recipientPhone,
                           String issueTitle,
                           String issueStatus,
                           String shortMessage) {
        if ((recipientEmail == null || recipientEmail.isBlank()) && (recipientPhone == null || recipientPhone.isBlank())) return;
        String subj = "Update: " + (issueTitle == null ? "Issue" : issueTitle) + " — " + (issueStatus == null ? "Updated" : issueStatus);
        String emailBody = EmailService.buildEmailBody(recipientName, issueTitle, issueStatus, shortMessage);
        String smsBody = SmsService.buildSmsBody(recipientName, issueTitle, issueStatus, shortMessage);
        long now = System.currentTimeMillis();
        // Determine user-level consent (if we have the user record)
        boolean allowEmail = true;
        boolean allowSms = true;
        if (userRepository != null && recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                java.util.Optional<User> ou = userRepository.findByEmail(recipientEmail.trim());
                if (ou.isPresent()) {
                    User u = ou.get();
                    allowEmail = u.getEmailConsent() == null ? true : Boolean.TRUE.equals(u.getEmailConsent());
                    allowSms = u.getSmsConsent() == null ? false : Boolean.TRUE.equals(u.getSmsConsent());
                    // require phoneVerified for SMS
                    if (!Boolean.TRUE.equals(u.getPhoneVerified())) allowSms = false;
                }
            } catch (Exception ex) {
                log.warn("Error checking user consent for {}: {}", recipientEmail, ex.getMessage());
            }
        }

        // Save notification record if repository present
        if (notificationRepository != null && recipientEmail != null && !recipientEmail.isBlank()) {
            saveNotification(recipientEmail, null, "MANUAL", shortMessage == null ? subj : shortMessage, now);
        }

        // Send channels only if user consent and provider available
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            if (allowEmail) {
                try {
                    log.debug("Sending email to {} (providerAvailable={} )", recipientEmail, emailService != null && emailService.isEnabled());
                } catch (Exception __) {}
                sendEmail(recipientEmail, subj, emailBody);
            } else {
                log.debug("Skipping email send to {} due to user consent=false", recipientEmail);
            }
        }
        if (recipientPhone != null && !recipientPhone.isBlank()) {
            if (allowSms) {
                try { log.debug("Sending SMS to {} (providerAvailable={})", recipientPhone, smsService != null && smsService.isEnabled()); } catch (Exception __) {}
                sendSms(recipientPhone, smsBody);
            } else {
                log.debug("Skipping SMS send to {} due to user consent or phone not verified", recipientPhone);
            }
        }
    }
    public void notifyFollowersOnStatusChange(Issue issue) {
        if (issue == null) return;
        String msg = "Update: Issue '" + truncate(issue.getTitle(),40) + "' is now " + issue.getStatus() + ".";
        // Persist notification for follower emails
        if (notificationRepository != null && issue.getFollowerEmails() != null) {
            long now = System.currentTimeMillis();
            for (String em : issue.getFollowerEmails()) {
                if (em == null || em.isBlank()) continue;
                saveNotification(em.trim(), issue.getId(), "ISSUE_STATUS", msg, now);
            }
        }
        // Email followers (use sendEmail which checks provider & logs if missing)
        if (issue.getFollowerEmails() != null) {
            for (String email : issue.getFollowerEmails()) {
                if (email == null || email.isBlank()) continue;
                sendEmail(email, "Issue Status Update", msg);
            }
        }
        // Webhooks
        if (issue.getFollowerWebhookUrls() != null) {
            for (String url : issue.getFollowerWebhookUrls()) {
                if (url == null || url.isBlank()) continue;
                try {
                    String json = "{\"title\":\"" + escape(issue.getTitle()) + "\",\"status\":\"" + issue.getStatus() + "\",\"message\":\"" + escape(msg) + "\"}";
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url.trim()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();
                    httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                            .exceptionally(ex -> { log.warn("Webhook failed {}: {}", url, ex.getMessage()); return null; });
                } catch (Exception e) {
                    log.warn("Error scheduling webhook to {}: {}", url, e.getMessage());
                }
            }
        }
    }

    public void notifyOwnerOnStatusChange(Issue issue) {
        if (issue == null || issue.getCreatedBy() == null) return;
        String msg = "Your issue '" + truncate(issue.getTitle(),40) + "' is now " + issue.getStatus() + ".";
        long now = System.currentTimeMillis();
        saveNotification(issue.getCreatedBy(), issue.getId(), "ISSUE_STATUS", msg, now);
        if (userRepository != null) {
            userRepository.findByEmail(issue.getCreatedBy()).ifPresent(u -> {
                sendEmail(u.getEmail(), "Issue Status Update", msg);
            });
        }
    }

    public void notifyOwnerOnVote(Issue issue, String voterEmail, long up, long down) {
        if (issue == null || issue.getCreatedBy() == null) return;
        if (issue.getCreatedBy().equalsIgnoreCase(voterEmail)) return; // skip self-vote notification
        String msg = "Your issue '" + truncate(issue.getTitle(),40) + "' received a vote. Up:" + up + " Down:" + down;
        saveNotification(issue.getCreatedBy(), issue.getId(), "ISSUE_VOTE", msg, System.currentTimeMillis());
    }

    public UserNotification createManualNotification(String email, String issueId, String type, String message) {
        if (notificationRepository == null || email == null || email.isBlank()) return null;
        long ts = System.currentTimeMillis();
        try {
        String emailKey = email.trim().toLowerCase();
        UserNotification n = UserNotification.builder()
            .userEmail(emailKey)
                    .issueId(issueId)
                    .type(type == null || type.isBlank() ? "MANUAL" : type.trim())
                    .message(message == null || message.isBlank() ? "Test notification" : message.trim())
                    .createdAt(ts)
                    .read(false)
                    .build();
            notificationRepository.save(n);
            log.debug("Manual notification created email={} id={}", email, n.getId());
            if (notificationStreamService != null) notificationStreamService.broadcast(n);
            return n;
        } catch (Exception e) {
            log.warn("Manual notification failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Report availability of configured providers for diagnostics.
     */
    public java.util.Map<String, Boolean> providerStatus() {
        java.util.Map<String, Boolean> m = new java.util.HashMap<>();
        try { m.put("emailProvider", emailService != null && emailService.isEnabled()); } catch (Exception e) { m.put("emailProvider", false); }
        try { m.put("mailSender", mailSender != null); } catch (Exception e) { m.put("mailSender", false); }
        try { m.put("smsProvider", smsService != null && smsService.isEnabled()); } catch (Exception e) { m.put("smsProvider", false); }
        return m;
    }

    // -------------------- Internal helpers --------------------
    private void saveNotification(String email, String issueId, String type, String message, long ts) {
        if (notificationRepository == null || email == null || email.isBlank()) return;
        try {
            String emailKey = email.trim().toLowerCase();
            UserNotification n = UserNotification.builder()
                    .userEmail(emailKey)
                    .issueId(issueId)
                    .type(type)
                    .message(message)
                    .createdAt(ts)
                    .read(false)
                    .build();
            notificationRepository.save(n);
            log.debug("Notification saved email={} type={} issueId={} id={}", emailKey, type, issueId, n.getId());
            if (notificationStreamService != null) {
                notificationStreamService.broadcast(n);
            }
        } catch (Exception e) {
            log.warn("Save notification failed: {}", e.getMessage());
        }
    }

    private String form(String k, String v) { return URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8); }
    private String basicAuth(String user, String pass) { return "Basic " + java.util.Base64.getEncoder().encodeToString((user+":"+pass).getBytes(StandardCharsets.UTF_8)); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String truncate(String v, int max) { if (v == null) return ""; return v.length() <= max ? v : v.substring(0, max - 3) + "..."; }
    private String escape(String v) { if (v == null) return ""; return v.replace("\\", "\\\\").replace("\"", "\\\""); }

    /**
     * Send an SMS message to the given phone number.
     *
     * This implementation is a safe no-op if no SMS provider is configured. It
     * logs the attempt so callers (and tests) can observe behavior. If you want
     * to enable real SMS sending, replace the body with a provider integration
     * (Twilio, MessageBird, etc.) and add configuration properties.
     */
    public void sendSms(String phoneE164, String message) {
        if (phoneE164 == null || phoneE164.isBlank() || message == null || message.isBlank()) {
            log.debug("sendSms called with empty phone or message; skipping");
            return;
        }
        try {
            if (smsService != null && smsService.isEnabled()) {
                smsService.sendSms(phoneE164, message);
                return;
            }
        } catch (Exception e) {
            log.warn("SMS send failed to {}: {}", phoneE164, e.getMessage());
            return;
        }
        // Fallback: no SMS provider configured — log
        log.info("(noop) sendSms to {} message={}", phoneE164, truncate(message, 160));
    }
}
