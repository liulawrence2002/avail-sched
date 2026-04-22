package com.goblinscheduler.util;

import com.goblinscheduler.model.Event;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

class ICSGeneratorTest {

    @Test
    void generate_success() {
        Event event = new Event();
        event.setTitle("Team dinner");
        event.setDescription("Let's eat!");
        event.setTimezone("UTC");
        event.setDurationMinutes(60);
        event.setFinalSlotStart(Instant.parse("2026-05-01T13:00:00Z"));
        event.setFinalizedAt(Instant.parse("2026-04-01T10:00:00Z"));

        String ics = ICSGenerator.generate(event);

        assertThat(ics).contains("BEGIN:VCALENDAR");
        assertThat(ics).contains("VERSION:2.0");
        assertThat(ics).contains("SUMMARY:Team dinner");
        assertThat(ics).contains("DESCRIPTION:Let's eat!");
        assertThat(ics).contains("DTSTART:20260501T130000Z");
        assertThat(ics).contains("DTEND:20260501T140000Z");
        assertThat(ics).contains("END:VCALENDAR");
    }

    @Test
    void generate_notFinalized_throws() {
        Event event = new Event();
        event.setTitle("Team dinner");
        event.setTimezone("UTC");
        event.setDurationMinutes(60);

        assertThatThrownBy(() -> ICSGenerator.generate(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not finalized");
    }

    @Test
    void generate_escapesSpecialChars() {
        Event event = new Event();
        event.setTitle("Meeting; with, stuff\\n");
        event.setTimezone("UTC");
        event.setDurationMinutes(30);
        event.setFinalSlotStart(Instant.parse("2026-05-01T13:00:00Z"));

        String ics = ICSGenerator.generate(event);
        assertThat(ics).doesNotContain("Meeting; with, stuff\\n");
    }
}
