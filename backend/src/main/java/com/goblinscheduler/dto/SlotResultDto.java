package com.goblinscheduler.dto;

import java.math.BigDecimal;
import java.util.List;

public record SlotResultDto(
    String slotStartUtc,
    BigDecimal score,
    int percentOfMax,
    int yesCount,
    int maybeCount,
    int bribeCount,
    int noCount,
    List<String> canAttend,
    List<String> cannotAttend
) {}
