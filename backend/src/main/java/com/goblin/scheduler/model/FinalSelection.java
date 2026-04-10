package com.goblin.scheduler.model;

import java.time.Instant;

public record FinalSelection(long eventId, Instant slotStartUtc, Instant finalizedAt) {}
