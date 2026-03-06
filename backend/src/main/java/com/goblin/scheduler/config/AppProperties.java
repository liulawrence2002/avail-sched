package com.goblin.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, String baseUrl) {
    public record Cors(List<String> allowedOrigins) {}
}

