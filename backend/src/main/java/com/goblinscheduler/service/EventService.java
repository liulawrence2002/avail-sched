package com.goblinscheduler.service;

import com.goblinscheduler.dto.*;
import com.goblinscheduler.exception.NotFoundException;
import com.goblinscheduler.exception.ValidationException;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import com.goblinscheduler.util.SlotGenerator;
import com.goblinscheduler.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EmailService emailService;
    private final String appUrl;

    private static final Set<Integer> VALID_DURATIONS = Set.of(30, 60, 90);
    private static final Set<String> VALID_VISIBILITIES = Set.of("aggregate_public", "host_only");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public EventService(EventRepository eventRepository,
                        ParticipantRepository participantRepository,
                        EmailService emailService,
                        @Value("${goblin.app-url}") String appUrl) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.emailService = emailService;
        this.appUrl = appUrl;
    }

    @Transactional
    public CreateEventResponse createEvent(CreateEventRequest request) {
        validateCreateRequest(request);

        Event event = new Event();
        event.setPublicId(TokenGenerator.generatePublicId());
        event.setHostToken(TokenGenerator.generateHostToken());
        event.setTitle(request.title().trim());
        event.setDescription(request.description() != null ? request.description().trim() : null);
        event.setTimezone(request.timezone());
        event.setSlotMinutes(request.slotMinutes());
        event.setDurationMinutes(request.durationMinutes());
        event.setStartDate(LocalDate.parse(request.startDate(), DATE_FORMATTER));
        event.setEndDate(LocalDate.parse(request.endDate(), DATE_FORMATTER));
        event.setDailyStartTime(LocalTime.parse(request.dailyStartTime(), TIME_FORMATTER));
        event.setDailyEndTime(LocalTime.parse(request.dailyEndTime(), TIME_FORMATTER));
        event.setLocation(request.location() != null ? request.location().trim() : null);
        event.setMeetingUrl(request.meetingUrl() != null ? request.meetingUrl().trim() : null);
        event.setResultsVisibility(request.resultsVisibility() != null ? request.resultsVisibility() : "aggregate_public");
        event.setViewCount(0);
        event.setRespondentCount(0);
        event.setCreatedAt(Instant.now());

        if (request.deadline() != null && !request.deadline().isBlank()) {
            event.setDeadline(Instant.parse(request.deadline()));
        }
        event.setAutoFinalize(request.autoFinalize() != null ? request.autoFinalize() : false);

        eventRepository.save(event);

        return new CreateEventResponse(
                event.getPublicId(),
                event.getHostToken(),
                appUrl + "/host/" + event.getHostToken()
        );
    }

    @Transactional
    public EventDetailResponse getEventDetail(String publicId) {
        Event event = eventRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        eventRepository.incrementViewCount(publicId);

        List<String> candidateSlotsUtc = SlotGenerator.generateSlotsUtc(
                event.getStartDate(),
                event.getEndDate(),
                event.getDailyStartTime(),
                event.getDailyEndTime(),
                event.getSlotMinutes(),
                event.getTimezone()
        ).stream().map(java.time.Instant::toString).toList();

        StatsDto stats = new StatsDto(event.getViewCount() + 1, event.getRespondentCount());

        FinalSelectionDto finalSelection = null;
        if (event.getFinalSlotStart() != null) {
            finalSelection = new FinalSelectionDto(
                    event.getFinalSlotStart().toString(),
                    event.getFinalizedAt() != null ? event.getFinalizedAt().toString() : null
            );
        }

        return new EventDetailResponse(
                event.getPublicId(),
                event.getTitle(),
                event.getDescription(),
                event.getTimezone(),
                event.getSlotMinutes(),
                event.getDurationMinutes(),
                event.getStartDate().toString(),
                event.getEndDate().toString(),
                event.getDailyStartTime().toString(),
                event.getDailyEndTime().toString(),
                event.getLocation(),
                event.getMeetingUrl(),
                event.getResultsVisibility(),
                candidateSlotsUtc,
                stats,
                finalSelection
        );
    }

    @Transactional
    public void finalizeEvent(String publicId, String hostToken, FinalizeEventRequest request) {
        Event event = eventRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (!event.getHostToken().equals(hostToken)) {
            throw new ValidationException("Invalid host token");
        }

        if (event.isFinalized()) {
            throw new com.goblinscheduler.exception.ConflictException("Event already finalized");
        }

        Instant slotStart;
        try {
            slotStart = Instant.parse(request.slotStartUtc());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid slotStartUtc format");
        }

        List<Instant> validSlots = SlotGenerator.generateSlotsUtc(
                event.getStartDate(),
                event.getEndDate(),
                event.getDailyStartTime(),
                event.getDailyEndTime(),
                event.getSlotMinutes(),
                event.getTimezone()
        );

        if (!validSlots.contains(slotStart)) {
            throw new ValidationException("Invalid slot for this event");
        }

        eventRepository.finalizeEvent(event.getId(), slotStart, Instant.now());

        // Notify all participants
        List<Participant> participants = participantRepository.findByEventId(event.getId());
        for (Participant p : participants) {
            try {
                emailService.sendEventFinalized(event, p);
            } catch (Exception e) {
                // Don't fail if one email fails
            }
        }
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getFinalSelection(String publicId) {
        Event event = eventRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (!event.isFinalized()) {
            throw new NotFoundException("Event not finalized");
        }

        List<String> candidateSlotsUtc = SlotGenerator.generateSlotsUtc(
                event.getStartDate(),
                event.getEndDate(),
                event.getDailyStartTime(),
                event.getDailyEndTime(),
                event.getSlotMinutes(),
                event.getTimezone()
        ).stream().map(java.time.Instant::toString).toList();

        StatsDto stats = new StatsDto(event.getViewCount(), event.getRespondentCount());
        FinalSelectionDto finalSelection = new FinalSelectionDto(
                event.getFinalSlotStart().toString(),
                event.getFinalizedAt() != null ? event.getFinalizedAt().toString() : null
        );

        return new EventDetailResponse(
                event.getPublicId(),
                event.getTitle(),
                event.getDescription(),
                event.getTimezone(),
                event.getSlotMinutes(),
                event.getDurationMinutes(),
                event.getStartDate().toString(),
                event.getEndDate().toString(),
                event.getDailyStartTime().toString(),
                event.getDailyEndTime().toString(),
                event.getLocation(),
                event.getMeetingUrl(),
                event.getResultsVisibility(),
                candidateSlotsUtc,
                stats,
                finalSelection
        );
    }

    @Transactional(readOnly = true)
    public Event getEventByPublicId(String publicId) {
        return eventRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Transactional(readOnly = true)
    public Event getEventByHostToken(String hostToken) {
        return eventRepository.findByHostToken(hostToken)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Transactional
    public void updateEvent(String hostToken, UpdateEventRequest request) {
        Event event = eventRepository.findByHostToken(hostToken)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.isFinalized()) {
            throw new com.goblinscheduler.exception.ConflictException("Cannot edit a finalized event");
        }

        validateCreateRequest(new CreateEventRequest(
                request.title(), request.description(), request.timezone(),
                request.slotMinutes(), request.durationMinutes(),
                request.startDate(), request.endDate(),
                request.dailyStartTime(), request.dailyEndTime(),
                request.location(), request.meetingUrl(), request.resultsVisibility(),
                null, null
        ));

        event.setTitle(request.title().trim());
        event.setDescription(request.description() != null ? request.description().trim() : null);
        event.setTimezone(request.timezone());
        event.setSlotMinutes(request.slotMinutes());
        event.setDurationMinutes(request.durationMinutes());
        event.setStartDate(LocalDate.parse(request.startDate(), DATE_FORMATTER));
        event.setEndDate(LocalDate.parse(request.endDate(), DATE_FORMATTER));
        event.setDailyStartTime(LocalTime.parse(request.dailyStartTime(), TIME_FORMATTER));
        event.setDailyEndTime(LocalTime.parse(request.dailyEndTime(), TIME_FORMATTER));
        event.setLocation(request.location() != null ? request.location().trim() : null);
        event.setMeetingUrl(request.meetingUrl() != null ? request.meetingUrl().trim() : null);
        event.setResultsVisibility(request.resultsVisibility() != null ? request.resultsVisibility() : "aggregate_public");

        eventRepository.updateEvent(event);
    }

    @Transactional
    public void deleteEvent(String hostToken) {
        Event event = eventRepository.findByHostToken(hostToken)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        eventRepository.softDelete(event.getId());
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> lookupEventsByHostTokens(List<String> hostTokens) {
        List<Event> events = eventRepository.findByHostTokens(hostTokens);
        return events.stream()
                .map(e -> new EventSummaryResponse(
                        e.getPublicId(),
                        e.getTitle(),
                        e.getDescription(),
                        e.getStartDate() != null ? e.getStartDate().toString() : null,
                        e.getEndDate() != null ? e.getEndDate().toString() : null,
                        e.isFinalized(),
                        e.getHostToken(),
                        e.getRespondentCount(),
                        e.getRespondentCount(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                        e.getFinalizedAt() != null ? e.getFinalizedAt().toString() : null,
                        e.getDeadline() != null ? e.getDeadline().toString() : null
                ))
                .toList();
    }

    private void validateCreateRequest(CreateEventRequest request) {
        if (!VALID_DURATIONS.contains(request.durationMinutes())) {
            throw new ValidationException("durationMinutes must be 30, 60, or 90");
        }
        if (request.durationMinutes() % request.slotMinutes() != 0) {
            throw new ValidationException("durationMinutes must be divisible by slotMinutes");
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(request.startDate(), DATE_FORMATTER);
            endDate = LocalDate.parse(request.endDate(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format, expected ISO-8601 (yyyy-MM-dd)");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("endDate must be on or after startDate");
        }

        LocalTime dailyStart;
        LocalTime dailyEnd;
        try {
            dailyStart = LocalTime.parse(request.dailyStartTime(), TIME_FORMATTER);
            dailyEnd = LocalTime.parse(request.dailyEndTime(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid time format, expected HH:mm");
        }
        if (!dailyEnd.isAfter(dailyStart)) {
            throw new ValidationException("dailyEndTime must be after dailyStartTime");
        }

        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException e) {
            throw new ValidationException("Invalid timezone");
        }

        if (request.resultsVisibility() != null && !VALID_VISIBILITIES.contains(request.resultsVisibility())) {
            throw new ValidationException("resultsVisibility must be aggregate_public or host_only");
        }
    }
}
