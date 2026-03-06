package com.goblin.scheduler.dto;

import java.time.Instant;

public record FinalSelectionResponse(
    String publicId,
    Instant slotStartUtc,
    Instant finalizedAt
) {}

