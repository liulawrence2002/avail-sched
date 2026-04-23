package com.goblinscheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClaudeConfig {

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    @Value("${gemini.max-tokens:1024}")
    private int maxTokens;

    @Bean
    public RestClient claudeRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public int getMaxTokens() { return maxTokens; }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
