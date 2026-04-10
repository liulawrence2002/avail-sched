package com.goblin.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.service.SlotService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class SlotServiceTest {

  private final SlotService slotService = new SlotService();

  @Test
  void generatesCandidateSlotsAcrossDays() {
    Event event =
        new Event(
            1L,
            "pub",
            "host",
            "Title",
            null,
            "America/New_York",
            30,
            60,
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 11),
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            "aggregate_public",
            Instant.now());

    assertEquals(6, slotService.generateCandidateSlots(event).size());
  }
}
