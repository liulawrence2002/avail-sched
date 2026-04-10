package com.goblin.scheduler.service;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.dto.*;
import com.goblin.scheduler.model.AvailabilityRecord;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.EventStats;
import com.goblin.scheduler.model.FinalSelection;
import com.goblin.scheduler.model.Participant;
import com.goblin.scheduler.repo.AvailabilityRepository;
import com.goblin.scheduler.repo.EventRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import com.goblin.scheduler.repo.ParticipantRepository;
import com.goblin.scheduler.util.TextSanitizer;
import com.goblin.scheduler.util.TokenGenerator;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {
  private static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60, 90);
  private static final Set<String> ALLOWED_RESULTS_VISIBILITIES =
      Set.of("aggregate_public", "host_only");
  private static final DateTimeFormatter ICS_DATE =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
          .withLocale(Locale.US)
          .withZone(ZoneId.of("UTC"));

  private final EventRepository eventRepository;
  private final ParticipantRepository participantRepository;
  private final AvailabilityRepository availabilityRepository;
  private final FinalSelectionRepository finalSelectionRepository;
  private final EventStatsRepository eventStatsRepository;
  private final SlotService slotService;
  private final ScoringService scoringService;
  private final TokenGenerator tokenGenerator;
  private final ResultCache resultCache;
  private final AppProperties appProperties;

  public EventService(
      EventRepository eventRepository,
      ParticipantRepository participantRepository,
      AvailabilityRepository availabilityRepository,
      FinalSelectionRepository finalSelectionRepository,
      EventStatsRepository eventStatsRepository,
      SlotService slotService,
      ScoringService scoringService,
      TokenGenerator tokenGenerator,
      ResultCache resultCache,
      AppProperties appProperties) {
    this.eventRepository = eventRepository;
    this.participantRepository = participantRepository;
    this.availabilityRepository = availabilityRepository;
    this.finalSelectionRepository = finalSelectionRepository;
    this.eventStatsRepository = eventStatsRepository;
    this.slotService = slotService;
    this.scoringService = scoringService;
    this.tokenGenerator = tokenGenerator;
    this.resultCache = resultCache;
    this.appProperties = appProperties;
  }

  public CreateEventResponse createEvent(CreateEventRequest request) {
    validateEventRequest(request);
    String resultsVisibility = normalizeResultsVisibility(request.resultsVisibility());
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

  public EventDetailResponse getEvent(String publicId) {
    Event event = requireEvent(publicId);
    return buildEventDetailResponse(event, true);
  }

  public EventDetailResponse getHostEvent(String hostToken) {
    Event event = requireEventByHostToken(hostToken);
    return buildEventDetailResponse(event, false);
  }

  public ParticipantAvailabilityResponse getParticipantAvailability(String publicId, String token) {
    Event event = requireEvent(publicId);
    Participant participant =
        participantRepository
            .findByTokenAndEventId(token, event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
    List<AvailabilityItem> items =
        availabilityRepository.findByParticipantAndEventId(participant.id(), event.id()).stream()
            .map(record -> new AvailabilityItem(record.slotStartUtc(), record.weight()))
            .toList();
    return new ParticipantAvailabilityResponse(
        participant.displayName(), participant.email(), items);
  }

  public void updateAvailability(String publicId, String token, UpdateAvailabilityRequest request) {
    Event event = requireEvent(publicId);
    ensureVotingOpen(event.id());
    Participant participant =
        participantRepository
            .findByTokenAndEventId(token, event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
    Set<Instant> validSlots = Set.copyOf(slotService.generateCandidateSlots(event));
    List<AvailabilityRecord> records =
        request.items().stream()
            .filter(item -> validSlots.contains(item.slotStartUtc()))
            .map(
                item ->
                    new AvailabilityRecord(
                        participant.id(), event.id(), item.slotStartUtc(), item.weight()))
            .toList();
    availabilityRepository.replaceForParticipant(event.id(), participant.id(), records);
    eventStatsRepository.setRespondentCount(
        event.id(), availabilityRepository.countParticipantsWithAvailability(event.id()));
    resultCache.evict(event.id());
  }

  public ResultsResponse getResults(String publicId) {
    Event event = requireEvent(publicId);
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
    Event event = requireEventByHostToken(hostToken);
    ResultsResponse cached = resultCache.get(event.id(), true);
    if (cached != null) {
      return cached;
    }
    ResultsResponse computed = computeResults(event, true);
    resultCache.put(event.id(), true, computed);
    return computed;
  }

  public FinalSelectionResponse finalizeEvent(
      String publicId, String hostToken, FinalizeRequest request) {
    Event event = requireEvent(publicId);
    if (!event.hostToken().equals(hostToken)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid host token");
    }
    if (finalSelectionRepository.findByEventId(event.id()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This event has already been finalized");
    }
    if (!slotService.generateCandidateSlots(event).contains(request.slotStartUtc())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot is not valid for this event");
    }
    FinalSelection selection = finalSelectionRepository.save(event.id(), request.slotStartUtc());
    resultCache.evict(event.id());
    return new FinalSelectionResponse(publicId, selection.slotStartUtc(), selection.finalizedAt());
  }

  public FinalSelectionResponse getFinalSelection(String publicId) {
    Event event = requireEvent(publicId);
    FinalSelection selection =
        finalSelectionRepository
            .findByEventId(event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));
    return new FinalSelectionResponse(publicId, selection.slotStartUtc(), selection.finalizedAt());
  }

  public String getIcs(String publicId) {
    Event event = requireEvent(publicId);
    FinalSelection selection =
        finalSelectionRepository
            .findByEventId(event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));
    Instant end = selection.slotStartUtc().plusSeconds(event.durationMinutes() * 60L);
    return String.join(
        "\r\n",
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Goblin Scheduler//EN",
        "BEGIN:VEVENT",
        "UID:%s@goblin-scheduler".formatted(event.publicId()),
        "DTSTAMP:%s".formatted(ICS_DATE.format(Instant.now())),
        "DTSTART:%s".formatted(ICS_DATE.format(selection.slotStartUtc())),
        "DTEND:%s".formatted(ICS_DATE.format(end)),
        "SUMMARY:%s".formatted(sanitizeIcs(event.title())),
        "DESCRIPTION:%s"
            .formatted(
                sanitizeIcs(
                    event.description() == null
                        ? "Scheduled with Goblin Scheduler"
                        : event.description())),
        "END:VEVENT",
        "END:VCALENDAR",
        "");
  }

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

  public JoinParticipantResponse joinParticipant(String publicId, JoinParticipantRequest request) {
    Event event = requireEvent(publicId);
    ensureVotingOpen(event.id());

    String displayName = TextSanitizer.sanitize(request.displayName());
    String email = normalizeEmail(request.email());
    if (email != null) {
      Participant existing =
          participantRepository.findByEmailAndEventId(email, event.id()).orElse(null);
      if (existing != null) {
        if (!existing.displayName().equals(displayName) || !email.equals(existing.email())) {
          participantRepository.updateIdentity(existing.id(), displayName, email);
        }
        return new JoinParticipantResponse(
            existing.token(), participantLink(event.publicId(), existing.token()), true);
      }
    }

    Participant participant =
        participantRepository.save(
            new Participant(
                0L,
                event.id(),
                tokenGenerator.randomUrlToken(),
                displayName,
                email,
                Instant.now()));
    return new JoinParticipantResponse(
        participant.token(), participantLink(event.publicId(), participant.token()), false);
  }

  private Event requireEvent(String publicId) {
    return eventRepository
        .findByPublicId(publicId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
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

  private void validateEventRequest(CreateEventRequest request) {
    if (!ALLOWED_DURATIONS.contains(request.durationMinutes())) {
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
    String resultsVisibility = normalizeResultsVisibility(request.resultsVisibility());
    if (!ALLOWED_RESULTS_VISIBILITIES.contains(resultsVisibility)) {
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

  private String sanitizeIcs(String value) {
    String normalized = value.replace("\r\n", "\n").replace("\r", "\n");
    return normalized
        .replace("\\", "\\\\")
        .replace(",", "\\,")
        .replace(";", "\\;")
        .replace("\n", "\\n");
  }

  private String normalizeResultsVisibility(String requestedVisibility) {
    if (requestedVisibility == null || requestedVisibility.isBlank()) {
      return "aggregate_public";
    }
    return requestedVisibility.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeEmail(String requestedEmail) {
    if (requestedEmail == null || requestedEmail.isBlank()) {
      return null;
    }
    return requestedEmail.trim().toLowerCase(Locale.ROOT);
  }

  private String participantLink(String publicId, String token) {
    return appProperties.baseUrl() + "/e/" + publicId + "?token=" + token;
  }

  private void ensureVotingOpen(long eventId) {
    if (finalSelectionRepository.findByEventId(eventId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting is closed for this event");
    }
  }
}
