package com.goblin.scheduler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateEventRequest(
    @NotBlank @Size(max = 160) String title,
    @Size(max = 4000) String description,
    @NotBlank
        @Size(max = 64)
        @Pattern(
            // Loose pre-filter for IANA-ish timezone strings: one or more Area/Location
            // segments, each starting with a letter and containing letters, digits, '+', '-',
            // or '_'. The service layer still calls ZoneId.of() for the authoritative check.
            regexp = "^[A-Za-z][A-Za-z0-9+\\-_]*(?:/[A-Za-z0-9+\\-_]+){0,2}$",
            message = "Timezone must be an IANA identifier like America/New_York")
        String timezone,
    @NotNull @Min(15) @Max(60) Integer slotMinutes,
    @NotNull @Min(30) @Max(90) Integer durationMinutes,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull LocalTime dailyStartTime,
    @NotNull LocalTime dailyEndTime,
    @Size(max = 32) String resultsVisibility) {}
