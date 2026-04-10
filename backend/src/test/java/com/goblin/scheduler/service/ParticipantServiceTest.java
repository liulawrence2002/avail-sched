package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.dto.AvailabilityItem;
import com.goblin.scheduler.dto.JoinParticipantRequest;
import com.goblin.scheduler.dto.JoinParticipantResponse;
import com.goblin.scheduler.dto.ParticipantAvailabilityResponse;
import com.goblin.scheduler.dto.UpdateAvailabilityRequest;
import com.goblin.scheduler.model.AvailabilityRecord;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.FinalSelection;
import com.goblin.scheduler.model.Participant;
import com.goblin.scheduler.repo.AvailabilityRepository;
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
class ParticipantServiceTest {

  @Mock EventQueryService eventQueryService;
  @Mock ParticipantRepository participantRepository;
  @Mock AvailabilityRepository availabilityRepository;
  @Mock FinalSelectionRepository finalSelectionRepository;
  @Mock EventStatsRepository eventStatsRepository;
  @Mock SlotService slotService;
  @Mock TokenGenerator tokenGenerator;
  @Mock ResultCache resultCache;
  @Mock AppProperties appProperties;

  @InjectMocks ParticipantService service;

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
        LocalDate.of(2026, 4, 1),
        LocalDate.of(2026, 4, 2),
        LocalTime.of(9, 0),
        LocalTime.of(17, 0),
        "aggregate_public",
        Instant.now());
  }

  @Test
  void joinParticipant_createsTokenAndMagicLink() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(tokenGenerator.randomUrlToken()).thenReturn("ptok");
    when(appProperties.baseUrl()).thenReturn("http://localhost");
    when(participantRepository.save(any()))
        .thenReturn(new Participant(1L, 1L, "ptok", "Alice", "alice@example.com", Instant.now()));

    JoinParticipantResponse response =
        service.joinParticipant("pub123", new JoinParticipantRequest("Alice", "alice@example.com"));

    assertEquals("ptok", response.participantToken());
    assertEquals("http://localhost/e/pub123?token=ptok", response.participantLink());
    assertFalse(response.existingParticipant());
  }

  @Test
  void joinParticipant_reusesExistingEmailParticipant() {
    Event event = testEvent();
    Participant existing =
        new Participant(5L, 1L, "existing", "Alice", "alice@example.com", Instant.now());
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(participantRepository.findByEmailAndEventId("alice@example.com", 1L))
        .thenReturn(Optional.of(existing));
    when(appProperties.baseUrl()).thenReturn("http://localhost");

    JoinParticipantResponse response =
        service.joinParticipant(
            "pub123", new JoinParticipantRequest("Alice Updated", "Alice@example.com"));

    assertTrue(response.existingParticipant());
    assertEquals("existing", response.participantToken());
    verify(participantRepository).updateIdentity(5L, "Alice Updated", "alice@example.com");
    verify(participantRepository, never()).save(any());
  }

  @Test
  void joinParticipant_afterFinalized_returns409() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(Optional.of(new FinalSelection(1L, Instant.now(), Instant.now())));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.joinParticipant("pub123", new JoinParticipantRequest("Alice", null)));
    assertEquals(409, ex.getStatusCode().value());
  }

  @Test
  void getParticipantAvailability_returnsSavedItemsAndEmail() {
    Event event = testEvent();
    Participant participant =
        new Participant(2L, 1L, "ptok", "Alice", "alice@example.com", Instant.now());
    Instant slot = Instant.parse("2026-04-01T09:00:00Z");
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(participantRepository.findByTokenAndEventId("ptok", 1L))
        .thenReturn(Optional.of(participant));
    when(availabilityRepository.findByParticipantAndEventId(2L, 1L))
        .thenReturn(List.of(new AvailabilityRecord(2L, 1L, slot, 1.0)));

    ParticipantAvailabilityResponse response = service.getParticipantAvailability("pub123", "ptok");

    assertEquals("Alice", response.displayName());
    assertEquals("alice@example.com", response.email());
    assertEquals(slot, response.items().getFirst().slotStartUtc());
  }

  @Test
  void updateAvailability_replacesRecordsAndEvictsCache() {
    Event event = testEvent();
    Instant slot = Instant.parse("2026-04-01T09:00:00Z");
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(participantRepository.findByTokenAndEventId("ptok", 1L))
        .thenReturn(Optional.of(new Participant(2L, 1L, "ptok", "Alice", null, Instant.now())));
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(slot));
    when(availabilityRepository.countParticipantsWithAvailability(1L)).thenReturn(1L);

    service.updateAvailability(
        "pub123", "ptok", new UpdateAvailabilityRequest(List.of(new AvailabilityItem(slot, 1.0))));

    verify(availabilityRepository).replaceForParticipant(eq(1L), eq(2L), any());
    verify(eventStatsRepository).setRespondentCount(1L, 1L);
    verify(resultCache).evictAfterCommit(1L);
  }

  @Test
  void updateAvailability_whenFinalized_returnsConflict() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(Optional.of(new FinalSelection(1L, Instant.now(), Instant.now())));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.updateAvailability(
                    "pub123", "ptok", new UpdateAvailabilityRequest(List.of())));
    assertEquals(409, ex.getStatusCode().value());
  }
}
