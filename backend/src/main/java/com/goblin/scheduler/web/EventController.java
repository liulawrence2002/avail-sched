package com.goblin.scheduler.web;

import com.goblin.scheduler.dto.*;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events")
    public CreateEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping("/events/{publicId}")
    public EventDetailResponse getEvent(@PathVariable String publicId) {
        return eventService.getEvent(publicId);
    }

    @PostMapping("/events/{publicId}/participants")
    public JoinParticipantResponse join(@PathVariable String publicId, @Valid @RequestBody JoinParticipantRequest request) {
        return eventService.joinParticipant(publicId, request);
    }

    @PutMapping("/events/{publicId}/participants/{token}/availability")
    public ResponseEntity<Void> updateAvailability(@PathVariable String publicId, @PathVariable String token, @Valid @RequestBody UpdateAvailabilityRequest request) {
        eventService.updateAvailability(publicId, token, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{publicId}/results")
    public ResultsResponse getResults(@PathVariable String publicId) {
        return eventService.getResults(publicId);
    }

    @PostMapping("/events/{publicId}/finalize")
    public FinalSelectionResponse finalizeEvent(@PathVariable String publicId, @RequestParam String hostToken, @Valid @RequestBody FinalizeRequest request) {
        return eventService.finalizeEvent(publicId, hostToken, request);
    }

    @GetMapping("/events/{publicId}/final")
    public FinalSelectionResponse getFinal(@PathVariable String publicId) {
        return eventService.getFinalSelection(publicId);
    }

    @GetMapping("/events/{publicId}/final.ics")
    public ResponseEntity<String> downloadIcs(@PathVariable String publicId) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"goblin-scheduler.ics\"")
            .contentType(MediaType.parseMediaType("text/calendar"))
            .body(eventService.getIcs(publicId));
    }

    @GetMapping("/host/{hostToken}")
    public Event hostEvent(@PathVariable String hostToken) {
        return eventService.requireEventByHostToken(hostToken);
    }
}

