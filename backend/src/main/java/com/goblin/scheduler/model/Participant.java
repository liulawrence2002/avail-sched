package com.goblin.scheduler.model;

import java.time.Instant;

public record Participant(
    long id,
    long eventId,
    String token,
    String displayName,
    String email,
    Instant createdAt
) {}
