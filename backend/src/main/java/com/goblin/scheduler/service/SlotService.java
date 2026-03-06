package com.goblin.scheduler.service;

import com.goblin.scheduler.model.Event;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {
    public List<Instant> generateCandidateSlots(Event event) {
        List<Instant> slots = new ArrayList<>();
        ZoneId zoneId = ZoneId.of(event.timezone());
        LocalDate date = event.startDate();
        while (!date.isAfter(event.endDate())) {
            LocalDateTime cursor = LocalDateTime.of(date, event.dailyStartTime());
            LocalDateTime latestStart = LocalDateTime.of(date, event.dailyEndTime()).minusMinutes(event.durationMinutes());
            while (!cursor.isAfter(latestStart)) {
                slots.add(cursor.atZone(zoneId).toInstant());
                cursor = cursor.plusMinutes(event.slotMinutes());
            }
            date = date.plusDays(1);
        }
        return slots;
    }
}

