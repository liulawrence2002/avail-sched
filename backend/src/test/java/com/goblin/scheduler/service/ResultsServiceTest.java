package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.dto.ResultsResponse;
import com.goblin.scheduler.model.Event;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ResultsServiceTest {

  @Mock EventQueryService eventQueryService;
  @Mock com.goblin.scheduler.repo.ParticipantRepository participantRepository;
  @Mock com.goblin.scheduler.repo.AvailabilityRepository availabilityRepository;
  @Mock com.goblin.scheduler.repo.EventStatsRepository eventStatsRepository;
  @Mock com.goblin.scheduler.repo.FinalSelectionRepository finalSelectionRepository;
  @Mock SlotService slotService;
  @Mock ScoringService scoringService;
  @Mock ResultCache resultCache;

  @InjectMocks ResultsService service;

  private Event testEvent(String visibility) {
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
        visibility,
        Instant.now());
  }

  @Test
  void getResults_usesPublicCacheAndHidesParticipantDetails() {
    Event event = testEvent("aggregate_public");
    ResultsResponse cached = new ResultsResponse("pub123", "UTC", 2, 1L, null, false, List.of());
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(resultCache.get(1L, false)).thenReturn(cached);

    ResultsResponse response = service.getResults("pub123");

    assertFalse(response.participantDetailsVisible());
    verify(scoringService, never()).scoreTopSlots(any(), any(), any(), any(), any(Boolean.class));
  }

  @Test
  void getResults_hostOnlyVisibility_returnsForbidden() {
    Event event = testEvent("host_only");
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.getResults("pub123"));
    assertEquals(403, ex.getStatusCode().value());
  }

  @Test
  void getHostResults_includesParticipantDetails() {
    Event event = testEvent("aggregate_public");
    ResultsResponse cached = new ResultsResponse("pub123", "UTC", 2, 1L, null, true, List.of());
    when(eventQueryService.requireEventByHostToken("host456")).thenReturn(event);
    when(resultCache.get(1L, true)).thenReturn(cached);

    ResultsResponse response = service.getHostResults("host456");

    assertTrue(response.participantDetailsVisible());
  }
}
