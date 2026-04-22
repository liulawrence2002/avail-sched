package com.goblinscheduler.controller;

import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.UserToken;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.UserTokenRepository;
import com.goblinscheduler.service.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CalendarController {
    private final EventRepository eventRepository;
    private final UserTokenRepository userTokenRepository;
    private final CalendarService calendarService;

    public CalendarController(EventRepository eventRepository, UserTokenRepository userTokenRepository, CalendarService calendarService) {
        this.eventRepository = eventRepository;
        this.userTokenRepository = userTokenRepository;
        this.calendarService = calendarService;
    }

    @PostMapping("/api/events/{publicId}/calendar")
    public ResponseEntity<Map<String, String>> addToCalendar(
            @PathVariable String publicId,
            @RequestHeader("X-Host-Token") String hostToken,
            @RequestBody Map<String, String> body) {
        var eventOpt = eventRepository.findByPublicId(publicId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Event event = eventOpt.get();
        if (!hostToken.equals(event.getHostToken())) {
            return ResponseEntity.status(403).build();
        }
        if (!event.isFinalized()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Event is not finalized"));
        }

        String providerUserId = body.get("providerUserId");
        if (providerUserId == null || providerUserId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "providerUserId required"));
        }

        UserToken token = userTokenRepository.findByProviderUserId("google", providerUserId);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Google account not connected"));
        }

        try {
            String link = calendarService.createCalendarEvent(token, event);
            return ResponseEntity.ok(Map.of("calendarLink", link));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }
}
