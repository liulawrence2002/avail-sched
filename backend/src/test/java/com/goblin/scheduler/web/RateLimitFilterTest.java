package com.goblin.scheduler.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private static final String CLIENT_IP = "203.0.113.45";
  private static final String TRUSTED_PROXY_IP = "10.0.0.1";
  private static final String ANOTHER_TRUSTED_PROXY_IP = "10.0.0.2";

  private MockHttpServletRequest requestFrom(String remoteAddr, String forwardedFor, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRemoteAddr(remoteAddr);
    if (forwardedFor != null) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }
    return request;
  }

  @Test
  void resolveClientIp_untrustedCaller_ignoresXForwardedFor() {
    RateLimitFilter filter = new RateLimitFilter(List.of(TRUSTED_PROXY_IP));
    HttpServletRequest request = requestFrom(CLIENT_IP, "9.9.9.9", "/api/events");

    assertEquals(CLIENT_IP, filter.resolveClientIp(request));
  }

  @Test
  void resolveClientIp_noTrustedProxies_alwaysUsesRemoteAddr() {
    RateLimitFilter filter = new RateLimitFilter(List.of());
    HttpServletRequest request = requestFrom(CLIENT_IP, "9.9.9.9", "/api/events");

    assertEquals(CLIENT_IP, filter.resolveClientIp(request));
  }

  @Test
  void resolveClientIp_trustedProxy_honorsFirstXffEntry() {
    RateLimitFilter filter = new RateLimitFilter(List.of(TRUSTED_PROXY_IP));
    HttpServletRequest request =
        requestFrom(TRUSTED_PROXY_IP, "203.0.113.7, 10.0.0.1", "/api/events");

    assertEquals("203.0.113.7", filter.resolveClientIp(request));
  }

  @Test
  void resolveClientIp_trustedProxyWithBlankXff_fallsBackToRemoteAddr() {
    RateLimitFilter filter = new RateLimitFilter(List.of(TRUSTED_PROXY_IP));
    HttpServletRequest request = requestFrom(TRUSTED_PROXY_IP, "", "/api/events");

    assertEquals(TRUSTED_PROXY_IP, filter.resolveClientIp(request));
  }

  @Test
  void resolveClientIp_multipleTrustedProxiesConfigured_eachHonored() {
    RateLimitFilter filter =
        new RateLimitFilter(List.of(TRUSTED_PROXY_IP, ANOTHER_TRUSTED_PROXY_IP));
    HttpServletRequest req1 = requestFrom(TRUSTED_PROXY_IP, "198.51.100.1", "/api/events");
    HttpServletRequest req2 = requestFrom(ANOTHER_TRUSTED_PROXY_IP, "198.51.100.2", "/api/events");

    assertEquals("198.51.100.1", filter.resolveClientIp(req1));
    assertEquals("198.51.100.2", filter.resolveClientIp(req2));
  }

  @Test
  void spoofedXffFromUntrustedClient_sharesBucketWithRealIp() throws Exception {
    // Anti-spoofing regression: the same untrusted client that issues 121 requests using
    // two different spoofed XFF values still shares a single bucket keyed on its real IP,
    // so the 121st request trips the limiter.
    RateLimitFilter filter = new RateLimitFilter(List.of(TRUSTED_PROXY_IP));
    FilterChain chain = Mockito.mock(FilterChain.class);

    for (int i = 0; i < 120; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      HttpServletRequest request =
          requestFrom(CLIENT_IP, (i % 2 == 0 ? "9.9.9.9" : "8.8.8.8"), "/api/events");
      filter.doFilter(request, response, chain);
      assertEquals(200, response.getStatus(), "request " + i + " should be allowed");
    }

    MockHttpServletResponse tripResponse = new MockHttpServletResponse();
    HttpServletRequest tripRequest = requestFrom(CLIENT_IP, "1.2.3.4", "/api/events");
    filter.doFilter(tripRequest, tripResponse, chain);
    assertEquals(429, tripResponse.getStatus());
  }
}
