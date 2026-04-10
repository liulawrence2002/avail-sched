package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.dto.FinalSelectionResponse;
import com.goblin.scheduler.dto.FinalizeRequest;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.FinalSelection;
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
class FinalizationServiceTest {

  @Mock EventQueryService eventQueryService;
  @Mock FinalSelectionRepository finalSelectionRepository;
  @Mock SlotService slotService;
  @Mock ResultCache resultCache;

  @InjectMocks FinalizationService service;

  private static final Instant SLOT = Instant.parse("2026-04-01T09:00:00Z");

  private Event testEvent() {
    return new Event(
        1L,
        "pub123",
        "host456",
        "Title",
        "Line one\rATTENDEE:bad",
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
  void finalize_validHost_finalizesAndEvictsCache() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(SLOT));
    when(finalSelectionRepository.save(1L, SLOT))
        .thenReturn(new FinalSelection(1L, SLOT, Instant.now()));

    FinalSelectionResponse response =
        service.finalizeEvent("pub123", "host456", new FinalizeRequest(SLOT));

    assertEquals("pub123", response.publicId());
    verify(resultCache).evict(1L);
  }

  @Test
  void finalize_wrongHostToken_returns403() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.finalizeEvent("pub123", "not-the-host", new FinalizeRequest(SLOT)));
    assertEquals(403, ex.getStatusCode().value());
  }

  @Test
  void finalize_duplicate_returns409() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(Optional.of(new FinalSelection(1L, SLOT, Instant.now())));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.finalizeEvent("pub123", "host456", new FinalizeRequest(SLOT)));
    assertEquals(409, ex.getStatusCode().value());
  }

  @Test
  void finalize_invalidSlot_returns400() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());
    when(slotService.generateCandidateSlots(event)).thenReturn(List.of(SLOT));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.finalizeEvent(
                    "pub123",
                    "host456",
                    new FinalizeRequest(Instant.parse("2027-01-01T00:00:00Z"))));
    assertEquals(400, ex.getStatusCode().value());
  }

  @Test
  void getFinalSelection_missing_returns404() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.getFinalSelection("pub123"));
    assertEquals(404, ex.getStatusCode().value());
  }

  @Test
  void getIcs_normalizesControlCharsAndEscapesLines() {
    Event event = testEvent();
    when(eventQueryService.requireEvent("pub123")).thenReturn(event);
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(Optional.of(new FinalSelection(1L, SLOT, Instant.now())));

    String ics = service.getIcs("pub123");

    assertNotNull(ics);
    assertTrue(ics.contains("DESCRIPTION:Line one\\nATTENDEE:bad"));
    assertTrue(ics.contains("\r\n"));
  }
}
