package com.goblinscheduler.dto;

import jakarta.validation.constraints.*;

public record UpdateEventRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 160, message = "Title must be at most 160 characters")
    String title,

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    String description,

    @NotBlank(message = "Timezone is required")
    String timezone,

    @NotNull(message = "slotMinutes is required")
    @Min(value = 15, message = "slotMinutes must be at least 15")
    @Max(value = 60, message = "slotMinutes must be at most 60")
    Integer slotMinutes,

    @NotNull(message = "durationMinutes is required")
    Integer durationMinutes,

    @NotNull(message = "startDate is required")
    String startDate,

    @NotNull(message = "endDate is required")
    String endDate,

    @NotBlank(message = "dailyStartTime is required")
    String dailyStartTime,

    @NotBlank(message = "dailyEndTime is required")
    String dailyEndTime,

    @Size(max = 500, message = "Location must be at most 500 characters")
    String location,

    @Size(max = 500, message = "Meeting URL must be at most 500 characters")
    String meetingUrl,

    String resultsVisibility
) {}
