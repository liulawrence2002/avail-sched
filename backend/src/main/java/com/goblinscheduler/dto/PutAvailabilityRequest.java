package com.goblinscheduler.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record PutAvailabilityRequest(
    @NotNull(message = "items is required")
    @Size(max = 2000, message = "items must be at most 2000 entries")
    List<AvailabilityItemDto> items
) {}
