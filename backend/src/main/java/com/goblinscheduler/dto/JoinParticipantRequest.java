package com.goblinscheduler.dto;

import jakarta.validation.constraints.*;

public record JoinParticipantRequest(
    @NotBlank(message = "displayName is required")
    @Size(max = 120, message = "displayName must be at most 120 characters")
    String displayName,

    @Size(max = 320, message = "Email must be at most 320 characters")
    String email
) {}
