package com.goblinscheduler.dto;

import java.math.BigDecimal;

public record AvailabilityItemDto(
    String slotStartUtc,
    BigDecimal weight
) {}
