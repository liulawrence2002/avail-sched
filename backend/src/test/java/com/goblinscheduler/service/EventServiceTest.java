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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, participantRepository, emailService, "http://localhost:3001");
    }

    @Test
    void createEvent_success() {
        CreateEventRequest request = new CreateEventRequest(
                "Team dinner", "Desc", "America/New_York",
                30, 60, "2026-05-01", "2026-05-03",
                "09:00", "18:00", "aggregate_public", null, null, null, null
        );

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        CreateEventResponse response = eventService.createEvent(request);

        assertThat(response.publicId()).isNotBlank();
        assertThat(response.hostToken()).isNotBlank();
        assertThat(response.hostLink()).contains("/host/");
    }

    @Test
    void createEvent_invalidDuration() {
        CreateEventRequest request = new CreateEventRequest(
                "T", null, "UTC", 15, 45,
                "2026-05-01", "2026-05-01", "09:00", "18:00", null, null, null, null, null
        );

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("durationMinutes must be 30, 60, or 90");
    }

    @Test
    void createEvent_durationNotDivisible() {
        CreateEventRequest request = new CreateEventRequest(
                "T", null, "UTC", 20, 30,
                "2026-05-01", "2026-05-01", "09:00", "18:00", null, null, null, null, null
        );

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("divisible by slotMinutes");
    }

    @Test
    void createEvent_endDateBeforeStartDate() {
        CreateEventRequest request = new CreateEventRequest(
                "T", null, "UTC", 30, 60,
                "2026-05-03", "2026-05-01", "09:00", "18:00", null, null, null, null, null
        );

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("endDate must be on or after startDate");
    }

    @Test
    void createEvent_dailyEndBeforeStart() {
        CreateEventRequest request = new CreateEventRequest(
                "T", null, "UTC", 30, 60,
                "2026-05-01", "2026-05-01", "18:00", "09:00", null, null, null, null, null
        );

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("dailyEndTime must be after dailyStartTime");
    }

    @Test
    void createEvent_invalidTimezone() {
        CreateEventRequest request = new CreateEventRequest(
                "T", null, "NotAZone", 30, 60,
                "2026-05-01", "2026-05-01", "09:00", "18:00", null, null, null, null, null
        );

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid timezone");
    }

    @Test
    void getEventDetail_notFound() {
        when(eventRepository.findByPublicId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventDetail("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void finalizeEvent_alreadyFinalized() {
        Event event = new Event();
        event.setId(1L);
        event.setPublicId("abc");
        event.setHostToken("host123");
        event.setFinalSlotStart(Instant.now());

        when(eventRepository.findByPublicId("abc")).thenReturn(Optional.of(event));

        FinalizeEventRequest req = new FinalizeEventRequest("2026-05-01T13:00:00Z");

        assertThatThrownBy(() -> eventService.finalizeEvent("abc", "host123", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already finalized");
    }

    @Test
    void finalizeEvent_invalidHostToken() {
        Event event = new Event();
        event.setId(1L);
        event.setPublicId("abc");
        event.setHostToken("realhost");

        when(eventRepository.findByPublicId("abc")).thenReturn(Optional.of(event));

        FinalizeEventRequest req = new FinalizeEventRequest("2026-05-01T13:00:00Z");

        assertThatThrownBy(() -> eventService.finalizeEvent("abc", "fakehost", req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid host token");
    }
}
