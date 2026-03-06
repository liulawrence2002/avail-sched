package com.goblin.scheduler.model;

import java.time.Instant;

public record AvailabilityRecord(
    long participantId,
    long eventId,
    Instant slotStartUtc,
    double weight
) {}

