package com.goblinscheduler.controller;

import com.goblinscheduler.model.UserToken;
import com.goblinscheduler.repository.UserTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;

@RestController
public class GoogleOAuthController {
    private final UserTokenRepository userTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${GOOGLE_CLIENT_ID:}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${goblin.app-url:http://localhost:3001}")
    private String appUrl;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";

    public GoogleOAuthController(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    @GetMapping("/api/oauth/google/url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("error", "Google OAuth not configured"));
        }
        String redirectUri = appUrl.replaceAll("/$", "") + "/api/oauth/google/callback";
        String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", SCOPE)
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .build().toUriString();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/api/oauth/google/callback")
    public ResponseEntity<Void> callback(@RequestParam String code) {
        String redirectUri = appUrl.replaceAll("/$", "") + "/api/oauth/google/callback";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "code=" + code
            + "&client_id=" + clientId
            + "&client_secret=" + clientSecret
            + "&redirect_uri=" + redirectUri
            + "&grant_type=authorization_code";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> data = response.getBody();
            String accessToken = (String) data.get("access_token");
            String refreshToken = (String) data.get("refresh_token");
            Integer expiresIn = (Integer) data.get("expires_in");

            // Get user info to identify the token
            HttpHeaders infoHeaders = new HttpHeaders();
            infoHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> infoRequest = new HttpEntity<>(infoHeaders);
            ResponseEntity<Map> infoResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET, infoRequest, Map.class);

            String providerUserId = "unknown";
            if (infoResponse.getStatusCode().is2xxSuccessful() && infoResponse.getBody() != null) {
                providerUserId = (String) infoResponse.getBody().get("id");
            }

            Instant expiresAt = expiresIn != null ? Instant.now().plusSeconds(expiresIn) : null;

            UserToken existing = userTokenRepository.findByProviderUserId("google", providerUserId);
            if (existing != null) {
                userTokenRepository.updateTokens("google", providerUserId, accessToken,
                    refreshToken != null ? refreshToken : existing.getRefreshToken(), expiresAt);
            } else {
                UserToken token = new UserToken();
                token.setProvider("google");
                token.setProviderUserId(providerUserId);
                token.setAccessToken(accessToken);
                token.setRefreshToken(refreshToken);
                token.setExpiresAt(expiresAt);
                userTokenRepository.save(token);
            }

            return ResponseEntity.status(302)
                .header("Location", appUrl + "/settings?google=connected&userId=" + providerUserId)
                .build();
        }

        return ResponseEntity.status(302)
            .header("Location", appUrl + "/settings?google=error")
            .build();
    }
}
