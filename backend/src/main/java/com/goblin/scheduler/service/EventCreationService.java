package com.goblin.scheduler.service;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.config.SchedulingRules;
import com.goblin.scheduler.dto.CreateEventRequest;
import com.goblin.scheduler.dto.CreateEventResponse;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.repo.EventRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.util.TextSanitizer;
import com.goblin.scheduler.util.TokenGenerator;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Validates and persists new events. Pulled out of the old {@code EventService} in Phase 2.2. */
@Service
@Transactional
public class EventCreationService {

  private final EventRepository eventRepository;
  private final EventStatsRepository eventStatsRepository;
  private final TokenGenerator tokenGenerator;
  private final AppProperties appProperties;

  public EventCreationService(
      EventRepository eventRepository,
      EventStatsRepository eventStatsRepository,
      TokenGenerator tokenGenerator,
      AppProperties appProperties) {
    this.eventRepository = eventRepository;
    this.eventStatsRepository = eventStatsRepository;
    this.tokenGenerator = tokenGenerator;
    this.appProperties = appProperties;
  }

  public CreateEventResponse createEvent(CreateEventRequest request) {
    validateEventRequest(request);
    String resultsVisibility = SchedulingRules.normalizeVisibility(request.resultsVisibility());
    Event saved =
        eventRepository.save(
            new Event(
                0L,
                tokenGenerator.randomPublicId(),
                tokenGenerator.randomUrlToken(),
                TextSanitizer.sanitize(request.title()),
                TextSanitizer.sanitize(request.description()),
                request.timezone(),
                request.slotMinutes(),
                request.durationMinutes(),
                request.startDate(),
                request.endDate(),
                request.dailyStartTime(),
                request.dailyEndTime(),
                resultsVisibility,
                Instant.now()));
    eventStatsRepository.init(saved.id());
    return new CreateEventResponse(
        saved.publicId(),
        saved.hostToken(),
        appProperties.baseUrl() + "/host/" + saved.hostToken());
  }

  private void validateEventRequest(CreateEventRequest request) {
    if (!SchedulingRules.ALLOWED_DURATIONS.contains(request.durationMinutes())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Duration must be 30, 60, or 90 minutes");
    }
    if (request.endDate().isBefore(request.startDate())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "End date must be on or after start date");
    }
    if (!request.dailyEndTime().isAfter(request.dailyStartTime())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Daily end time must be after daily start time");
    }
    if ((request.durationMinutes() % request.slotMinutes()) != 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Duration must align with slot size");
    }
    String resultsVisibility = SchedulingRules.normalizeVisibility(request.resultsVisibility());
    if (!SchedulingRules.ALLOWED_VISIBILITIES.contains(resultsVisibility)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Results visibility is not supported");
    }
    try {
      ZoneId.of(request.timezone());
    } catch (DateTimeException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use a valid IANA timezone, like America/New_York");
    }
  }
}
