package com.goblin.scheduler.web;

import com.goblin.scheduler.dto.*;
import com.goblin.scheduler.service.EventCreationService;
import com.goblin.scheduler.service.EventQueryService;
import com.goblin.scheduler.service.FinalizationService;
import com.goblin.scheduler.service.ParticipantService;
import com.goblin.scheduler.service.ResultsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@Tag(name = "Events", description = "Event lifecycle: create, join, vote, finalize")
public class EventController {
  private static final Logger log = LoggerFactory.getLogger(EventController.class);
  private static final String HOST_TOKEN_HEADER = "X-Host-Token";

  private final EventCreationService eventCreationService;
  private final EventQueryService eventQueryService;
  private final ParticipantService participantService;
  private final ResultsService resultsService;
  private final FinalizationService finalizationService;

  public EventController(
      EventCreationService eventCreationService,
      EventQueryService eventQueryService,
      ParticipantService participantService,
      ResultsService resultsService,
      FinalizationService finalizationService) {
    this.eventCreationService = eventCreationService;
    this.eventQueryService = eventQueryService;
    this.participantService = participantService;
    this.resultsService = resultsService;
    this.finalizationService = finalizationService;
  }

  @Operation(summary = "Create a new scheduling event")
  @PostMapping("/events")
  public CreateEventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
    return eventCreationService.createEvent(request);
  }

  @Operation(summary = "Get event details by public ID")
  @GetMapping("/events/{publicId}")
  public EventDetailResponse getEvent(@PathVariable String publicId) {
    return eventQueryService.getEvent(publicId);
  }

  @Operation(summary = "Join an event as a participant")
  @PostMapping("/events/{publicId}/participants")
  public JoinParticipantResponse join(
      @PathVariable String publicId, @Valid @RequestBody JoinParticipantRequest request) {
    return participantService.joinParticipant(publicId, request);
  }

  @Operation(summary = "Save participant availability for an event")
  @PutMapping("/events/{publicId}/participants/{token}/availability")
  public ResponseEntity<Void> updateAvailability(
      @PathVariable String publicId,
      @PathVariable String token,
      @Valid @RequestBody UpdateAvailabilityRequest request) {
    participantService.updateAvailability(publicId, token, request);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Get saved participant availability for an event")
  @GetMapping("/events/{publicId}/participants/{token}/availability")
  public ParticipantAvailabilityResponse getParticipantAvailability(
      @PathVariable String publicId, @PathVariable String token) {
    return participantService.getParticipantAvailability(publicId, token);
  }

  @Operation(summary = "Get scored availability results")
  @GetMapping("/events/{publicId}/results")
  public ResultsResponse getResults(@PathVariable String publicId) {
    return resultsService.getResults(publicId);
  }

  @Operation(summary = "Get scored availability results with participant details by host token")
  @GetMapping("/host/{hostToken}/results")
  public ResultsResponse getHostResults(@PathVariable String hostToken) {
    return resultsService.getHostResults(hostToken);
  }

  @Operation(
      summary = "Finalize a time slot for the event",
      description =
          "Pass the host token via the X-Host-Token header. The hostToken query parameter is"
              + " accepted for one release to give callers time to migrate and is logged as"
              + " deprecated.")
  @PostMapping("/events/{publicId}/finalize")
  public FinalSelectionResponse finalizeEvent(
      @PathVariable String publicId,
      @RequestHeader(name = HOST_TOKEN_HEADER, required = false) String headerToken,
      @RequestParam(name = "hostToken", required = false) String queryToken,
      @Valid @RequestBody FinalizeRequest request) {
    String hostToken = resolveHostToken(headerToken, queryToken);
    return finalizationService.finalizeEvent(publicId, hostToken, request);
  }

  @Operation(summary = "Get the finalized time slot")
  @GetMapping("/events/{publicId}/final")
  public FinalSelectionResponse getFinal(@PathVariable String publicId) {
    return finalizationService.getFinalSelection(publicId);
  }

  @Operation(summary = "Download ICS calendar file for finalized event")
  @GetMapping("/events/{publicId}/final.ics")
  public ResponseEntity<String> downloadIcs(@PathVariable String publicId) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"goblin-scheduler.ics\"")
        .contentType(MediaType.parseMediaType("text/calendar"))
        .body(finalizationService.getIcs(publicId));
  }

  @Operation(summary = "Get event details by host token")
  @GetMapping("/host/{hostToken}")
  public EventDetailResponse hostEvent(@PathVariable String hostToken) {
    return eventQueryService.getHostEvent(hostToken);
  }

  private String resolveHostToken(String headerToken, String queryToken) {
    if (headerToken != null && !headerToken.isBlank()) {
      return headerToken;
    }
    if (queryToken != null && !queryToken.isBlank()) {
      log.warn(
          "Host token accepted via deprecated query string; migrate callers to the {} header",
          HOST_TOKEN_HEADER);
      return queryToken;
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Missing host token; send the " + HOST_TOKEN_HEADER + " header");
  }
}
