package com.goblin.scheduler.dto;

import java.util.List;

public record ParticipantAvailabilityResponse(
    String displayName,
    List<AvailabilityItem> items
) {}
