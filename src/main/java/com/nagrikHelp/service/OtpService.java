package com.nagrikHelp.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.nagrikHelp.model.OtpThrottle;
import com.nagrikHelp.repository.OtpThrottleRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final SmsService smsService;
    private final OtpThrottleRepository otpThrottleRepository;

    // OTP entries keyed by phone number (in-memory for active otps)
    private final Map<String, OtpEntry> otps = new ConcurrentHashMap<>();

    private final Random random = new Random();

    // TTL in seconds
    private static final long OTP_TTL = 5 * 60; // 5 minutes

    // Configurable limits
    @org.springframework.beans.factory.annotation.Value("${otp.resend.cooldown.seconds:60}")
    private long resendCooldownSeconds = 60;

    @org.springframework.beans.factory.annotation.Value("${otp.resend.max-per-window:5}")
    private int maxPerWindow = 5;

    @org.springframework.beans.factory.annotation.Value("${otp.resend.window-seconds:3600}")
    private long windowSeconds = 3600; // 1 hour

    public String generateAndSendOtp(String phone, String purpose) {
        long now = Instant.now().getEpochSecond();
        checkAndRecordSendMongo(phone, now);

        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpEntry entry = new OtpEntry(code, now);
        otps.put(phone, entry);

        String body = String.format("Your verification code for %s is %s. It will expire in 5 minutes.", purpose, code);
        if (smsService != null && smsService.isEnabled()) {
            smsService.sendSms(phone, body);
        }
        return code;
    }

    private void checkAndRecordSendMongo(String phone, long now) {
        final int MAX_RETRIES = 5;
        int attempts = 0;
        while (attempts++ < MAX_RETRIES) {
            OtpThrottle throttle = otpThrottleRepository.findByPhone(phone).orElseGet(() -> {
                OtpThrottle t = new OtpThrottle();
                t.setPhone(phone);
                t.setSends(new ArrayList<>());
                return t;
            });

            List<Long> sends = throttle.getSends();
            if (sends == null) sends = new ArrayList<>();

            // prune old entries older than window
            sends.removeIf(ts -> now - ts > windowSeconds);

            // determine rate-limit state and retry-after value
            if (!sends.isEmpty()) {
                long last = sends.get(sends.size() - 1);
                long sinceLast = now - last;
                if (sinceLast < resendCooldownSeconds) {
                    long retryAfter = resendCooldownSeconds - sinceLast;
                    throw new OtpRateLimitException("Cooldown active", retryAfter);
                }
            }

            if (sends.size() >= maxPerWindow) {
                long oldest = sends.get(0);
                long retryAfter = windowSeconds - (now - oldest);
                if (retryAfter < 0) retryAfter = 0;
                throw new OtpRateLimitException("Exceeded max sends", retryAfter);
            }

            // append and try to save (optimistic locking)
            sends.add(now);
            throttle.setSends(sends);
            try {
                otpThrottleRepository.save(throttle);
                return; // success
            } catch (org.springframework.dao.ConcurrencyFailureException ex) {
                // retry loop will attempt again
            }
        }
        // if we reach here, too many concurrent modifications
        throw new OtpRateLimitException("Server busy, try again", 10);
    }

    public boolean verifyOtp(String phone, String code) {
        OtpEntry entry = otps.get(phone);
        if (entry == null) return false;
        long now = Instant.now().getEpochSecond();
        if (now - entry.createdAt > OTP_TTL) {
            otps.remove(phone);
            return false;
        }
        boolean ok = entry.code.equals(code);
        if (ok) otps.remove(phone);
        return ok;
    }

    @Data
    @AllArgsConstructor
    static class OtpEntry {
        String code;
        long createdAt;
    }
}
