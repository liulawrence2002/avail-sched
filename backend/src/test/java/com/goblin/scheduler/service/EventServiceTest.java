package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.dto.AvailabilityItem;
import com.goblin.scheduler.dto.CreateEventRequest;
import com.goblin.scheduler.dto.CreateEventResponse;
import com.goblin.scheduler.dto.EventDetailResponse;
import com.goblin.scheduler.dto.FinalSelectionResponse;
import com.goblin.scheduler.dto.FinalizeRequest;
import com.goblin.scheduler.dto.JoinParticipantRequest;
import com.goblin.scheduler.dto.JoinParticipantResponse;
import com.goblin.scheduler.dto.ParticipantAvailabilityResponse;
import com.goblin.scheduler.dto.ResultsResponse;
import com.goblin.scheduler.dto.UpdateAvailabilityRequest;
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
import com.goblin.scheduler.util.TokenGenerator;
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
class EventServiceTest {

  @Mock EventRepository eventRepository;
  @Mock ParticipantRepository participantRepository;
  @Mock AvailabilityRepository availabilityRepository;
  @Mock FinalSelectionRepository finalSelectionRepository;
  @Mock EventStatsRepository eventStatsRepository;
  @Mock SlotService slotService;
  @Mock ScoringService scoringService;
  @Mock TokenGenerator tokenGenerator;
  @Mock ResultCache resultCache;
  @Mock AppProperties appProperties;

  @InjectMocks EventService eventService;

  private static final LocalDate START = LocalDate.of(2026, 4, 1);
  private static final LocalDate END = LocalDate.of(2026, 4, 2);
  private static final LocalTime DAILY_START = LocalTime.of(9, 0);
  private static final LocalTime DAILY_END = LocalTime.of(17, 0);

  private Event testEvent() {
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
  void createEvent_savesAndReturnsResponseWithDefaultVisibility() {
    Event saved = testEvent();
    when(tokenGenerator.randomPublicId()).thenReturn("pub123");
    when(tokenGenerator.randomUrlToken()).thenReturn("host456");
    when(eventRepository.save(any())).thenReturn(saved);
    when(appProperties.baseUrl()).thenReturn("http://localhost");

    CreateEventResponse response = eventService.createEvent(validRequest());

    assertEquals("pub123", response.publicId());
    assertEquals("host456", response.hostToken());
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
        assertThrows(ResponseStatusException.class, () -> eventService.createEvent(request));

    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void getEvent_returnsDetailWithVisibility() {
    Event event = testEvent();
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(eventStatsRepository.findByEventId(1L)).thenReturn(Optional.of(new EventStats(1L, 10, 3)));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of());

    EventDetailResponse response = eventService.getEvent("pub123");

    assertEquals("aggregate_public", response.resultsVisibility());
    assertEquals(3, response.stats().respondentCount());
    verify(eventStatsRepository).incrementView(1L);
  }

  @Test
  void joinParticipant_createsTokenAndMagicLink() {
    Event event = testEvent();
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(tokenGenerator.randomUrlToken()).thenReturn("ptok");
    when(appProperties.baseUrl()).thenReturn("http://localhost");
    when(participantRepository.save(any()))
        .thenReturn(new Participant(1L, 1L, "ptok", "Alice", "alice@example.com", Instant.now()));

    JoinParticipantResponse response =
        eventService.joinParticipant(
            "pub123", new JoinParticipantRequest("Alice", "alice@example.com"));

    assertEquals("ptok", response.participantToken());
    assertEquals("http://localhost/e/pub123?token=ptok", response.participantLink());
    assertFalse(response.existingParticipant());
  }

  @Test
  void joinParticipant_reusesExistingEmailParticipant() {
    Event event = testEvent();
    Participant existing =
        new Participant(5L, 1L, "existing", "Alice", "alice@example.com", Instant.now());
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(participantRepository.findByEmailAndEventId("alice@example.com", 1L))
        .thenReturn(Optional.of(existing));
    when(appProperties.baseUrl()).thenReturn("http://localhost");

    JoinParticipantResponse response =
        eventService.joinParticipant(
            "pub123", new JoinParticipantRequest("Alice Updated", "Alice@example.com"));

    assertTrue(response.existingParticipant());
    assertEquals("existing", response.participantToken());
    verify(participantRepository).updateIdentity(5L, "Alice Updated", "alice@example.com");
    verify(participantRepository, never()).save(any());
  }

  @Test
  void getParticipantAvailability_returnsSavedItemsAndEmail() {
    Event event = testEvent();
    Participant participant =
        new Participant(2L, 1L, "ptok", "Alice", "alice@example.com", Instant.now());
    Instant slot = Instant.parse("2026-04-01T09:00:00Z");
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(participantRepository.findByTokenAndEventId("ptok", 1L))
        .thenReturn(Optional.of(participant));
    when(availabilityRepository.findByParticipantAndEventId(2L, 1L))
        .thenReturn(List.of(new AvailabilityRecord(2L, 1L, slot, 1.0)));

    ParticipantAvailabilityResponse response =
        eventService.getParticipantAvailability("pub123", "ptok");

    assertEquals("Alice", response.displayName());
    assertEquals("alice@example.com", response.email());
    assertEquals(slot, response.items().getFirst().slotStartUtc());
  }

  @Test
  void updateAvailability_replacesRecordsAndEvictsCache() {
    Event event = testEvent();
    Instant slot = Instant.parse("2026-04-01T09:00:00Z");
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(participantRepository.findByTokenAndEventId("ptok", 1L))
        .thenReturn(Optional.of(new Participant(2L, 1L, "ptok", "Alice", null, Instant.now())));
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(slot));
    when(availabilityRepository.countParticipantsWithAvailability(1L)).thenReturn(1L);

    eventService.updateAvailability(
        "pub123", "ptok", new UpdateAvailabilityRequest(List.of(new AvailabilityItem(slot, 1.0))));

    verify(availabilityRepository).replaceForParticipant(eq(1L), eq(2L), any());
    verify(eventStatsRepository).setRespondentCount(1L, 1L);
    verify(resultCache).evict(1L);
  }

  @Test
  void updateAvailability_whenFinalized_returnsConflict() {
    Event event = testEvent();
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(Optional.of(new FinalSelection(1L, Instant.now(), Instant.now())));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                eventService.updateAvailability(
                    "pub123", "ptok", new UpdateAvailabilityRequest(List.of())));

    assertEquals(409, ex.getStatusCode().value());
  }

  @Test
  void getResults_usesPublicCacheAndHidesParticipantDetails() {
    Event event = testEvent();
    ResultsResponse cached = new ResultsResponse("pub123", "UTC", 2, 1L, null, false, List.of());
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(resultCache.get(1L, false)).thenReturn(cached);

    ResultsResponse response = eventService.getResults("pub123");

    assertFalse(response.participantDetailsVisible());
    verify(scoringService, never()).scoreTopSlots(any(), any(), any(), any(), any(Boolean.class));
  }

  @Test
  void getResults_hostOnlyVisibility_returnsForbidden() {
    Event event =
        new Event(
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
            "host_only",
            Instant.now());
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> eventService.getResults("pub123"));

    assertEquals(403, ex.getStatusCode().value());
  }

  @Test
  void getHostResults_includesParticipantDetails() {
    Event event = testEvent();
    ResultsResponse cached = new ResultsResponse("pub123", "UTC", 2, 1L, null, true, List.of());
    when(eventRepository.findByHostToken("host456")).thenReturn(Optional.of(event));
    when(resultCache.get(1L, true)).thenReturn(cached);

    ResultsResponse response = eventService.getHostResults("host456");

    assertTrue(response.participantDetailsVisible());
  }

  @Test
  void finalizeEvent_validHost_finalizesAndEvictsCache() {
    Event event = testEvent();
    Instant slot = Instant.parse("2026-04-01T09:00:00Z");
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(slot));
    when(finalSelectionRepository.save(1L, slot))
        .thenReturn(new FinalSelection(1L, slot, Instant.now()));

    FinalSelectionResponse response =
        eventService.finalizeEvent("pub123", "host456", new FinalizeRequest(slot));

    assertEquals("pub123", response.publicId());
    verify(resultCache).evict(1L);
  }

  @Test
  void getIcs_normalizesCarriageReturns() {
    Event event =
        new Event(
            1L,
            "pub123",
            "host456",
            "Title",
            "Line one\rATTENDEE:bad",
            "UTC",
            30,
            60,
            START,
            END,
            DAILY_START,
            DAILY_END,
            "aggregate_public",
            Instant.now());
    Instant slot = Instant.parse("2026-04-01T09:00:00Z");
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(Optional.of(new FinalSelection(1L, slot, Instant.now())));

    String ics = eventService.getIcs("pub123");

    assertTrue(ics.contains("DESCRIPTION:Line one\\nATTENDEE:bad"));
    assertTrue(ics.contains("\r\n"));
    assertNotNull(ics);
  }
}
