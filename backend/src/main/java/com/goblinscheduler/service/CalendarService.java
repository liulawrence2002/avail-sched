package com.goblinscheduler.service;

import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.UserToken;
import com.goblinscheduler.repository.UserTokenRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class CalendarService {
    private final UserTokenRepository userTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public CalendarService(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    public String createCalendarEvent(UserToken token, Event event) {
        Instant start = event.getFinalSlotStart();
        Instant end = start.plus(Duration.ofMinutes(event.getDurationMinutes()));

        Map<String, Object> body = Map.of(
            "summary", event.getTitle(),
            "description", event.getDescription() != null ? event.getDescription() : "Scheduled via Goblin Scheduler",
            "location", event.getLocation() != null ? event.getLocation() : "",
            "start", Map.of(
                "dateTime", start.toString(),
                "timeZone", event.getTimezone()
            ),
            "end", Map.of(
                "dateTime", end.toString(),
                "timeZone", event.getTimezone()
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://www.googleapis.com/calendar/v3/calendars/primary/events",
            request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("htmlLink");
        }
        throw new RuntimeException("Failed to create calendar event: " + response.getStatusCode());
    }
}
