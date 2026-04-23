package com.goblinscheduler.dto;

public record InsightDto(
    String type,
    String severity,
    String title,
    String message,
    String hostToken,
    String publicId,
    String actionLabel,
    String actionUrl
) {
    public static final String LOW_RESPONSE = "LOW_RESPONSE";
    public static final String READY_TO_FINALIZE = "READY_TO_FINALIZE";
    public static final String APPROACHING_DEADLINE = "APPROACHING_DEADLINE";
    public static final String STALE_EVENT = "STALE_EVENT";
    public static final String CROSS_EVENT_CONFLICT = "CROSS_EVENT_CONFLICT";
}
