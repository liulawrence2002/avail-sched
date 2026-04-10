package com.goblin.scheduler.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, String baseUrl) {
  public record Cors(List<String> allowedOrigins) {}
}
