package com.goblin.scheduler.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record Event(
    long id,
    String publicId,
    String hostToken,
    String title,
    String description,
    String timezone,
    int slotMinutes,
    int durationMinutes,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime dailyStartTime,
    LocalTime dailyEndTime,
    String resultsVisibility,
    Instant createdAt) {}
