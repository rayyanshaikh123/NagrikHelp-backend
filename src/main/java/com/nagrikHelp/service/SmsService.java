package com.nagrikHelp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-phone:}")
    private String fromPhone;

    private volatile boolean enabled = false;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostConstruct
    public void init() {
        enabled = accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank() && fromPhone != null && !fromPhone.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Send SMS via Twilio REST API (no Twilio SDK required).
     */
    public void sendSms(String to, String body) {
        if (!enabled || to == null || to.isBlank() || body == null || body.isBlank()) return;
        try {
            log.info("Sending SMS to {} (len={})", to, body.length());
            String form = buildForm(Map.of(
                    "To", to,
                    "From", fromPhone,
                    "Body", body
            ));
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", URLEncoder.encode(accountSid, StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                log.info("Twilio SMS sent to {} status={} body={}", to, resp.statusCode(), resp.body());
                return; // success
            }
            // log non-2xx
            log.warn("Twilio SMS failed to {}: status={} body={}", to, resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.warn("Twilio sendSms error to {}: {}", to, e.getMessage());
        }
    }

    private static String buildForm(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public static String buildSmsBody(String userName, String issueTitle, String issueStatus, String shortMessage) {
        String safe = userName == null ? "User" : userName;
        String title = issueTitle == null ? "issue" : issueTitle;
        String status = issueStatus == null ? "updated" : issueStatus;
        String msg = shortMessage == null ? "" : shortMessage;
        String body = String.format("%s: %s — %s. %s", safe, title, status, msg);
        return body.length() > 320 ? body.substring(0, 317) + "..." : body;
    }
}
