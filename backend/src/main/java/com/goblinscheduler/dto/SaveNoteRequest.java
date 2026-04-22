package com.goblinscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveNoteRequest(
    @NotBlank(message = "Content is required")
    @Size(max = 20000, message = "Content must be at most 20000 characters")
    String content
) {}
