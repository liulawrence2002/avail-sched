package com.goblinscheduler.dto;

public record CreateEventResponse(
    String publicId,
    String hostToken,
    String hostLink
) {}
