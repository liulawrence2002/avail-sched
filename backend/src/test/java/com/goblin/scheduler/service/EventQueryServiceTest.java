package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.dto.EventDetailResponse;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.EventStats;
import com.goblin.scheduler.repo.EventRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

  @Mock EventRepository eventRepository;
  @Mock EventStatsRepository eventStatsRepository;
  @Mock FinalSelectionRepository finalSelectionRepository;
  @Mock SlotService slotService;

  @InjectMocks EventQueryService service;

  private Event sampleEvent() {
    return new Event(
        1L,
        "pub123",
        "host456",
        "Title",
        "Desc",
        "UTC",
        30,
        60,
        LocalDate.of(2026, 4, 1),
        LocalDate.of(2026, 4, 2),
        LocalTime.of(9, 0),
        LocalTime.of(17, 0),
        "aggregate_public",
        Instant.now());
  }

  @Test
  void getEvent_returnsDetailAndIncrementsViewCount() {
    Event event = sampleEvent();
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(eventStatsRepository.findByEventId(1L)).thenReturn(Optional.of(new EventStats(1L, 10, 3)));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of());

    EventDetailResponse response = service.getEvent("pub123");

    assertEquals("pub123", response.publicId());
    assertEquals("aggregate_public", response.resultsVisibility());
    assertEquals(3, response.stats().respondentCount());
    verify(eventStatsRepository).incrementView(1L);
  }

  @Test
  void getHostEvent_returnsDetailWithoutIncrementingView() {
    Event event = sampleEvent();
    when(eventRepository.findByHostToken("host456")).thenReturn(Optional.of(event));
    when(eventStatsRepository.findByEventId(1L)).thenReturn(Optional.of(new EventStats(1L, 10, 3)));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of());

    EventDetailResponse response = service.getHostEvent("host456");

    assertEquals("pub123", response.publicId());
    verify(eventStatsRepository, org.mockito.Mockito.never()).incrementView(1L);
  }

  @Test
  void requireEvent_unknown_returns404() {
    when(eventRepository.findByPublicId("missing")).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.requireEvent("missing"));
    assertEquals(404, ex.getStatusCode().value());
  }

  @Test
  void requireEventByHostToken_unknown_returns404() {
    when(eventRepository.findByHostToken("missing")).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.requireEventByHostToken("missing"));
    assertEquals(404, ex.getStatusCode().value());
  }
}
