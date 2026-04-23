package com.goblinscheduler.service;

import com.goblinscheduler.dto.ResultsResponse;
import com.goblinscheduler.dto.SlotResultDto;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.AgentActionRepository;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulingAgentService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingAgentService.class);
    private static final Duration MIN_ACTION_INTERVAL = Duration.ofHours(6);

    private final AIService aiService;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final ResultsService resultsService;
    private final AgentActionRepository agentActionRepository;
    private final EmailService emailService;

    public SchedulingAgentService(AIService aiService,
                                   EventRepository eventRepository,
                                   ParticipantRepository participantRepository,
                                   ResultsService resultsService,
                                   AgentActionRepository agentActionRepository,
                                   EmailService emailService) {
        this.aiService = aiService;
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.resultsService = resultsService;
        this.agentActionRepository = agentActionRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void processAgentEnabledEvents() {
        if (!aiService.isAvailable()) return;

        try {
            List<Event> events = eventRepository.findAgentEnabledEvents();
            for (Event event : events) {
                try {
                    processEvent(event);
                } catch (Exception e) {
                    logger.error("Agent processing failed for event {}", event.getPublicId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Agent loop failed", e);
        }
    }

    private void processEvent(Event event) {
        // Rate limit: 1 action per event per 6 hours
        Instant lastAction = agentActionRepository.getLastActionTime(event.getId());
        if (lastAction != null && Duration.between(lastAction, Instant.now()).compareTo(MIN_ACTION_INTERVAL) < 0) {
            return;
        }

        // Build event state for AI
        Map<String, Object> eventState = buildEventState(event);

        // Ask AI what to do
        Map<String, Object> decision = aiService.agentDecide(eventState);
        String action = (String) decision.getOrDefault("action", "none");
        String reason = (String) decision.getOrDefault("reason", "");

        if ("none".equals(action)) {
            logger.debug("Agent: no action needed for event {}", event.getPublicId());
            return;
        }

        // Execute the action
        String result = executeAction(event, action, decision);

        // Log the action
        String payload;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            payload = mapper.writeValueAsString(decision);
        } catch (Exception e) {
            payload = "{}";
        }

        agentActionRepository.save(event.getId(), action, "completed", payload, result);
        logger.info("Agent executed '{}' for event {}: {}", action, event.getPublicId(), reason);
    }

    private String executeAction(Event event, String action, Map<String, Object> decision) {
        switch (action) {
            case "send_reminder" -> {
                List<Participant> participants = participantRepository.findByEventId(event.getId());
                Set<Long> respondentIds = participantRepository.findAllAvailabilityByEventId(event.getId())
                        .stream()
                        .filter(a -> a.getSlotStart() != null)
                        .map(a -> a.getParticipantId())
                        .collect(Collectors.toSet());

                int sent = 0;
                for (Participant p : participants) {
                    if (!respondentIds.contains(p.getId())) {
                        try {
                            emailService.sendReminder(event, p);
                            sent++;
                        } catch (Exception e) {
                            logger.warn("Agent: failed to send reminder to {}", p.getEmail());
                        }
                    }
                }
                return String.format("Sent %d reminders to non-respondents", sent);
            }
            case "suggest_finalize" -> {
                return "Suggested finalization to host. " + decision.getOrDefault("message", "");
            }
            case "status_update" -> {
                return "Status update generated. " + decision.getOrDefault("message", "");
            }
            case "stuck_alert" -> {
                return "Stuck alert generated. " + decision.getOrDefault("message", "");
            }
            default -> {
                return "Unknown action: " + action;
            }
        }
    }

    private Map<String, Object> buildEventState(Event event) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("title", event.getTitle());
        state.put("createdAt", event.getCreatedAt().toString());
        state.put("respondentCount", event.getRespondentCount());
        state.put("isFinalized", event.isFinalized());

        long hoursOld = Duration.between(event.getCreatedAt(), Instant.now()).toHours();
        state.put("hoursOld", hoursOld);

        if (event.getDeadline() != null) {
            long hoursToDeadline = Duration.between(Instant.now(), event.getDeadline()).toHours();
            state.put("hoursToDeadline", hoursToDeadline);
        }

        // Participant info
        List<Participant> participants = participantRepository.findByEventId(event.getId());
        state.put("totalParticipants", participants.size());

        Set<Long> respondentIds = participantRepository.findAllAvailabilityByEventId(event.getId())
                .stream()
                .filter(a -> a.getSlotStart() != null)
                .map(a -> a.getParticipantId())
                .collect(Collectors.toSet());

        List<String> nonRespondents = participants.stream()
                .filter(p -> !respondentIds.contains(p.getId()))
                .map(Participant::getDisplayName)
                .toList();
        state.put("nonRespondents", nonRespondents);

        // Top slot info
        try {
            ResultsResponse results = resultsService.getHostResults(event.getHostToken());
            if (results.topSlots() != null && !results.topSlots().isEmpty()) {
                SlotResultDto best = results.topSlots().get(0);
                state.put("bestSlotYesCount", best.yesCount());
                state.put("bestSlotNoCount", best.noCount());
                state.put("bestSlotPercentOfMax", best.percentOfMax());
            }
        } catch (Exception e) {
            // Skip
        }

        return state;
    }

    public List<Map<String, Object>> getAgentActions(String hostToken, int limit) {
        Event event = eventRepository.findByHostToken(hostToken)
                .orElseThrow(() -> new com.goblinscheduler.exception.NotFoundException("Event not found"));
        return agentActionRepository.findByEventId(event.getId(), limit);
    }
}
