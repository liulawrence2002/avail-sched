package com.goblinscheduler.service;

import com.goblinscheduler.dto.ResultsResponse;
import com.goblinscheduler.exception.ForbiddenException;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultsServiceTest {

    @Mock
    private EventService eventService;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private ResultsService resultsService;

    private Event createEvent() {
        Event event = new Event();
        event.setId(1L);
        event.setPublicId("abc123");
        event.setTimezone("UTC");
        event.setResultsVisibility("aggregate_public");
        event.setRespondentCount(2);
        return event;
    }

    @Test
    void getPublicResults_hostOnly_throwsForbidden() {
        Event event = createEvent();
        event.setResultsVisibility("host_only");
        when(eventService.getEventByPublicId("abc123")).thenReturn(event);

        assertThatThrownBy(() -> resultsService.getPublicResults("abc123"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("host-only");
    }

    @Test
    void getHostResults_success() {
        Event event = createEvent();
        event.setHostToken("host123");
        when(eventService.getEventByHostToken("host123")).thenReturn(event);

        ParticipantRepository.ParticipantAvailabilityView v1 = new ParticipantRepository.ParticipantAvailabilityView();
        v1.setParticipantId(1L);
        v1.setDisplayName("Alice");
        v1.setEmail("alice@example.com");
        v1.setSlotStart(Instant.parse("2026-05-01T13:00:00Z"));
        v1.setWeight(BigDecimal.ONE);

        ParticipantRepository.ParticipantAvailabilityView v2 = new ParticipantRepository.ParticipantAvailabilityView();
        v2.setParticipantId(2L);
        v2.setDisplayName("Bob");
        v2.setEmail("bob@example.com");
        v2.setSlotStart(Instant.parse("2026-05-01T13:00:00Z"));
        v2.setWeight(BigDecimal.valueOf(0.6));

        when(participantRepository.findAllAvailabilityByEventId(1L)).thenReturn(List.of(v1, v2));

        ResultsResponse response = resultsService.getHostResults("host123");

        assertThat(response.publicId()).isEqualTo("abc123");
        assertThat(response.participantDetailsVisible()).isTrue();
        assertThat(response.participantCount()).isEqualTo(2);
        assertThat(response.topSlots()).isNotEmpty();

        var topSlot = response.topSlots().get(0);
        assertThat(topSlot.score()).isEqualTo(BigDecimal.valueOf(1.6));
        assertThat(topSlot.yesCount()).isEqualTo(1);
        assertThat(topSlot.maybeCount()).isEqualTo(1);
        assertThat(topSlot.canAttend()).contains("Alice", "Bob");
    }

    @Test
    void getPublicResults_hidesParticipantDetails() {
        Event event = createEvent();
        when(eventService.getEventByPublicId("abc123")).thenReturn(event);

        ParticipantRepository.ParticipantAvailabilityView v1 = new ParticipantRepository.ParticipantAvailabilityView();
        v1.setParticipantId(1L);
        v1.setDisplayName("Alice");
        v1.setSlotStart(Instant.parse("2026-05-01T13:00:00Z"));
        v1.setWeight(BigDecimal.ONE);

        when(participantRepository.findAllAvailabilityByEventId(1L)).thenReturn(List.of(v1));

        ResultsResponse response = resultsService.getPublicResults("abc123");
        assertThat(response.participantDetailsVisible()).isFalse();

        var topSlot = response.topSlots().get(0);
        assertThat(topSlot.canAttend()).isEmpty();
        assertThat(topSlot.cannotAttend()).isEmpty();
    }
}
