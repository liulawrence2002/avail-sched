package com.goblinscheduler.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultsResponse(
    String publicId,
    String timezone,
    int participantCount,
    int respondentCount,
    FinalSelectionDto finalSelection,
    boolean participantDetailsVisible,
    List<SlotResultDto> topSlots
) {}
