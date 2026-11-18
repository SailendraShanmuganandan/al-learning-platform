package com.allearning.backend.communication.service;

import com.allearning.backend.communication.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for public contact submissions.
 * <p>
 * Code comment: This in-memory implementation is suitable for single-node deployments.
 * It must be revisited for multi-instance deployment (e.g. distributed Redis token bucket / sliding window)
 * and proxy headers (inspecting trusted client IP from X-Forwarded-For or Cloudflare CF-Connecting-IP).
 */
@Component
public class ContactRateLimiter {

    private final int limitPerHour;
    private final Map<String, Deque<Instant>> ipSubmissions = new ConcurrentHashMap<>();

    public ContactRateLimiter(@Value("${app.contact.rate-limit-per-hour:5}") int limitPerHour) {
        this.limitPerHour = limitPerHour;
    }

    /**
     * Checks whether the given client IP has exceeded the allowed submissions per hour.
     * If exceeded, throws {@link RateLimitExceededException} with Retry-After seconds.
     * Otherwise, registers this submission timestamp.
     *
     * @param clientIp client IP address
     */
    public synchronized void checkRateLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }

        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);

        Deque<Instant> timestamps = ipSubmissions.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        // Evict timestamps older than 1 hour
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(oneHourAgo)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= limitPerHour) {
            Instant oldest = timestamps.peekFirst();
            long retryAfterSeconds = Math.max(1, Duration.between(now, oldest.plus(1, ChronoUnit.HOURS)).getSeconds());
            throw new RateLimitExceededException(
                    retryAfterSeconds,
                    "Too many contact form submissions from this IP. Please try again later."
            );
        }

        timestamps.addLast(now);
    }

    /**
     * Clears rate limit records (primarily for testing).
     */
    public synchronized void reset() {
        ipSubmissions.clear();
    }
}
