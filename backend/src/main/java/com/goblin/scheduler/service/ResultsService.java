package com.goblin.scheduler.service;

import com.goblin.scheduler.dto.ResultsResponse;
import com.goblin.scheduler.model.AvailabilityRecord;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.EventStats;
import com.goblin.scheduler.model.FinalSelection;
import com.goblin.scheduler.model.Participant;
import com.goblin.scheduler.repo.AvailabilityRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import com.goblin.scheduler.repo.ParticipantRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Scoring + host-view results. Pulled out of the old {@code EventService} in Phase 2.2.
 *
 * <p>The {@code aggregate_public} and {@code host_only} visibility paths share an underlying
 * computation that differs only in whether participant display names are included in the top slot
 * views. Both paths go through the shared {@link ResultCache} (keyed on event id + detail-inclusion
 * flag) so the host and public views can coexist without cache interference.
 */
@Service
public class ResultsService {

  private final EventQueryService eventQueryService;
  private final ParticipantRepository participantRepository;
  private final AvailabilityRepository availabilityRepository;
  private final EventStatsRepository eventStatsRepository;
  private final FinalSelectionRepository finalSelectionRepository;
  private final SlotService slotService;
  private final ScoringService scoringService;
  private final ResultCache resultCache;

  public ResultsService(
      EventQueryService eventQueryService,
      ParticipantRepository participantRepository,
      AvailabilityRepository availabilityRepository,
      EventStatsRepository eventStatsRepository,
      FinalSelectionRepository finalSelectionRepository,
      SlotService slotService,
      ScoringService scoringService,
      ResultCache resultCache) {
    this.eventQueryService = eventQueryService;
    this.participantRepository = participantRepository;
    this.availabilityRepository = availabilityRepository;
    this.eventStatsRepository = eventStatsRepository;
    this.finalSelectionRepository = finalSelectionRepository;
    this.slotService = slotService;
    this.scoringService = scoringService;
    this.resultCache = resultCache;
  }

  public ResultsResponse getResults(String publicId) {
    Event event = eventQueryService.requireEvent(publicId);
    if ("host_only".equals(event.resultsVisibility())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Results are only available to the host for this event");
    }
    ResultsResponse cached = resultCache.get(event.id(), false);
    if (cached != null) {
      return cached;
    }
    ResultsResponse computed = computeResults(event, false);
    resultCache.put(event.id(), false, computed);
    return computed;
  }

  public ResultsResponse getHostResults(String hostToken) {
    Event event = eventQueryService.requireEventByHostToken(hostToken);
    ResultsResponse cached = resultCache.get(event.id(), true);
    if (cached != null) {
      return cached;
    }
    ResultsResponse computed = computeResults(event, true);
    resultCache.put(event.id(), true, computed);
    return computed;
  }

  private ResultsResponse computeResults(Event event, boolean includeParticipantDetails) {
    List<Instant> candidateSlots = slotService.generateCandidateSlots(event);
    List<Participant> participants = participantRepository.findByEventId(event.id());
    Map<Long, Map<Instant, Double>> availabilityMap = new HashMap<>();
    for (AvailabilityRecord record : availabilityRepository.findByEventId(event.id())) {
      availabilityMap
          .computeIfAbsent(record.participantId(), ignored -> new HashMap<>())
          .put(record.slotStartUtc(), record.weight());
    }
    EventStats stats =
        eventStatsRepository.findByEventId(event.id()).orElse(new EventStats(event.id(), 0, 0));
    FinalSelection finalSelection = finalSelectionRepository.findByEventId(event.id()).orElse(null);

    return new ResultsResponse(
        event.publicId(),
        event.timezone(),
        participants.size(),
        stats.respondentCount(),
        finalSelection == null
            ? null
            : new ResultsResponse.FinalView(
                finalSelection.slotStartUtc(), finalSelection.finalizedAt()),
        includeParticipantDetails,
        scoringService.scoreTopSlots(
            event, candidateSlots, participants, availabilityMap, includeParticipantDetails));
  }
}
