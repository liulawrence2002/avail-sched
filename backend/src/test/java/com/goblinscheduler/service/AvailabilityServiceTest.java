package com.goblinscheduler.service;

import com.goblinscheduler.dto.*;
import com.goblinscheduler.exception.*;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Event createEvent() {
        Event event = new Event();
        event.setId(1L);
        event.setPublicId("abc123");
        event.setTimezone("UTC");
        event.setSlotMinutes(30);
        event.setDurationMinutes(60);
        event.setStartDate(LocalDate.of(2026, 5, 1));
        event.setEndDate(LocalDate.of(2026, 5, 1));
        event.setDailyStartTime(LocalTime.of(9, 0));
        event.setDailyEndTime(LocalTime.of(18, 0));
        return event;
    }

    @Test
    void joinParticipant_success() {
        Event event = createEvent();
        when(eventService.getEventByPublicId("abc123")).thenReturn(event);
        when(participantRepository.findByEventIdAndEmail(1L, "avery@example.com")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> {
            Participant p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        JoinParticipantRequest req = new JoinParticipantRequest("Avery", "avery@example.com");
        JoinParticipantResponse resp = availabilityService.joinParticipant("abc123", req);

        assertThat(resp.existingParticipant()).isFalse();
        assertThat(resp.participantToken()).isNotBlank();
    }

    @Test
    void joinParticipant_existingEmail() {
        Event event = createEvent();
        Participant existing = new Participant();
        existing.setId(1L);
        existing.setEventId(1L);
        existing.setToken("existing-token");
        existing.setDisplayName("Avery");
        existing.setEmail("avery@example.com");

        when(eventService.getEventByPublicId("abc123")).thenReturn(event);
        when(participantRepository.findByEventIdAndEmail(1L, "avery@example.com")).thenReturn(Optional.of(existing));

        JoinParticipantRequest req = new JoinParticipantRequest("Avery", "avery@example.com");
        JoinParticipantResponse resp = availabilityService.joinParticipant("abc123", req);

        assertThat(resp.existingParticipant()).isTrue();
        assertThat(resp.participantToken()).isEqualTo("existing-token");
    }

    @Test
    void putAvailability_finalizedEvent_throws() {
        Event event = createEvent();
        event.setFinalSlotStart(Instant.now());
        when(eventService.getEventByPublicId("abc123")).thenReturn(event);

        PutAvailabilityRequest req = new PutAvailabilityRequest(List.of());

        assertThatThrownBy(() -> availabilityService.putParticipantAvailability("abc123", "token", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event is finalized");
    }

    @Test
    void getParticipantAvailability_notFound() {
        Event event = createEvent();
        when(eventService.getEventByPublicId("abc123")).thenReturn(event);
        when(participantRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getParticipantAvailability("abc123", "bad"))
                .isInstanceOf(NotFoundException.class);
    }
}
