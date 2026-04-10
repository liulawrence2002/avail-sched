package com.goblin.scheduler.dto;

import java.time.Instant;
import java.util.List;

public record ResultsResponse(
    String publicId,
    String timezone,
    int participantCount,
    long respondentCount,
    FinalView finalSelection,
    boolean participantDetailsVisible,
    List<ResultSlot> topSlots) {
  public record FinalView(Instant slotStartUtc, Instant finalizedAt) {}

  public record ResultSlot(
      Instant slotStartUtc,
      double score,
      double percentOfMax,
      int yesCount,
      int maybeCount,
      int bribeCount,
      int noCount,
      List<String> canAttend,
      List<String> cannotAttend) {}
}
