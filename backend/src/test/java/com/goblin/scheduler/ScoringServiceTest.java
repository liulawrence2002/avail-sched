package com.goblin.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.goblin.scheduler.dto.ResultsResponse;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.Participant;
import com.goblin.scheduler.service.ScoringService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

  private final ScoringService scoringService = new ScoringService();

  @Test
  void scoresAvailabilityAcrossSubSlots() {
    Event event =
        new Event(
            1L,
            "pub",
            "host",
            "Title",
            null,
            "UTC",
            30,
            60,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 10),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            "aggregate_public",
            Instant.now());
    Instant slotOne = Instant.parse("2026-03-10T09:00:00Z");
    Instant slotTwo = Instant.parse("2026-03-10T09:30:00Z");
    List<Participant> participants =
        List.of(
            new Participant(1L, 1L, "p1", "Avery", null, Instant.now()),
            new Participant(2L, 1L, "p2", "Blair", null, Instant.now()));

    List<ResultsResponse.ResultSlot> results =
        scoringService.scoreTopSlots(
            event,
            List.of(slotOne),
            participants,
            Map.of(
                1L, Map.of(slotOne, 1.0, slotTwo, 1.0),
                2L, Map.of(slotOne, 0.6, slotTwo, 0.6)),
            true);

    assertEquals(1, results.size());
    assertEquals(3.2, results.getFirst().score());
    assertEquals(1, results.getFirst().yesCount());
    assertEquals(1, results.getFirst().maybeCount());
  }
}
