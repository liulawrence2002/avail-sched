package com.goblin.scheduler.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 120;
    private static final long WINDOW_SECONDS = 60;
    private static final int CLEANUP_INTERVAL = 100;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestsSinceCleanup = new AtomicInteger();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (requestsSinceCleanup.incrementAndGet() >= CLEANUP_INTERVAL) {
            cleanupExpiredBuckets();
        }

        String key = request.getRemoteAddr() + ":" + normalizePath(request.getRequestURI());
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.windowStart.plusSeconds(WINDOW_SECONDS))) {
                return new Bucket(now, now, 1);
            }
            return new Bucket(existing.windowStart, now, existing.count + 1);
        });
        if (bucket.count > LIMIT) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Slow down, goblin.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void cleanupExpiredBuckets() {
        requestsSinceCleanup.set(0);
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        buckets.entrySet().removeIf(entry -> entry.getValue().lastSeen.isBefore(cutoff));
    }

    private String normalizePath(String requestUri) {
        if (requestUri.matches("^/api/events/[^/]+/participants/[^/]+/availability$")) {
            return "/api/events/:publicId/participants/:token/availability";
        }
        if (requestUri.matches("^/api/events/[^/]+/participants$")) {
            return "/api/events/:publicId/participants";
        }
        if (requestUri.matches("^/api/events/[^/]+/results$")) {
            return "/api/events/:publicId/results";
        }
        if (requestUri.matches("^/api/events/[^/]+/final(\\.ics)?$")) {
            return "/api/events/:publicId/final";
        }
        if (requestUri.matches("^/api/events/[^/]+/finalize$")) {
            return "/api/events/:publicId/finalize";
        }
        if (requestUri.matches("^/api/events/[^/]+$")) {
            return "/api/events/:publicId";
        }
        if (requestUri.matches("^/api/host/[^/]+/results$")) {
            return "/api/host/:hostToken/results";
        }
        if (requestUri.matches("^/api/host/[^/]+$")) {
            return "/api/host/:hostToken";
        }
        return requestUri;
    }

    private record Bucket(Instant windowStart, Instant lastSeen, int count) {}
}
