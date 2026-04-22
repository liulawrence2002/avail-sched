package com.goblinscheduler.dto;

public record EventSummaryResponse(
    String publicId,
    String title,
    String description,
    String startDate,
    String endDate,
    boolean finalized,
    String hostToken,
    int participantCount,
    int respondentCount,
    String createdAt,
    String finalizedAt,
    String deadline
) {}
