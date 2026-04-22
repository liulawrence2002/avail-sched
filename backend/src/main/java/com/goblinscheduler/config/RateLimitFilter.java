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
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String normalizedPath = normalizePath(httpRequest.getRequestURI());
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
        private long windowStart = System.currentTimeMillis();
        private int count = 0;

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                windowStart = now;
                count = 0;
            }
            if (count < MAX_REQUESTS) {
                count++;
                return true;
            }
            return false;
        }
    }
}
