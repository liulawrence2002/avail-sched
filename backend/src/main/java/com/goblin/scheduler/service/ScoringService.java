package com.goblin.scheduler.service;

import com.goblin.scheduler.config.SchedulingRules;
import com.goblin.scheduler.config.ScoringThresholds;
import com.goblin.scheduler.dto.ResultsResponse;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.Participant;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {

  private final ScoringThresholds thresholds;

  public ScoringService(ScoringThresholds thresholds) {
    this.thresholds = thresholds;
  }

  public List<ResultsResponse.ResultSlot> scoreTopSlots(
      Event event,
      List<Instant> candidateSlots,
      List<Participant> participants,
      Map<Long, Map<Instant, Double>> availabilityMap,
      boolean includeParticipantDetails) {
    int subSlots = event.durationMinutes() / event.slotMinutes();
    double maxScore = Math.max(1.0, participants.size() * subSlots);

    return candidateSlots.stream()
        .map(
            slot -> {
              double score = 0.0;
              int yes = 0;
              int maybe = 0;
              int bribe = 0;
              int no = 0;
              List<String> canAttend = new ArrayList<>();
              List<String> cannotAttend = new ArrayList<>();
              for (Participant participant : participants) {
                double participantScore = 0.0;
                for (int i = 0; i < subSlots; i++) {
                  Instant subSlot = slot.plusSeconds((long) event.slotMinutes() * 60 * i);
                  participantScore +=
                      availabilityMap
                          .getOrDefault(participant.id(), Map.of())
                          .getOrDefault(subSlot, 0.0);
                }
                double normalized = subSlots == 0 ? 0.0 : participantScore / subSlots;
                score += participantScore;
                if (normalized >= thresholds.yesThreshold()) {
                  yes++;
                  if (includeParticipantDetails) {
                    canAttend.add(participant.displayName());
                  }
                } else if (normalized >= thresholds.maybeThreshold()) {
                  maybe++;
                  if (includeParticipantDetails) {
                    canAttend.add(participant.displayName());
                  }
                } else if (normalized >= thresholds.bribeThreshold()) {
                  bribe++;
                  if (includeParticipantDetails) {
                    cannotAttend.add(participant.displayName() + " (snacks may help)");
                  }
                } else {
                  no++;
                  if (includeParticipantDetails) {
                    cannotAttend.add(participant.displayName());
                  }
                }
              }
              return new ResultsResponse.ResultSlot(
                  slot,
                  round(score),
                  round((score / maxScore) * 100.0),
                  yes,
                  maybe,
                  bribe,
                  no,
                  canAttend,
                  cannotAttend);
            })
        .sorted(Comparator.comparingDouble(ResultsResponse.ResultSlot::score).reversed())
        .limit(SchedulingRules.TOP_SLOTS_COUNT)
        .toList();
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
