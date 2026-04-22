package com.goblinscheduler.util;

import com.goblinscheduler.model.Event;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ICSGenerator {

    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private ICSGenerator() {}

    public static String generate(Event event) {
        if (event.getFinalSlotStart() == null) {
            throw new IllegalStateException("Event not finalized");
        }

        ZoneId zoneId = ZoneId.of(event.getTimezone());
        var startZdt = event.getFinalSlotStart().atZone(zoneId);
        var endZdt = startZdt.plusMinutes(event.getDurationMinutes());

        String startUtc = startZdt.withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FORMATTER);
        String endUtc = endZdt.withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FORMATTER);
        String now = java.time.Instant.now().atZone(ZoneId.of("UTC")).format(ICS_FORMATTER);
        String uid = UUID.randomUUID().toString() + "@goblin-scheduler";

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Goblin Scheduler//EN\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("\r\n");
        sb.append("DTSTAMP:").append(now).append("\r\n");
        sb.append("DTSTART:").append(startUtc).append("\r\n");
        sb.append("DTEND:").append(endUtc).append("\r\n");
        sb.append("SUMMARY:").append(escape(event.getTitle())).append("\r\n");
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append("DESCRIPTION:").append(escape(event.getDescription())).append("\r\n");
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            sb.append("LOCATION:").append(escape(event.getLocation())).append("\r\n");
        }
        if (event.getMeetingUrl() != null && !event.getMeetingUrl().isBlank()) {
            sb.append("URL:").append(escape(event.getMeetingUrl())).append("\r\n");
        }
        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");

        return sb.toString();
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace(";", "\\;")
                    .replace(",", "\\,")
                    .replace("\n", "\\n")
                    .replace("\r", "");
    }
}
