package com.goblinscheduler.service;

import com.goblinscheduler.dto.*;
import com.goblinscheduler.exception.*;
import com.goblinscheduler.model.AvailabilityItem;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import com.goblinscheduler.util.SlotGenerator;
import com.goblinscheduler.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final EmailService emailService;
    private final String appUrl;

    public AvailabilityService(ParticipantRepository participantRepository,
                               EventRepository eventRepository,
                               EventService eventService,
                               EmailService emailService,
                               @Value("${goblin.app-url}") String appUrl) {
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.emailService = emailService;
        this.appUrl = appUrl;
    }

    @Transactional
    public JoinParticipantResponse joinParticipant(String publicId, JoinParticipantRequest request) {
        Event event = eventService.getEventByPublicId(publicId);

        if (event.isFinalized()) {
            throw new ConflictException("Event is finalized");
        }

        if (request.email() != null && !request.email().isBlank()) {
            Optional<Participant> existing = participantRepository.findByEventIdAndEmail(event.getId(), request.email().trim());
            if (existing.isPresent()) {
                Participant p = existing.get();
                return new JoinParticipantResponse(
                        p.getToken(),
                        buildParticipantLink(publicId, p.getToken()),
                        true
                );
            }
        }

        Participant participant = new Participant();
        participant.setEventId(event.getId());
        participant.setToken(TokenGenerator.generateParticipantToken());
        participant.setDisplayName(request.displayName().trim());
        participant.setEmail(request.email() != null && !request.email().isBlank() ? request.email().trim() : null);

        participantRepository.save(participant);

        // Send welcome email asynchronously
        try {
            emailService.sendParticipantWelcome(event, participant);
        } catch (Exception e) {
            // Don't fail the join if email fails
        }

        return new JoinParticipantResponse(
                participant.getToken(),
                buildParticipantLink(publicId, participant.getToken()),
                false
        );
    }

    @Transactional(readOnly = true)
    public ParticipantAvailabilityResponse getParticipantAvailability(String publicId, String token) {
        Event event = eventService.getEventByPublicId(publicId);
        Participant participant = participantRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        if (!participant.getEventId().equals(event.getId())) {
            throw new NotFoundException("Participant not found for this event");
        }

        List<AvailabilityItemDto> items = participantRepository.findItemsByParticipantId(participant.getId())
                .stream()
                .map(i -> new AvailabilityItemDto(i.getSlotStart().toString(), i.getWeight()))
                .toList();

        return new ParticipantAvailabilityResponse(
                participant.getDisplayName(),
                participant.getEmail(),
                items
        );
    }

    @Transactional
    public void putParticipantAvailability(String publicId, String token, PutAvailabilityRequest request) {
        Event event = eventService.getEventByPublicId(publicId);

        if (event.isFinalized()) {
            throw new ConflictException("Event is finalized");
        }

        Participant participant = participantRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        if (!participant.getEventId().equals(event.getId())) {
            throw new NotFoundException("Participant not found for this event");
        }

        List<Instant> validSlots = SlotGenerator.generateSlotsUtc(
                event.getStartDate(),
                event.getEndDate(),
                event.getDailyStartTime(),
                event.getDailyEndTime(),
                event.getSlotMinutes(),
                event.getTimezone()
        );

        Set<Instant> validSlotSet = new HashSet<>(validSlots);

        List<AvailabilityItem> items = new ArrayList<>();
        boolean hasExisting = !participantRepository.findItemsByParticipantId(participant.getId()).isEmpty();

        for (AvailabilityItemDto dto : request.items()) {
            Instant slotStart;
            try {
                slotStart = Instant.parse(dto.slotStartUtc());
            } catch (Exception e) {
                continue; // invalid slots ignored
            }

            if (!validSlotSet.contains(slotStart)) {
                continue; // invalid slots ignored
            }

            BigDecimal weight = dto.weight();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
                continue;
            }

            weight = weight.setScale(2, RoundingMode.HALF_UP);
            items.add(new AvailabilityItem(participant.getId(), slotStart, weight));
        }

        participantRepository.deleteItemsByParticipantId(participant.getId());
        participantRepository.saveItems(participant.getId(), items);

        if (!hasExisting && !items.isEmpty()) {
            eventRepository.incrementRespondentCount(event.getId());
        }
    }

    private String buildParticipantLink(String publicId, String token) {
        return String.format("%s/e/%s?token=%s", appUrl, publicId, token);
    }
}
