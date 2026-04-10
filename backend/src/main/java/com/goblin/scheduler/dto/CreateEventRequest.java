package com.goblin.scheduler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateEventRequest(
    @NotBlank @Size(max = 160) String title,
    @Size(max = 4000) String description,
    @NotBlank String timezone,
    @NotNull @Min(30) @Max(30) Integer slotMinutes,
    @NotNull Integer durationMinutes,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull LocalTime dailyStartTime,
    @NotNull LocalTime dailyEndTime,
    @Size(max = 32) String resultsVisibility) {}
