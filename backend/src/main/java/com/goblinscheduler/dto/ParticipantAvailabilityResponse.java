package com.goblinscheduler.dto;

import java.util.List;

public record ParticipantAvailabilityResponse(
    String displayName,
    String email,
    List<AvailabilityItemDto> items
) {}
