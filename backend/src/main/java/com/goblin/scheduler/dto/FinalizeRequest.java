package com.goblin.scheduler.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FinalizeRequest(@NotNull Instant slotStartUtc) {}

