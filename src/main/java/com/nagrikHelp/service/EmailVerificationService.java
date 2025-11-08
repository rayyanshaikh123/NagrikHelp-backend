package com.nagrikHelp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.nagrikHelp.repository.OtpThrottleRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailService emailService;
    private final OtpThrottleRepository otpThrottleRepository;

    private final Map<String, EmailEntry> emails = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final long CODE_TTL = 10 * 60; // 10 minutes

    public String generateAndSendCode(String email, String userName) {
        long now = Instant.now().getEpochSecond();
        // Rate limit via same OtpThrottleRepository keyed by email for simplicity
        checkAndRecordSendMongo(email, now);
        String code = String.format("%06d", random.nextInt(1_000_000));
        emails.put(email, new EmailEntry(code, now));
        // Build a friendly HTML body using EmailService helper
        String shortMessage = String.format("Your verification code is %s. It will expire in 10 minutes.", code);
        String htmlBody = EmailService.buildEmailBody(userName == null ? "User" : userName, "Email verification", "", shortMessage);
        try {
            emailService.sendEmail(email, "Verify your email", htmlBody);
        } catch (Exception ex) {
            // log and continue; caller will observe via absence of errors
        }
        return code;
    }

    private void checkAndRecordSendMongo(String email, long now) {
        // Reuse OtpThrottle logic: same approach but keyed by email
        final int MAX_RETRIES = 3;
        int attempts = 0;
        while (attempts++ < MAX_RETRIES) {
            com.nagrikHelp.model.OtpThrottle throttle = otpThrottleRepository.findByPhone(email).orElseGet(() -> {
                com.nagrikHelp.model.OtpThrottle t = new com.nagrikHelp.model.OtpThrottle();
                t.setPhone(email);
                t.setSends(new java.util.ArrayList<>());
                return t;
            });

            java.util.List<Long> sends = throttle.getSends();
            if (sends == null) sends = new java.util.ArrayList<>();
            // prune older than window (reuse default)
            long windowSeconds = 3600;
            sends.removeIf(ts -> now - ts > windowSeconds);
            if (sends.size() >= 5) {
                throw new RuntimeException("Rate limit exceeded") ;
            }
            sends.add(now);
            throttle.setSends(sends);
            try { otpThrottleRepository.save(throttle); return; } catch (org.springframework.dao.ConcurrencyFailureException ex) {}
        }
        throw new RuntimeException("Unable to record send") ;
    }

    public boolean verifyCode(String email, String code) {
        EmailEntry e = emails.get(email);
        if (e == null) return false;
        long now = Instant.now().getEpochSecond();
        if (now - e.createdAt > CODE_TTL) { emails.remove(email); return false; }
        boolean ok = e.code.equals(code);
        if (ok) emails.remove(email);
        return ok;
    }

    static class EmailEntry {
        final String code;
        final long createdAt;
        EmailEntry(String c, long t) { this.code = c; this.createdAt = t; }
    }
}
