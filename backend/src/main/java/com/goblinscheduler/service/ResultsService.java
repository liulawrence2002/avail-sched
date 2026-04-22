package com.goblinscheduler.service;

import com.goblinscheduler.dto.*;
import com.goblinscheduler.exception.*;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.repository.ParticipantRepository;
import com.goblinscheduler.util.SlotGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResultsService {

    private final EventService eventService;
    private final ParticipantRepository participantRepository;

    public ResultsService(EventService eventService, ParticipantRepository participantRepository) {
        this.eventService = eventService;
        this.participantRepository = participantRepository;
    }

    @Transactional(readOnly = true)
    public ResultsResponse getPublicResults(String publicId) {
        Event event = eventService.getEventByPublicId(publicId);

        if ("host_only".equals(event.getResultsVisibility())) {
            throw new ForbiddenException("Results are host-only");
        }

        return buildResults(event, false);
    }

    @Transactional(readOnly = true)
    public ResultsResponse getHostResults(String hostToken) {
        Event event = eventService.getEventByHostToken(hostToken);
        return buildResults(event, true);
    }

    private ResultsResponse buildResults(Event event, boolean participantDetailsVisible) {
        List<ParticipantRepository.ParticipantAvailabilityView> rows = participantRepository.findAllAvailabilityByEventId(event.getId());

        Map<Instant, SlotAccumulator> slotMap = new HashMap<>();
        Map<Long, String> participantNames = new HashMap<>();

        for (ParticipantRepository.ParticipantAvailabilityView row : rows) {
            participantNames.put(row.getParticipantId(), row.getDisplayName());
            if (row.getSlotStart() == null || row.getWeight() == null) {
                continue;
            }

            SlotAccumulator acc = slotMap.computeIfAbsent(row.getSlotStart(), k -> new SlotAccumulator());
            BigDecimal weight = row.getWeight();
            acc.score = acc.score.add(weight);

            if (weight.compareTo(BigDecimal.valueOf(1.0)) == 0) {
                acc.yesCount++;
                acc.canAttend.add(row.getDisplayName());
            } else if (weight.compareTo(BigDecimal.valueOf(0.6)) == 0) {
                acc.maybeCount++;
                acc.canAttend.add(row.getDisplayName());
            } else if (weight.compareTo(BigDecimal.valueOf(0.3)) == 0) {
                acc.bribeCount++;
                acc.canAttend.add(row.getDisplayName());
            } else {
                acc.noCount++;
                acc.cannotAttend.add(row.getDisplayName());
            }
        }

        List<SlotResultDto> topSlots = slotMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().score.compareTo(a.getValue().score))
                .limit(5)
                .map(e -> {
                    SlotAccumulator acc = e.getValue();
                    BigDecimal score = acc.score.setScale(1, RoundingMode.HALF_UP);
                    int maxScore = participantNames.size();
                    int percentOfMax = maxScore > 0
                            ? acc.score.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(maxScore), 0, RoundingMode.HALF_UP).intValue()
                            : 0;

                    List<String> canAttend = participantDetailsVisible ? acc.canAttend : List.of();
                    List<String> cannotAttend = participantDetailsVisible ? acc.cannotAttend : List.of();

                    return new SlotResultDto(
                            e.getKey().toString(),
                            score,
                            percentOfMax,
                            acc.yesCount,
                            acc.maybeCount,
                            acc.bribeCount,
                            acc.noCount,
                            canAttend,
                            cannotAttend
                    );
                })
                .toList();

        int participantCount = (int) rows.stream().map(ParticipantRepository.ParticipantAvailabilityView::getParticipantId).distinct().count();
        int respondentCount = event.getRespondentCount();

        FinalSelectionDto finalSelection = null;
        if (event.getFinalSlotStart() != null) {
            finalSelection = new FinalSelectionDto(
                    event.getFinalSlotStart().toString(),
                    event.getFinalizedAt() != null ? event.getFinalizedAt().toString() : null
            );
        }

        return new ResultsResponse(
                event.getPublicId(),
                event.getTimezone(),
                participantCount,
                respondentCount,
                finalSelection,
                participantDetailsVisible,
                topSlots
        );
    }

    private static class SlotAccumulator {
        BigDecimal score = BigDecimal.ZERO;
        int yesCount = 0;
        int maybeCount = 0;
        int bribeCount = 0;
        int noCount = 0;
        List<String> canAttend = new ArrayList<>();
        List<String> cannotAttend = new ArrayList<>();
    }
}
