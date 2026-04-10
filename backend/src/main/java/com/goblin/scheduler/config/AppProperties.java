package com.goblin.scheduler.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, String baseUrl, RateLimit rateLimit) {

  public AppProperties {
    if (rateLimit == null) {
      rateLimit = new RateLimit(List.of());
    }
  }

  public record Cors(List<String> allowedOrigins) {}

  /**
   * Rate-limit tuning. {@code trustedProxies} is the set of exact IP literals whose {@code
   * X-Forwarded-For} header will be honored; any other client's XFF header is ignored, which
   * defeats the trivial "set XFF to a random IP to escape your own bucket" bypass.
   */
  public record RateLimit(List<String> trustedProxies) {
    public RateLimit {
      trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
    }
  }
}
