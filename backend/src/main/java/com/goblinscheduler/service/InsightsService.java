package com.goblinscheduler.service;

import com.goblinscheduler.dto.InsightDto;
import com.goblinscheduler.dto.ResultsResponse;
import com.goblinscheduler.dto.SlotResultDto;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class InsightsService {

    private final EventRepository eventRepository;
    private final ResultsService resultsService;

    public InsightsService(EventRepository eventRepository, ResultsService resultsService) {
        this.eventRepository = eventRepository;
        this.resultsService = resultsService;
    }

    public List<InsightDto> generateInsights(List<String> hostTokens) {
        List<Event> events = eventRepository.findByHostTokens(hostTokens);
        List<InsightDto> insights = new ArrayList<>();

        for (Event event : events) {
            if (event.isFinalized() || event.isDeleted()) continue;

            // Low response rate
            if (event.getRespondentCount() == 0 && event.getCreatedAt() != null
                    && Duration.between(event.getCreatedAt(), Instant.now()).toHours() > 24) {
                insights.add(new InsightDto(
                        InsightDto.LOW_RESPONSE, "warning",
                        "No responses yet",
                        String.format("'%s' has been open for over 24 hours with no responses. Consider nudging participants.", event.getTitle()),
                        event.getHostToken(), event.getPublicId(),
                        "Nudge participants", "/host/" + event.getHostToken()
                ));
            } else if (event.getRespondentCount() > 0 && event.getRespondentCount() < 3
                    && event.getCreatedAt() != null
                    && Duration.between(event.getCreatedAt(), Instant.now()).toHours() > 48) {
                insights.add(new InsightDto(
                        InsightDto.LOW_RESPONSE, "info",
                        "Low response rate",
                        String.format("Only %d people have responded to '%s'. Sending a reminder might help.",
                                event.getRespondentCount(), event.getTitle()),
                        event.getHostToken(), event.getPublicId(),
                        "Send reminder", "/host/" + event.getHostToken()
                ));
            }

            // Approaching deadline
            if (event.getDeadline() != null) {
                Duration timeToDeadline = Duration.between(Instant.now(), event.getDeadline());
                if (timeToDeadline.toHours() > 0 && timeToDeadline.toHours() < 24) {
                    insights.add(new InsightDto(
                            InsightDto.APPROACHING_DEADLINE, "warning",
                            "Deadline approaching",
                            String.format("'%s' deadline is in %d hours. %d people have responded so far.",
                                    event.getTitle(), timeToDeadline.toHours(), event.getRespondentCount()),
                            event.getHostToken(), event.getPublicId(),
                            "Review responses", "/host/" + event.getHostToken()
                    ));
                }
            }

            // Ready to finalize (strong consensus)
            if (event.getRespondentCount() >= 2) {
                try {
                    ResultsResponse results = resultsService.getHostResults(event.getHostToken());
                    List<SlotResultDto> topSlots = results.topSlots();
                    if (topSlots != null && !topSlots.isEmpty()) {
                        SlotResultDto best = topSlots.get(0);
                        if (best.percentOfMax() >= 80 && best.yesCount() >= 2 && best.noCount() == 0) {
                            insights.add(new InsightDto(
                                    InsightDto.READY_TO_FINALIZE, "success",
                                    "Strong consensus found",
                                    String.format("'%s' has strong agreement — %d people marked 'definitely' for the top slot with no conflicts. Ready to finalize!",
                                            event.getTitle(), best.yesCount()),
                                    event.getHostToken(), event.getPublicId(),
                                    "Finalize now", "/host/" + event.getHostToken()
                            ));
                        }
                    }
                } catch (Exception e) {
                    // Skip if results can't be loaded
                }
            }

            // Stale event
            if (event.getRespondentCount() == 0 && event.getCreatedAt() != null
                    && Duration.between(event.getCreatedAt(), Instant.now()).toDays() > 7) {
                insights.add(new InsightDto(
                        InsightDto.STALE_EVENT, "info",
                        "Event appears stale",
                        String.format("'%s' was created over a week ago with no responses. Consider deleting it or re-sharing the link.",
                                event.getTitle()),
                        event.getHostToken(), event.getPublicId(),
                        "View event", "/host/" + event.getHostToken()
                ));
            }
        }

        // Cross-event conflict detection
        insights.addAll(detectCrossEventConflicts(events));

        return insights;
    }

    public List<InsightDto> detectCrossEventConflicts(List<Event> events) {
        List<InsightDto> conflicts = new ArrayList<>();
        List<Event> activeEvents = events.stream()
                .filter(e -> !e.isFinalized() && !e.isDeleted() && e.getRespondentCount() > 0)
                .toList();

        if (activeEvents.size() < 2) return conflicts;

        // Compare top slots across events
        Map<Event, List<SlotResultDto>> eventSlots = new LinkedHashMap<>();
        for (Event event : activeEvents) {
            try {
                ResultsResponse results = resultsService.getHostResults(event.getHostToken());
                if (results.topSlots() != null && !results.topSlots().isEmpty()) {
                    eventSlots.put(event, results.topSlots().stream().limit(3).toList());
                }
            } catch (Exception e) {
                // Skip
            }
        }

        List<Event> eventList = new ArrayList<>(eventSlots.keySet());
        for (int i = 0; i < eventList.size(); i++) {
            for (int j = i + 1; j < eventList.size(); j++) {
                Event e1 = eventList.get(i);
                Event e2 = eventList.get(j);
                List<SlotResultDto> slots1 = eventSlots.get(e1);
                List<SlotResultDto> slots2 = eventSlots.get(e2);

                for (SlotResultDto s1 : slots1) {
                    for (SlotResultDto s2 : slots2) {
                        if (s1.slotStartUtc().equals(s2.slotStartUtc())) {
                            conflicts.add(new InsightDto(
                                    InsightDto.CROSS_EVENT_CONFLICT, "warning",
                                    "Potential scheduling conflict",
                                    String.format("Top slots for '%s' and '%s' overlap at the same time. Consider this when finalizing.",
                                            e1.getTitle(), e2.getTitle()),
                                    e1.getHostToken(), e1.getPublicId(),
                                    "Review", "/host/" + e1.getHostToken()
                            ));
                            break;
                        }
                    }
                }
            }
        }

        return conflicts;
    }
}
