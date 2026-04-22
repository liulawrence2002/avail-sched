package com.goblinscheduler.util;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class SlotGenerator {

    private SlotGenerator() {}

    public static List<Instant> generateSlotsUtc(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime dailyStartTime,
            LocalTime dailyEndTime,
            int slotMinutes,
            String timezone
    ) {
        List<Instant> slots = new ArrayList<>();
        ZoneId zoneId = ZoneId.of(timezone);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dateTime = LocalDateTime.of(currentDate, dailyStartTime);
            ZonedDateTime zonedStart = dateTime.atZone(zoneId);
            ZonedDateTime zonedEnd = LocalDateTime.of(currentDate, dailyEndTime).atZone(zoneId);

            ZonedDateTime current = zonedStart;
            while (current.isBefore(zonedEnd) || current.equals(zonedEnd)) {
                // slot must fit within daily bounds
                if (current.plusMinutes(slotMinutes).isAfter(zonedEnd)) {
                    break;
                }
                slots.add(current.toInstant());
                current = current.plusMinutes(slotMinutes);
            }

            currentDate = currentDate.plusDays(1);
        }

        return slots;
    }
}
