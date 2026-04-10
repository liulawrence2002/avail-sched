package com.goblin.scheduler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateAvailabilityRequest(@Valid @NotNull List<AvailabilityItem> items) {}
