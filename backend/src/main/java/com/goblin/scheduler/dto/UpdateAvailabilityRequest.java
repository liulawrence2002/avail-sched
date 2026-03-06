package com.goblin.scheduler.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateAvailabilityRequest(@Valid @NotEmpty List<AvailabilityItem> items) {}

