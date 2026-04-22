package com.goblinscheduler.dto;

public record NoteResponse(
    Long id,
    String content,
    String createdAt,
    String updatedAt
) {}
