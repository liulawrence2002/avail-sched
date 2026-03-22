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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EventService {
    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60, 90);
    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withLocale(Locale.US).withZone(ZoneId.of("UTC"));

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
        AppProperties appProperties
    ) {
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
        Event saved = eventRepository.save(new Event(
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
            Instant.now()
        ));
        eventStatsRepository.init(saved.id());
        return new CreateEventResponse(saved.publicId(), saved.hostToken(), appProperties.baseUrl() + "/host/" + saved.hostToken());
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
        Participant participant = participantRepository.findByTokenAndEventId(token, event.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
        List<AvailabilityItem> items = availabilityRepository.findByParticipantAndEventId(participant.id(), event.id()).stream()
            .map(record -> new AvailabilityItem(record.slotStartUtc(), record.weight()))
            .toList();
        return new ParticipantAvailabilityResponse(participant.displayName(), items);
    }

    public void updateAvailability(String publicId, String token, UpdateAvailabilityRequest request) {
        Event event = requireEvent(publicId);
        Participant participant = participantRepository.findByTokenAndEventId(token, event.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
        Set<Instant> validSlots = Set.copyOf(slotService.generateCandidateSlots(event));
        List<AvailabilityRecord> records = request.items().stream()
            .filter(item -> validSlots.contains(item.slotStartUtc()))
            .map(item -> new AvailabilityRecord(participant.id(), event.id(), item.slotStartUtc(), item.weight()))
            .toList();
        availabilityRepository.replaceForParticipant(event.id(), participant.id(), records);
        eventStatsRepository.setRespondentCount(event.id(), availabilityRepository.countParticipantsWithAvailability(event.id()));
        resultCache.evict(event.id());
    }

    public ResultsResponse getResults(String publicId) {
        Event event = requireEvent(publicId);
        ResultsResponse cached = resultCache.get(event.id());
        if (cached != null) {
            return cached;
        }
        ResultsResponse computed = computeResults(event);
        resultCache.put(event.id(), computed);
        return computed;
    }

    public FinalSelectionResponse finalizeEvent(String publicId, String hostToken, FinalizeRequest request) {
        Event event = requireEvent(publicId);
        if (!event.hostToken().equals(hostToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid host token");
        }
        if (finalSelectionRepository.findByEventId(event.id()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This event has already been finalized");
        }
        if (!slotService.generateCandidateSlots(event).contains(request.slotStartUtc())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot is not valid for this event");
        }
        FinalSelection selection = finalSelectionRepository.save(event.id(), request.slotStartUtc());
        return new FinalSelectionResponse(publicId, selection.slotStartUtc(), selection.finalizedAt());
    }

    public FinalSelectionResponse getFinalSelection(String publicId) {
        Event event = requireEvent(publicId);
        FinalSelection selection = finalSelectionRepository.findByEventId(event.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));
        return new FinalSelectionResponse(publicId, selection.slotStartUtc(), selection.finalizedAt());
    }

    public String getIcs(String publicId) {
        Event event = requireEvent(publicId);
        FinalSelection selection = finalSelectionRepository.findByEventId(event.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));
        Instant end = selection.slotStartUtc().plusSeconds(event.durationMinutes() * 60L);
        return """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Goblin Scheduler//EN
            BEGIN:VEVENT
            UID:%s@goblin-scheduler
            DTSTAMP:%s
            DTSTART:%s
            DTEND:%s
            SUMMARY:%s
            DESCRIPTION:%s
            END:VEVENT
            END:VCALENDAR
            """.formatted(
            event.publicId(),
            ICS_DATE.format(Instant.now()),
            ICS_DATE.format(selection.slotStartUtc()),
            ICS_DATE.format(end),
            sanitizeIcs(event.title()),
            sanitizeIcs(event.description() == null ? "Scheduled with Goblin Scheduler" : event.description())
        );
    }

    public Event requireEventByHostToken(String hostToken) {
        return eventRepository.findByHostToken(hostToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Host event not found"));
    }

    private EventDetailResponse buildEventDetailResponse(Event event, boolean trackView) {
        if (trackView) {
            eventStatsRepository.incrementView(event.id());
        }
        EventStats stats = eventStatsRepository.findByEventId(event.id()).orElse(new EventStats(event.id(), 0, 0));
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
            candidateSlots,
            new EventDetailResponse.StatsView(stats.viewCount(), stats.respondentCount()),
            finalSelection == null ? null : new EventDetailResponse.FinalView(finalSelection.slotStartUtc(), finalSelection.finalizedAt())
        );
    }

    public JoinParticipantResponse joinParticipant(String publicId, JoinParticipantRequest request) {
        Event event = requireEvent(publicId);
        Participant participant = participantRepository.save(new Participant(0L, event.id(), tokenGenerator.randomUrlToken(), TextSanitizer.sanitize(request.displayName()), Instant.now()));
        return new JoinParticipantResponse(participant.token());
    }

    private Event requireEvent(String publicId) {
        return eventRepository.findByPublicId(publicId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private ResultsResponse computeResults(Event event) {
        List<Instant> candidateSlots = slotService.generateCandidateSlots(event);
        List<Participant> participants = participantRepository.findByEventId(event.id());
        Map<Long, Map<Instant, Double>> availabilityMap = new HashMap<>();
        for (AvailabilityRecord record : availabilityRepository.findByEventId(event.id())) {
            availabilityMap.computeIfAbsent(record.participantId(), ignored -> new HashMap<>()).put(record.slotStartUtc(), record.weight());
        }

        return new ResultsResponse(event.publicId(), participants.size(), scoringService.scoreTopSlots(event, candidateSlots, participants, availabilityMap));
    }

    private void validateEventRequest(CreateEventRequest request) {
        if (!ALLOWED_DURATIONS.contains(request.durationMinutes())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be 30, 60, or 90 minutes");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }
        if (!request.dailyEndTime().isAfter(request.dailyStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily end time must be after daily start time");
        }
        if ((request.durationMinutes() % request.slotMinutes()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must align with slot size");
        }
        ZoneId.of(request.timezone());
    }

    private String sanitizeIcs(String value) {
        return value.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
