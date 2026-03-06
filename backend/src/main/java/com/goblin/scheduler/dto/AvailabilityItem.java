package com.goblin.scheduler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AvailabilityItem(
    @NotNull Instant slotStartUtc,
    @NotNull @Min(0) @Max(1) Double weight
) {}

