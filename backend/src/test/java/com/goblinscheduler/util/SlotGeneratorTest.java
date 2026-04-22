package com.goblinscheduler.util;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SlotGeneratorTest {

    @Test
    void generateSlotsUtc_basic() {
        List<Instant> slots = SlotGenerator.generateSlotsUtc(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                60,
                "UTC"
        );

        assertThat(slots).hasSize(3);
        assertThat(slots.get(0)).isEqualTo(Instant.parse("2026-05-01T09:00:00Z"));
        assertThat(slots.get(1)).isEqualTo(Instant.parse("2026-05-01T10:00:00Z"));
        assertThat(slots.get(2)).isEqualTo(Instant.parse("2026-05-01T11:00:00Z"));
    }

    @Test
    void generateSlotsUtc_multipleDays() {
        List<Instant> slots = SlotGenerator.generateSlotsUtc(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                30,
                "UTC"
        );

        assertThat(slots).hasSize(4);
        assertThat(slots.get(0)).isEqualTo(Instant.parse("2026-05-01T09:00:00Z"));
        assertThat(slots.get(1)).isEqualTo(Instant.parse("2026-05-01T09:30:00Z"));
        assertThat(slots.get(2)).isEqualTo(Instant.parse("2026-05-02T09:00:00Z"));
        assertThat(slots.get(3)).isEqualTo(Instant.parse("2026-05-02T09:30:00Z"));
    }

    @Test
    void generateSlotsUtc_slotDoesNotFitAtEnd() {
        // 9:00-10:00 with 45 min slots -> only 1 slot (9:00), 9:45 doesn't fit
        List<Instant> slots = SlotGenerator.generateSlotsUtc(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                45,
                "UTC"
        );

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0)).isEqualTo(Instant.parse("2026-05-01T09:00:00Z"));
    }
}
