package com.goblin.scheduler.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventDetailResponse(
    String publicId,
    String title,
    String description,
    String timezone,
    int slotMinutes,
    int durationMinutes,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime dailyStartTime,
    LocalTime dailyEndTime,
    List<Instant> candidateSlotsUtc,
    StatsView stats,
    FinalView finalSelection
) {
    public record StatsView(long viewCount, long responseCount) {}
    public record FinalView(Instant slotStartUtc, Instant finalizedAt) {}
}

