package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.dto.AvailabilityItem;
import com.goblin.scheduler.dto.CreateEventRequest;
import com.goblin.scheduler.dto.CreateEventResponse;
import com.goblin.scheduler.dto.FinalSelectionResponse;
import com.goblin.scheduler.dto.FinalizeRequest;
import com.goblin.scheduler.dto.JoinParticipantRequest;
import com.goblin.scheduler.dto.JoinParticipantResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cross-cutting characterization tests for {@link EventService}.
 *
 * <p>Written before the Phase 2 service split so that the refactor can be validated by running
 * these tests unchanged. This complements {@code EventServiceTest} (which drills into individual
 * methods) by exercising the full create → join → save → results → finalize → ICS pipeline as a
 * single flow, and by covering the edge cases the characterization requirement in Phase 0.4 calls
 * out: duplicate finalize (409), finalize-invalid-slot (400), and out-of-range availability items
 * silently filtered.
 *
 * <p>When Phase 2.2 splits {@code EventService} into five smaller services, this file is the safety
 * net that proves external behavior is preserved.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceCharacterizationTest {

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

  private static final long EVENT_ID = 42L;
  private static final String PUBLIC_ID = "pub-xyz";
  private static final String HOST_TOKEN = "host-xyz";
  private static final LocalDate START_DATE = LocalDate.of(2026, 4, 1);
  private static final LocalDate END_DATE = LocalDate.of(2026, 4, 1);
  private static final LocalTime DAILY_START = LocalTime.of(9, 0);
  private static final LocalTime DAILY_END = LocalTime.of(11, 0);
  // 30-minute slots × 60-minute duration ⇒ valid starts at 09:00 and 09:30 (latestStart = 10:00).
  private static final Instant SLOT_ONE = Instant.parse("2026-04-01T09:00:00Z");
  private static final Instant SLOT_TWO = Instant.parse("2026-04-01T09:30:00Z");
  private static final Instant OUT_OF_RANGE_SLOT = Instant.parse("2026-04-01T15:00:00Z");

  private Event event(String visibility) {
    return new Event(
        EVENT_ID,
        PUBLIC_ID,
        HOST_TOKEN,
        "Design review",
        "Sync with the team",
        "UTC",
        30,
        60,
        START_DATE,
        END_DATE,
        DAILY_START,
        DAILY_END,
        visibility,
        Instant.parse("2026-03-01T00:00:00Z"));
  }

  private CreateEventRequest validCreateRequest() {
    return new CreateEventRequest(
        "Design review",
        "Sync with the team",
        "UTC",
        30,
        60,
        START_DATE,
        END_DATE,
        DAILY_START,
        DAILY_END,
        null);
  }

  @Test
  void fullHappyFlow_createJoinSaveResultsFinalizeIcs() {
    Event event = event("aggregate_public");

    // --- create ---
    when(tokenGenerator.randomPublicId()).thenReturn(PUBLIC_ID);
    when(tokenGenerator.randomUrlToken()).thenReturn(HOST_TOKEN, "participant-tok");
    when(eventRepository.save(any())).thenReturn(event);
    when(appProperties.baseUrl()).thenReturn("https://example.test");

    CreateEventResponse created = eventService.createEvent(validCreateRequest());
    assertEquals(PUBLIC_ID, created.publicId());
    assertEquals(HOST_TOKEN, created.hostToken());
    assertEquals("https://example.test/host/" + HOST_TOKEN, created.hostLink());
    verify(eventStatsRepository).init(EVENT_ID);

    // --- join ---
    when(eventRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(event));
    lenient().when(finalSelectionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
    Participant savedParticipant =
        new Participant(7L, EVENT_ID, "participant-tok", "Alice", null, Instant.now());
    when(participantRepository.save(any())).thenReturn(savedParticipant);

    JoinParticipantResponse joined =
        eventService.joinParticipant(PUBLIC_ID, new JoinParticipantRequest("Alice", null));
    assertEquals("participant-tok", joined.participantToken());
    assertFalse(joined.existingParticipant());

    // --- save availability ---
    when(participantRepository.findByTokenAndEventId("participant-tok", EVENT_ID))
        .thenReturn(Optional.of(savedParticipant));
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(SLOT_ONE, SLOT_TWO));
    when(availabilityRepository.countParticipantsWithAvailability(EVENT_ID)).thenReturn(1L);

    eventService.updateAvailability(
        PUBLIC_ID,
        "participant-tok",
        new UpdateAvailabilityRequest(List.of(new AvailabilityItem(SLOT_ONE, 1.0))));
    verify(availabilityRepository).replaceForParticipant(eq(EVENT_ID), eq(7L), any());
    verify(resultCache).evict(EVENT_ID);

    // --- results ---
    when(resultCache.get(EVENT_ID, false)).thenReturn(null);
    when(participantRepository.findByEventId(EVENT_ID)).thenReturn(List.of(savedParticipant));
    when(availabilityRepository.findByEventId(EVENT_ID))
        .thenReturn(List.of(new AvailabilityRecord(7L, EVENT_ID, SLOT_ONE, 1.0)));
    when(eventStatsRepository.findByEventId(EVENT_ID))
        .thenReturn(Optional.of(new EventStats(EVENT_ID, 0, 1)));
    ResultsResponse.ResultSlot topSlot =
        new ResultsResponse.ResultSlot(SLOT_ONE, 1.0, 100.0, 1, 0, 0, 0, List.of(), List.of());
    when(scoringService.scoreTopSlots(any(), any(), any(), any(), eq(false)))
        .thenReturn(List.of(topSlot));

    ResultsResponse results = eventService.getResults(PUBLIC_ID);
    assertEquals(1, results.participantCount());
    assertEquals(1, results.topSlots().size());
    assertFalse(results.participantDetailsVisible());
    verify(resultCache).put(EVENT_ID, false, results);

    // --- finalize ---
    when(finalSelectionRepository.save(EVENT_ID, SLOT_ONE))
        .thenReturn(new FinalSelection(EVENT_ID, SLOT_ONE, Instant.parse("2026-03-15T00:00:00Z")));

    FinalSelectionResponse finalized =
        eventService.finalizeEvent(PUBLIC_ID, HOST_TOKEN, new FinalizeRequest(SLOT_ONE));
    assertEquals(PUBLIC_ID, finalized.publicId());
    assertEquals(SLOT_ONE, finalized.slotStartUtc());

    // --- ICS ---
    when(finalSelectionRepository.findByEventId(EVENT_ID))
        .thenReturn(
            Optional.of(
                new FinalSelection(EVENT_ID, SLOT_ONE, Instant.parse("2026-03-15T00:00:00Z"))));
    String ics = eventService.getIcs(PUBLIC_ID);

    assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
    assertTrue(ics.contains("VERSION:2.0"));
    assertTrue(ics.contains("PRODID:-//Goblin Scheduler//EN"));
    assertTrue(ics.contains("BEGIN:VEVENT\r\n"));
    assertTrue(ics.contains("UID:" + PUBLIC_ID + "@goblin-scheduler"));
    assertTrue(ics.contains("DTSTART:20260401T090000Z"));
    assertTrue(ics.contains("DTEND:20260401T100000Z"));
    assertTrue(ics.contains("SUMMARY:Design review"));
    assertTrue(ics.contains("DESCRIPTION:Sync with the team"));
    assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
  }

  @Test
  void duplicateFinalize_returns409() {
    Event event = event("aggregate_public");
    when(eventRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(EVENT_ID))
        .thenReturn(Optional.of(new FinalSelection(EVENT_ID, SLOT_ONE, Instant.now())));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> eventService.finalizeEvent(PUBLIC_ID, HOST_TOKEN, new FinalizeRequest(SLOT_TWO)));
    assertEquals(409, ex.getStatusCode().value());
  }

  @Test
  void finalizeInvalidSlot_returns400() {
    Event event = event("aggregate_public");
    when(eventRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(SLOT_ONE, SLOT_TWO));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                eventService.finalizeEvent(
                    PUBLIC_ID, HOST_TOKEN, new FinalizeRequest(OUT_OF_RANGE_SLOT)));
    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void finalizeWrongHostToken_returns403() {
    Event event = event("aggregate_public");
    when(eventRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(event));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                eventService.finalizeEvent(
                    PUBLIC_ID, "not-the-host-token", new FinalizeRequest(SLOT_ONE)));
    assertEquals(403, ex.getStatusCode().value());
  }

  @Test
  void updateAvailability_silentlyFiltersOutOfRangeSlots() {
    Event event = event("aggregate_public");
    Participant participant = new Participant(7L, EVENT_ID, "ptok", "Alice", null, Instant.now());

    when(eventRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
    when(participantRepository.findByTokenAndEventId("ptok", EVENT_ID))
        .thenReturn(Optional.of(participant));
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(SLOT_ONE, SLOT_TWO));
    when(availabilityRepository.countParticipantsWithAvailability(EVENT_ID)).thenReturn(1L);

    eventService.updateAvailability(
        PUBLIC_ID,
        "ptok",
        new UpdateAvailabilityRequest(
            List.of(
                new AvailabilityItem(SLOT_ONE, 1.0),
                new AvailabilityItem(OUT_OF_RANGE_SLOT, 1.0),
                new AvailabilityItem(SLOT_TWO, 0.5))));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AvailabilityRecord>> captor = ArgumentCaptor.forClass(List.class);
    verify(availabilityRepository).replaceForParticipant(eq(EVENT_ID), eq(7L), captor.capture());

    List<Instant> kept = new ArrayList<>();
    for (AvailabilityRecord record : captor.getValue()) {
      kept.add(record.slotStartUtc());
    }
    assertEquals(List.of(SLOT_ONE, SLOT_TWO), kept);
    assertNotNull(kept);
  }

  @Test
  void hostOnlyVisibility_blocksPublicResultsButAllowsHostResults() {
    Event hostOnlyEvent = event("host_only");
    when(eventRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(hostOnlyEvent));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> eventService.getResults(PUBLIC_ID));
    assertEquals(403, ex.getStatusCode().value());

    when(eventRepository.findByHostToken(HOST_TOKEN)).thenReturn(Optional.of(hostOnlyEvent));
    when(resultCache.get(EVENT_ID, true))
        .thenReturn(new ResultsResponse(PUBLIC_ID, "UTC", 0, 0L, null, true, List.of()));

    ResultsResponse hostView = eventService.getHostResults(HOST_TOKEN);
    assertTrue(hostView.participantDetailsVisible());
  }
}
