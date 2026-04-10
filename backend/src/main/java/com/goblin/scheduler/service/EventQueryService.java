package com.goblin.scheduler.service;

import com.goblin.scheduler.dto.EventDetailResponse;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.EventStats;
import com.goblin.scheduler.model.FinalSelection;
import com.goblin.scheduler.repo.EventRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Event lookup hub and detail-view builder.
 *
 * <p>Phase 2.2 of the launch-readiness plan extracted this from the monolithic {@code
 * EventService}. It owns {@link #requireEvent} and {@link #requireEventByHostToken} so the other
 * four services in the split can share a single canonical not-found error. It also builds the
 * {@link EventDetailResponse} returned by {@code GET /events/{publicId}} and {@code GET
 * /host/{hostToken}}, which is why it injects the stats / final-selection / slot repositories.
 */
@Service
public class EventQueryService {

  private final EventRepository eventRepository;
  private final EventStatsRepository eventStatsRepository;
  private final FinalSelectionRepository finalSelectionRepository;
  private final SlotService slotService;

  public EventQueryService(
      EventRepository eventRepository,
      EventStatsRepository eventStatsRepository,
      FinalSelectionRepository finalSelectionRepository,
      SlotService slotService) {
    this.eventRepository = eventRepository;
    this.eventStatsRepository = eventStatsRepository;
    this.finalSelectionRepository = finalSelectionRepository;
    this.slotService = slotService;
  }

  public EventDetailResponse getEvent(String publicId) {
    Event event = requireEvent(publicId);
    return buildEventDetailResponse(event, true);
  }

  public EventDetailResponse getHostEvent(String hostToken) {
    Event event = requireEventByHostToken(hostToken);
    return buildEventDetailResponse(event, false);
  }

  /** Look up an event by its public ID or raise a 404. Shared by the other four split services. */
  public Event requireEvent(String publicId) {
    return eventRepository
        .findByPublicId(publicId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
  }

  /** Look up an event by its host token or raise a 404. Shared by ResultsService.getHostResults. */
  public Event requireEventByHostToken(String hostToken) {
    return eventRepository
        .findByHostToken(hostToken)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Host event not found"));
  }

  private EventDetailResponse buildEventDetailResponse(Event event, boolean trackView) {
    if (trackView) {
      eventStatsRepository.incrementView(event.id());
    }
    EventStats stats =
        eventStatsRepository.findByEventId(event.id()).orElse(new EventStats(event.id(), 0, 0));
    FinalSelection finalSelection = finalSelectionRepository.findByEventId(event.id()).orElse(null);
    List<Instant> candidateSlots = slotService.generateCandidateSlots(event);
    return new EventDetailResponse(
        event.publicId(),
        event.title(),
        event.description(),
        event.timezone(),
        event.slotMinutes(),
        event.durationMinutes(),
        event.startDate(),
        event.endDate(),
        event.dailyStartTime(),
        event.dailyEndTime(),
        event.resultsVisibility(),
        candidateSlots,
        new EventDetailResponse.StatsView(stats.viewCount(), stats.respondentCount()),
        finalSelection == null
            ? null
            : new EventDetailResponse.FinalView(
                finalSelection.slotStartUtc(), finalSelection.finalizedAt()));
  }
}
