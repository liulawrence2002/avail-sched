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

public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 120;
    private static final long WINDOW_SECONDS = 60;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.windowStart.plusSeconds(WINDOW_SECONDS))) {
                return new Bucket(now, 1);
            }
            return new Bucket(existing.windowStart, existing.count + 1);
        });
        if (bucket.count > LIMIT) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Slow down, goblin.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Bucket(Instant windowStart, int count) {}
}
