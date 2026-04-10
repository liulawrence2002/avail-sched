package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.dto.CreateEventRequest;
import com.goblin.scheduler.dto.CreateEventResponse;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.repo.EventRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.util.TokenGenerator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EventCreationServiceTest {

  @Mock EventRepository eventRepository;
  @Mock EventStatsRepository eventStatsRepository;
  @Mock TokenGenerator tokenGenerator;
  @Mock AppProperties appProperties;

  @InjectMocks EventCreationService service;

  private static final LocalDate START = LocalDate.of(2026, 4, 1);
  private static final LocalDate END = LocalDate.of(2026, 4, 2);
  private static final LocalTime DAILY_START = LocalTime.of(9, 0);
  private static final LocalTime DAILY_END = LocalTime.of(17, 0);

  private Event savedEvent() {
    return new Event(
        1L,
        "pub123",
        "host456",
        "Title",
        "Desc",
        "UTC",
        30,
        60,
        START,
        END,
        DAILY_START,
        DAILY_END,
        "aggregate_public",
        Instant.now());
  }

  private CreateEventRequest validRequest() {
    return new CreateEventRequest(
        "Team Sync", "A description", "UTC", 30, 60, START, END, DAILY_START, DAILY_END, null);
  }

  @Test
  void createEvent_defaultsVisibilityToAggregatePublic() {
    when(tokenGenerator.randomPublicId()).thenReturn("pub123");
    when(tokenGenerator.randomUrlToken()).thenReturn("host456");
    when(eventRepository.save(any())).thenReturn(savedEvent());
    when(appProperties.baseUrl()).thenReturn("http://localhost");

    CreateEventResponse response = service.createEvent(validRequest());

    assertEquals("pub123", response.publicId());
    assertEquals("host456", response.hostToken());
    assertEquals("http://localhost/host/host456", response.hostLink());
    verify(eventRepository)
        .save(argThat(event -> "aggregate_public".equals(event.resultsVisibility())));
    verify(eventStatsRepository).init(1L);
  }

  @Test
  void createEvent_invalidTimezone_returns400() {
    CreateEventRequest request =
        new CreateEventRequest(
            "Title", null, "Fake/Zone", 30, 60, START, END, DAILY_START, DAILY_END, null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.createEvent(request));
    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void createEvent_disallowedDuration_returns400() {
    CreateEventRequest request =
        new CreateEventRequest(
            "Title", null, "UTC", 30, 45, START, END, DAILY_START, DAILY_END, null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.createEvent(request));
    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void createEvent_endBeforeStart_returns400() {
    CreateEventRequest request =
        new CreateEventRequest(
            "Title", null, "UTC", 30, 60, END, START, DAILY_START, DAILY_END, null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.createEvent(request));
    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void createEvent_dailyEndBeforeStart_returns400() {
    CreateEventRequest request =
        new CreateEventRequest(
            "Title", null, "UTC", 30, 60, START, END, DAILY_END, DAILY_START, null);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.createEvent(request));
    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void createEvent_unknownVisibility_returns400() {
    CreateEventRequest request =
        new CreateEventRequest(
            "Title", null, "UTC", 30, 60, START, END, DAILY_START, DAILY_END, "everybody_gets_it");

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.createEvent(request));
    assertEquals(400, ex.getStatusCode().value());
  }
}
