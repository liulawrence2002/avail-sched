package com.goblin.scheduler.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
  private static final int LIMIT = 120;
  private static final long WINDOW_SECONDS = 60;
  private static final int CLEANUP_INTERVAL = 100;
  private static final String XFF_HEADER = "X-Forwarded-For";

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final AtomicInteger requestsSinceCleanup = new AtomicInteger();
  private final Set<String> trustedProxies;

  public RateLimitFilter(List<String> trustedProxies) {
    this.trustedProxies = trustedProxies == null ? Set.of() : Set.copyOf(trustedProxies);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (requestsSinceCleanup.incrementAndGet() >= CLEANUP_INTERVAL) {
      cleanupExpiredBuckets();
    }

    String clientIp = resolveClientIp(request);
    String key = clientIp + ":" + normalizePath(request.getRequestURI());
    Bucket bucket =
        buckets.compute(
            key,
            (ignored, existing) -> {
              Instant now = Instant.now();
              if (existing == null
                  || now.isAfter(existing.windowStart.plusSeconds(WINDOW_SECONDS))) {
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

  /**
   * Resolve the client IP for bucket keying. If the immediate TCP peer ({@code getRemoteAddr}) is
   * one of the configured trusted proxies, trust the first entry of {@code X-Forwarded-For};
   * otherwise ignore XFF so an untrusted client cannot influence its own bucket by setting the
   * header to a random value. Visible for testing.
   */
  String resolveClientIp(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    if (!trustedProxies.contains(remoteAddr)) {
      return remoteAddr;
    }
    String xff = request.getHeader(XFF_HEADER);
    if (xff == null || xff.isBlank()) {
      return remoteAddr;
    }
    int comma = xff.indexOf(',');
    String first = (comma == -1 ? xff : xff.substring(0, comma)).trim();
    return first.isEmpty() ? remoteAddr : first;
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
