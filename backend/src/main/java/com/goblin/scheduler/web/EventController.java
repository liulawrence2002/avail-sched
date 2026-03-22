package com.goblin.scheduler.web;

import com.goblin.scheduler.dto.*;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Events", description = "Event lifecycle: create, join, vote, finalize")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Create a new scheduling event")
    @PostMapping("/events")
    public CreateEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @Operation(summary = "Get event details by public ID")
    @GetMapping("/events/{publicId}")
    public EventDetailResponse getEvent(@PathVariable String publicId) {
        return eventService.getEvent(publicId);
    }

    @Operation(summary = "Join an event as a participant")
    @PostMapping("/events/{publicId}/participants")
    public JoinParticipantResponse join(@PathVariable String publicId, @Valid @RequestBody JoinParticipantRequest request) {
        return eventService.joinParticipant(publicId, request);
    }

    @Operation(summary = "Save participant availability for an event")
    @PutMapping("/events/{publicId}/participants/{token}/availability")
    public ResponseEntity<Void> updateAvailability(@PathVariable String publicId, @PathVariable String token, @Valid @RequestBody UpdateAvailabilityRequest request) {
        eventService.updateAvailability(publicId, token, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get saved participant availability for an event")
    @GetMapping("/events/{publicId}/participants/{token}/availability")
    public ParticipantAvailabilityResponse getParticipantAvailability(@PathVariable String publicId, @PathVariable String token) {
        return eventService.getParticipantAvailability(publicId, token);
    }

    @Operation(summary = "Get scored availability results")
    @GetMapping("/events/{publicId}/results")
    public ResultsResponse getResults(@PathVariable String publicId) {
        return eventService.getResults(publicId);
    }

    @Operation(summary = "Finalize a time slot for the event")
    @PostMapping("/events/{publicId}/finalize")
    public FinalSelectionResponse finalizeEvent(@PathVariable String publicId, @RequestParam String hostToken, @Valid @RequestBody FinalizeRequest request) {
        return eventService.finalizeEvent(publicId, hostToken, request);
    }

    @Operation(summary = "Get the finalized time slot")
    @GetMapping("/events/{publicId}/final")
    public FinalSelectionResponse getFinal(@PathVariable String publicId) {
        return eventService.getFinalSelection(publicId);
    }

    @Operation(summary = "Download ICS calendar file for finalized event")
    @GetMapping("/events/{publicId}/final.ics")
    public ResponseEntity<String> downloadIcs(@PathVariable String publicId) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"goblin-scheduler.ics\"")
            .contentType(MediaType.parseMediaType("text/calendar"))
            .body(eventService.getIcs(publicId));
    }

    @Operation(summary = "Get event details by host token")
    @GetMapping("/host/{hostToken}")
    public EventDetailResponse hostEvent(@PathVariable String hostToken) {
        return eventService.getHostEvent(hostToken);
    }
}
