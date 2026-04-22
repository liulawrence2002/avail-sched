package com.goblinscheduler.dto;

import java.util.List;

public record EventDetailResponse(
    String publicId,
    String title,
    String description,
    String timezone,
    int slotMinutes,
    int durationMinutes,
    String startDate,
    String endDate,
    String dailyStartTime,
    String dailyEndTime,
    String resultsVisibility,
    List<String> candidateSlotsUtc,
    StatsDto stats,
    FinalSelectionDto finalSelection
) {}
