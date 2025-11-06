package com.nagrikHelp.service;

public class OtpRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public OtpRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
