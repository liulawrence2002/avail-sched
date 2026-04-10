package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.FinalSelection;
import com.goblin.scheduler.repo.EventRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Behavioral snapshots of the ICS body produced by the full pipeline for adversarial event titles
 * and descriptions. The assertions go through {@code FinalizationService.getIcs} (the
 * post-Phase-2.2 owner of the endpoint) so the characterization covers the full pipeline including
 * RFC 5545 line folding introduced in Phase 1.3.
 */
@ExtendWith(MockitoExtension.class)
class IcsOutputCharacterizationTest {

  @Mock EventRepository eventRepository;
  @Mock EventStatsRepository eventStatsRepository;
  @Mock FinalSelectionRepository finalSelectionRepository;
  @Mock SlotService slotService;
  @Mock ResultCache resultCache;

  private FinalizationService finalization;
  private EventQueryService query;

  @BeforeEach
  void setUp() {
    query =
        new EventQueryService(
            eventRepository, eventStatsRepository, finalSelectionRepository, slotService);
    finalization =
        new FinalizationService(query, finalSelectionRepository, slotService, resultCache);
  }

  private static final Instant SLOT_START = Instant.parse("2026-04-01T09:00:00Z");

  private Event eventWith(String title, String description) {
    return new Event(
        1L,
        "pub123",
        "host456",
        title,
        description,
        "UTC",
        30,
        60,
        LocalDate.of(2026, 4, 1),
        LocalDate.of(2026, 4, 1),
        LocalTime.of(9, 0),
        LocalTime.of(11, 0),
        "aggregate_public",
        Instant.parse("2026-03-01T00:00:00Z"));
  }

  private String renderIcs(Event event) {
    when(eventRepository.findByPublicId("pub123")).thenReturn(Optional.of(event));
    when(finalSelectionRepository.findByEventId(1L))
        .thenReturn(
            Optional.of(new FinalSelection(1L, SLOT_START, Instant.parse("2026-03-15T00:00:00Z"))));
    return finalization.getIcs("pub123");
  }

  @Test
  void adversarialTitle_commaSemicolonNewline_isEscaped() {
    String ics = renderIcs(eventWith("a,b;c\nd", "Plain description"));

    assertTrue(ics.contains("SUMMARY:a\\,b\\;c\\nd"));
    assertFalse(ics.contains("SUMMARY:a,b;c"));
  }

  @Test
  void adversarialTitle_injectedBeginVevent_stopsAtFoldableLine() {
    String ics = renderIcs(eventWith("BEGIN:VEVENT\nSUMMARY:injected", "Desc"));

    assertTrue(ics.contains("SUMMARY:BEGIN:VEVENT\\nSUMMARY:injected"));
    long rogueCount = ics.lines().filter(line -> line.equals("SUMMARY:injected")).count();
    assertEquals(0, rogueCount);
  }

  @Test
  void adversarialTitle_emoji_roundTripsUnchanged() {
    String title = "Team ☕ weekly 🗓️";
    String ics = renderIcs(eventWith(title, "Catch-up"));

    assertTrue(ics.contains("SUMMARY:" + title));
  }

  @Test
  void adversarialTitle_200Chars_isFoldedPerRfc5545() {
    String longTitle = "x".repeat(200);
    String ics = renderIcs(eventWith(longTitle, "Desc"));

    for (String physical : ics.split("\r\n", -1)) {
      int octets = physical.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
      assertTrue(octets <= 75, "Physical line '" + physical + "' is " + octets + " octets");
    }
    String unfolded = ics.replace("\r\n ", "");
    assertTrue(unfolded.contains("SUMMARY:" + longTitle));
  }

  @Test
  void backslash_inDescription_isEscapedFirst() {
    String ics = renderIcs(eventWith("Title", "path\\to\\file"));

    assertTrue(ics.contains("DESCRIPTION:path\\\\to\\\\file"));
  }

  @Test
  void nullDescription_usesDefaultCopy() {
    String ics = renderIcs(eventWith("Title", null));

    assertTrue(ics.contains("DESCRIPTION:Scheduled with Goblin Scheduler"));
  }

  @Test
  void crlfEndings_arePresentOnEveryLine() {
    String ics = renderIcs(eventWith("Title", "Desc"));

    String[] parts = ics.split("\r\n", -1);
    assertTrue(parts.length >= 11, "Expected at least 11 CRLF-separated segments");
    assertEquals("BEGIN:VCALENDAR", parts[0]);
    assertEquals("END:VCALENDAR", parts[parts.length - 2]);
    assertEquals("", parts[parts.length - 1]);
  }
}
