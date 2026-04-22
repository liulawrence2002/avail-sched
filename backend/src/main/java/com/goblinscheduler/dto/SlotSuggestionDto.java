package com.goblinscheduler.dto;

import java.math.BigDecimal;

public record SlotSuggestionDto(
    String slotStartUtc,
    BigDecimal score,
    int percentOfMax,
    int confidenceScore,
    String reasoning,
    int yesCount,
    int maybeCount,
    int bribeCount,
    int noCount
) {}
