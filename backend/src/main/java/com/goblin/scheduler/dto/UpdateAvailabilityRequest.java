package com.goblin.scheduler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateAvailabilityRequest(
    // A month-long event at 15-minute granularity across a 12-hour day yields roughly 1,440
    // candidate slots, so 2,000 is a comfortable upper bound that still rejects abuse payloads.
    @Valid @NotNull @Size(max = 2000) List<AvailabilityItem> items) {}
