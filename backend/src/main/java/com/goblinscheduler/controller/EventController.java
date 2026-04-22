package com.goblinscheduler.controller;

import com.goblinscheduler.dto.*;
import com.goblinscheduler.exception.*;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.service.AvailabilityService;
import com.goblinscheduler.service.EventService;
import com.goblinscheduler.service.ResultsService;
import com.goblinscheduler.util.ICSGenerator;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventController {

    private final EventService eventService;
    private final AvailabilityService availabilityService;
    private final ResultsService resultsService;

    public EventController(EventService eventService,
                           AvailabilityService availabilityService,
                           ResultsService resultsService) {
        this.eventService = eventService;
        this.availabilityService = availabilityService;
        this.resultsService = resultsService;
    }

    @PostMapping("/api/events")
    public ResponseEntity<CreateEventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(request));
    }

    @GetMapping("/api/events/{publicId}")
    public ResponseEntity<EventDetailResponse> getEventDetail(@PathVariable String publicId) {
        return ResponseEntity.ok(eventService.getEventDetail(publicId));
    }

    @PostMapping("/api/events/{publicId}/participants")
    public ResponseEntity<JoinParticipantResponse> joinParticipant(
            @PathVariable String publicId,
            @Valid @RequestBody JoinParticipantRequest request) {
        return ResponseEntity.ok(availabilityService.joinParticipant(publicId, request));
    }

    @GetMapping("/api/events/{publicId}/participants/{token}/availability")
    public ResponseEntity<ParticipantAvailabilityResponse> getParticipantAvailability(
            @PathVariable String publicId,
            @PathVariable String token) {
        return ResponseEntity.ok(availabilityService.getParticipantAvailability(publicId, token));
    }

    @PutMapping("/api/events/{publicId}/participants/{token}/availability")
    public ResponseEntity<Void> putParticipantAvailability(
            @PathVariable String publicId,
            @PathVariable String token,
            @Valid @RequestBody PutAvailabilityRequest request) {
        availabilityService.putParticipantAvailability(publicId, token, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/events/{publicId}/results")
    public ResponseEntity<ResultsResponse> getPublicResults(@PathVariable String publicId) {
        return ResponseEntity.ok(resultsService.getPublicResults(publicId));
    }

    @GetMapping("/api/host/{hostToken}/results")
    public ResponseEntity<ResultsResponse> getHostResults(@PathVariable String hostToken) {
        return ResponseEntity.ok(resultsService.getHostResults(hostToken));
    }

    @PostMapping("/api/events/{publicId}/finalize")
    public ResponseEntity<Void> finalizeEvent(
            @PathVariable String publicId,
            @RequestHeader("X-Host-Token") String hostToken,
            @RequestBody FinalizeEventRequest request) {
        eventService.finalizeEvent(publicId, hostToken, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/events/{publicId}/final")
    public ResponseEntity<EventDetailResponse> getFinalSelection(@PathVariable String publicId) {
        return ResponseEntity.ok(eventService.getFinalSelection(publicId));
    }

    @GetMapping("/api/events/{publicId}/final.ics")
    public ResponseEntity<String> downloadIcs(@PathVariable String publicId) {
        Event event = eventService.getEventByPublicId(publicId);
        if (!event.isFinalized()) {
            throw new NotFoundException("Event not finalized");
        }
        String ics = ICSGenerator.generate(event);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8");
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event.ics");
        return new ResponseEntity<>(ics, headers, HttpStatus.OK);
    }
}
