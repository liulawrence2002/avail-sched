package com.goblin.scheduler.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinParticipantRequest(
    @NotBlank @Size(max = 120) String displayName,
    @Email @Size(max = 320) String email
) {}
