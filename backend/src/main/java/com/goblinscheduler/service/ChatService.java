package com.goblinscheduler.service;

import com.goblinscheduler.dto.ResultsResponse;
import com.goblinscheduler.dto.SlotResultDto;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.AIChatRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final int MAX_HISTORY = 20;

    private final AIService aiService;
    private final AIChatRepository chatRepository;
    private final EventService eventService;
    private final ResultsService resultsService;
    private final ParticipantRepository participantRepository;

    public ChatService(AIService aiService,
                       AIChatRepository chatRepository,
                       EventService eventService,
                       ResultsService resultsService,
                       ParticipantRepository participantRepository) {
        this.aiService = aiService;
        this.chatRepository = chatRepository;
        this.eventService = eventService;
        this.resultsService = resultsService;
        this.participantRepository = participantRepository;
    }

    public Map<String, Object> chat(String hostToken, String userMessage) {
        if (!aiService.isAvailable()) {
            return Map.of("role", "assistant", "content",
                    "AI assistant is currently unavailable. Please check that the API key is configured.");
        }

        Event event = eventService.getEventByHostToken(hostToken);
        Long eventId = event.getId();

        // Save user message
        chatRepository.saveMessage(eventId, "user", userMessage);

        // Build context
        String context = buildEventContext(event);

        // Load history
        List<Map<String, String>> history = chatRepository.getHistory(eventId, MAX_HISTORY);

        // Call AI
        String response = aiService.chat(context, history);
        if (response == null) {
            response = "I'm having trouble connecting to the AI service right now. Please try again in a moment.";
        }

        // Save assistant response
        chatRepository.saveMessage(eventId, "assistant", response);

        return Map.of("role", "assistant", "content", response);
    }

    public List<Map<String, String>> getHistory(String hostToken) {
        Event event = eventService.getEventByHostToken(hostToken);
        return chatRepository.getHistory(event.getId(), MAX_HISTORY);
    }

    private String buildEventContext(Event event) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Event: ").append(event.getTitle()).append("\n");
        if (event.getDescription() != null) {
            ctx.append("Description: ").append(event.getDescription()).append("\n");
        }
        ctx.append("Date range: ").append(event.getStartDate()).append(" to ").append(event.getEndDate()).append("\n");
        ctx.append("Time window: ").append(event.getDailyStartTime()).append(" - ").append(event.getDailyEndTime()).append("\n");
        ctx.append("Timezone: ").append(event.getTimezone()).append("\n");
        ctx.append("Status: ").append(event.isFinalized() ? "Finalized" : "Active").append("\n");

        if (event.getDeadline() != null) {
            ctx.append("Deadline: ").append(formatInstant(event.getDeadline(), event.getTimezone())).append("\n");
        }

        // Participants
        List<Participant> participants = participantRepository.findByEventId(event.getId());
        ctx.append("\nParticipants (").append(participants.size()).append(" total):\n");

        Set<Long> respondentIds = participantRepository.findAllAvailabilityByEventId(event.getId())
                .stream()
                .filter(a -> a.getSlotStart() != null)
                .map(a -> a.getParticipantId())
                .collect(Collectors.toSet());

        for (Participant p : participants) {
            boolean responded = respondentIds.contains(p.getId());
            ctx.append("- ").append(p.getDisplayName())
                    .append(responded ? " (responded)" : " (not responded)")
                    .append("\n");
        }

        // Results summary
        try {
            ResultsResponse results = resultsService.getHostResults(event.getHostToken());
            if (results.topSlots() != null && !results.topSlots().isEmpty()) {
                ctx.append("\nTop slots:\n");
                for (int i = 0; i < Math.min(3, results.topSlots().size()); i++) {
                    SlotResultDto slot = results.topSlots().get(i);
                    ctx.append(String.format("#%d: %s — %d yes, %d maybe, %d no",
                            i + 1,
                            formatInstant(Instant.parse(slot.slotStartUtc()), event.getTimezone()),
                            slot.yesCount(), slot.maybeCount(), slot.noCount()));
                    if (slot.canAttend() != null && !slot.canAttend().isEmpty()) {
                        ctx.append(" (can attend: ").append(String.join(", ", slot.canAttend())).append(")");
                    }
                    ctx.append("\n");
                }
            }
        } catch (Exception e) {
            // Skip results if unavailable
        }

        if (event.isFinalized()) {
            ctx.append("\nFinalized time: ").append(formatInstant(event.getFinalSlotStart(), event.getTimezone())).append("\n");
        }

        return ctx.toString();
    }

    private String formatInstant(Instant instant, String timezone) {
        try {
            return instant.atZone(ZoneId.of(timezone))
                    .format(DateTimeFormatter.ofPattern("EEEE, MMM d 'at' h:mm a"));
        } catch (Exception e) {
            return instant.toString();
        }
    }
}
