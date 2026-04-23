package com.goblinscheduler.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS = 120;
    private static final int MAX_AI_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final long AI_WINDOW_MS = 3_600_000; // 1 hour

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, RateLimitBucket> aiBuckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String normalizedPath = normalizePath(httpRequest.getRequestURI());

        // Stricter rate limit for AI endpoints
        if (isAIEndpoint(httpRequest.getRequestURI())) {
            String aiKey = clientIp + ":ai";
            RateLimitBucket aiBucket = aiBuckets.computeIfAbsent(aiKey, k -> new RateLimitBucket(MAX_AI_REQUESTS, AI_WINDOW_MS));
            if (!aiBucket.tryConsume()) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"message\":\"AI rate limit exceeded. Maximum 10 AI calls per hour.\"}");
                return;
            }
        }

        String key = clientIp + ":" + normalizedPath;
        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket());

        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"message\":\"Rate limit exceeded\"}");
        }
    }

    private boolean isAIEndpoint(String uri) {
        return uri.contains("/api/ai/") || uri.contains("/ai-suggestions")
                || uri.contains("/chat") || uri.contains("/generate-prep")
                || uri.contains("/generate-followup");
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        String riHeader = request.getHeader("X-Real-Ip");
        if (riHeader != null && !riHeader.isBlank()) {
            return riHeader;
        }
        return request.getRemoteAddr();
    }

    private String normalizePath(String path) {
        String normalized = path;
        normalized = normalized.replaceAll("/[a-zA-Z0-9_-]{16,32}/", "/{id}/");
        normalized = normalized.replaceAll("\\.[a-zA-Z]+$", "");
        return normalized;
    }

    private static class RateLimitBucket {
        private final int maxRequests;
        private final long windowMs;
        private long windowStart = System.currentTimeMillis();
        private int count = 0;

        RateLimitBucket() {
            this(MAX_REQUESTS, WINDOW_MS);
        }

        RateLimitBucket(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                windowStart = now;
                count = 0;
            }
            if (count < maxRequests) {
                count++;
                return true;
            }
            return false;
        }
    }
}
