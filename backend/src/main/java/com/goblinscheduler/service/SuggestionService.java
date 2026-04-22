package com.goblinscheduler.service;

import com.goblinscheduler.dto.*;
import com.goblinscheduler.model.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SuggestionService {

    private final ResultsService resultsService;

    public SuggestionService(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    @Transactional(readOnly = true)
    public List<SlotSuggestionDto> getSuggestions(String hostToken) {
        ResultsResponse results = resultsService.getHostResults(hostToken);

        List<SlotResultDto> slots = results.topSlots();
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }

        int totalParticipants = results.participantCount();

        return slots.stream()
                .limit(3)
                .map(slot -> {
                    int confidence = computeConfidence(slot, totalParticipants);
                    String reasoning = buildReasoning(slot, totalParticipants, confidence);
                    return new SlotSuggestionDto(
                            slot.slotStartUtc(),
                            slot.score(),
                            slot.percentOfMax(),
                            confidence,
                            reasoning,
                            slot.yesCount(),
                            slot.maybeCount(),
                            slot.bribeCount(),
                            slot.noCount()
                    );
                })
                .sorted((a, b) -> Integer.compare(b.confidenceScore(), a.confidenceScore()))
                .toList();
    }

    private int computeConfidence(SlotResultDto slot, int totalParticipants) {
        if (totalParticipants == 0) return 0;

        double base = slot.percentOfMax();
        double yesBonus = slot.yesCount() * 5.0;
        double maybeBonus = slot.maybeCount() * 2.0;
        double noPenalty = slot.noCount() * 10.0;

        double raw = base + yesBonus + maybeBonus - noPenalty;
        return Math.max(0, Math.min(100, (int) Math.round(raw)));
    }

    private String buildReasoning(SlotResultDto slot, int totalParticipants, int confidence) {
        StringBuilder sb = new StringBuilder();

        int available = slot.yesCount() + slot.maybeCount() + slot.bribeCount();
        sb.append(available).append(" of ").append(totalParticipants).append(" can attend");

        if (slot.yesCount() > 0) {
            sb.append(" (").append(slot.yesCount()).append(" definitely");
            if (slot.maybeCount() > 0) {
                sb.append(", ").append(slot.maybeCount()).append(" probably");
            }
            sb.append(")");
        } else if (slot.maybeCount() > 0) {
            sb.append(" (").append(slot.maybeCount()).append(" probably, ").append(slot.bribeCount()).append(" if needed)");
        }

        if (slot.noCount() > 0) {
            sb.append(". ").append(slot.noCount()).append(" cannot make it");
        }

        if (confidence >= 85) {
            sb.append(". Strong consensus — this is your best bet.");
        } else if (confidence >= 60) {
            sb.append(". Good availability with minor conflicts.");
        } else if (confidence >= 40) {
            sb.append(". Moderate availability — some key people may miss it.");
        } else {
            sb.append(". Low availability — consider expanding your date range.");
        }

        return sb.toString();
    }
}
