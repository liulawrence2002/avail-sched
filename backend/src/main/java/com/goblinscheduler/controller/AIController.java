package com.goblinscheduler.controller;

import com.goblinscheduler.dto.InsightDto;
import com.goblinscheduler.dto.SlotSuggestionDto;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.ParticipantRepository;
import com.goblinscheduler.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AIController {

    private final AIService aiService;
    private final SuggestionService suggestionService;
    private final InsightsService insightsService;
    private final ChatService chatService;
    private final EventService eventService;
    private final SchedulingAgentService schedulingAgentService;
    private final ParticipantRepository participantRepository;

    public AIController(AIService aiService,
                        SuggestionService suggestionService,
                        InsightsService insightsService,
                        ChatService chatService,
                        EventService eventService,
                        SchedulingAgentService schedulingAgentService,
                        ParticipantRepository participantRepository) {
        this.aiService = aiService;
        this.suggestionService = suggestionService;
        this.insightsService = insightsService;
        this.chatService = chatService;
        this.eventService = eventService;
        this.schedulingAgentService = schedulingAgentService;
        this.participantRepository = participantRepository;
    }

    // ── Phase 1.1: Natural Language Event Creation ───────────────────────────

    @PostMapping("/ai/parse-event")
    public ResponseEntity<?> parseEvent(@RequestBody Map<String, String> request) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "parsed", Map.of()));
        }
        String text = request.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Text is required"));
        }
        Map<String, Object> parsed = aiService.parseEventDescription(text);
        return ResponseEntity.ok(Map.of("available", true, "parsed", parsed));
    }

    // ── Phase 1.2: AI-Enhanced Suggestion Reasoning ─────────────────────────

    @GetMapping("/host/{hostToken}/ai-suggestions")
    public ResponseEntity<?> getAISuggestions(@PathVariable String hostToken) {
        // Get algorithmic suggestions first
        List<SlotSuggestionDto> suggestions = suggestionService.getSuggestions(hostToken);

        if (!aiService.isAvailable() || suggestions.isEmpty()) {
            return ResponseEntity.ok(Map.of("aiEnhanced", false, "suggestions", suggestions));
        }

        // Build slot data for AI
        Event event = eventService.getEventByHostToken(hostToken);
        List<Map<String, Object>> slotData = suggestions.stream()
                .map(s -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("slotStartUtc", s.slotStartUtc());
                    map.put("yesCount", s.yesCount());
                    map.put("maybeCount", s.maybeCount());
                    map.put("bribeCount", s.bribeCount());
                    map.put("noCount", s.noCount());
                    map.put("confidenceScore", s.confidenceScore());
                    map.put("reasoning", s.reasoning());
                    return map;
                })
                .toList();

        String aiReasoning = aiService.generateSuggestionReasoning(event.getTitle(), slotData);

        // Try to merge AI reasoning into suggestions
        if (aiReasoning != null) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> aiSlots = mapper.readValue(
                        extractJsonArray(aiReasoning), List.class);

                List<SlotSuggestionDto> enhanced = new java.util.ArrayList<>();
                for (SlotSuggestionDto s : suggestions) {
                    String newReasoning = s.reasoning();
                    for (Map<String, Object> aiSlot : aiSlots) {
                        if (s.slotStartUtc().equals(aiSlot.get("slotStartUtc"))) {
                            newReasoning = (String) aiSlot.getOrDefault("reasoning", s.reasoning());
                            break;
                        }
                    }
                    enhanced.add(new SlotSuggestionDto(
                            s.slotStartUtc(), s.score(), s.percentOfMax(),
                            s.confidenceScore(), newReasoning,
                            s.yesCount(), s.maybeCount(), s.bribeCount(), s.noCount()
                    ));
                }
                return ResponseEntity.ok(Map.of("aiEnhanced", true, "suggestions", enhanced));
            } catch (Exception e) {
                // Fall back to algorithmic suggestions
            }
        }

        return ResponseEntity.ok(Map.of("aiEnhanced", false, "suggestions", suggestions));
    }

    // ── Phase 1.3: Dashboard Insights ───────────────────────────────────────

    @PostMapping("/insights")
    public ResponseEntity<List<InsightDto>> getInsights(@RequestBody Map<String, List<String>> request) {
        List<String> hostTokens = request.get("hostTokens");
        if (hostTokens == null || hostTokens.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(insightsService.generateInsights(hostTokens));
    }

    // ── Phase 2.1: AI Chat ──────────────────────────────────────────────────

    @PostMapping("/host/{hostToken}/chat")
    public ResponseEntity<?> chat(@PathVariable String hostToken, @RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message is required"));
        }
        Map<String, Object> response = chatService.chat(hostToken, message);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/host/{hostToken}/chat")
    public ResponseEntity<List<Map<String, String>>> getChatHistory(@PathVariable String hostToken) {
        return ResponseEntity.ok(chatService.getHistory(hostToken));
    }

    // ── Phase 2.2: Meeting Prep Notes ───────────────────────────────────────

    @PostMapping("/host/{hostToken}/generate-prep")
    public ResponseEntity<?> generatePrepNotes(@PathVariable String hostToken) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "content", ""));
        }

        Event event = eventService.getEventByHostToken(hostToken);
        List<Participant> participants = participantRepository.findByEventId(event.getId());
        List<String> names = participants.stream().map(Participant::getDisplayName).toList();

        String existingNotes = null;
        try {
            var notesRes = eventService.getEventByHostToken(hostToken);
            // Notes are loaded separately, pass null for now
        } catch (Exception e) { /* skip */ }

        String finalTime = event.getFinalSlotStart() != null ? event.getFinalSlotStart().toString() : "Not yet finalized";

        String content = aiService.generateMeetingPrep(
                event.getTitle(), event.getDescription(), names, existingNotes, finalTime);

        if (content == null) {
            return ResponseEntity.ok(Map.of("available", false, "content", ""));
        }

        return ResponseEntity.ok(Map.of("available", true, "content", content));
    }

    // ── Phase 2.3: Follow-up Messages ───────────────────────────────────────

    @PostMapping("/host/{hostToken}/generate-followup")
    public ResponseEntity<?> generateFollowup(@PathVariable String hostToken,
                                               @RequestBody Map<String, String> request) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "subject", "", "body", ""));
        }

        String variant = request.getOrDefault("variant", "confirmation");
        Event event = eventService.getEventByHostToken(hostToken);
        List<Participant> participants = participantRepository.findByEventId(event.getId());
        List<String> names = participants.stream().map(Participant::getDisplayName).toList();
        String finalTime = event.getFinalSlotStart() != null ? event.getFinalSlotStart().toString() : "";

        Map<String, String> result = aiService.generateFollowup(variant, event.getTitle(), finalTime, names);
        return ResponseEntity.ok(Map.of(
                "available", true,
                "subject", result.getOrDefault("subject", event.getTitle()),
                "body", result.getOrDefault("body", "")
        ));
    }

    // ── Phase 3.1: Agent Actions ────────────────────────────────────────────

    @GetMapping("/host/{hostToken}/agent-actions")
    public ResponseEntity<List<Map<String, Object>>> getAgentActions(@PathVariable String hostToken) {
        return ResponseEntity.ok(schedulingAgentService.getAgentActions(hostToken, 20));
    }

    // ── Phase 3.3: Natural Language Recurrence ──────────────────────────────

    @PostMapping("/ai/parse-recurrence")
    public ResponseEntity<?> parseRecurrence(@RequestBody Map<String, String> request) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "dates", List.of()));
        }
        String text = request.get("text");
        String timezone = request.getOrDefault("timezone", "UTC");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Text is required"));
        }
        List<Map<String, String>> dates = aiService.parseRecurrence(text, timezone);
        return ResponseEntity.ok(Map.of("available", true, "dates", dates));
    }

    // ── Phase 3.3: Event Series Creation ───────────────────────────────────

    @PostMapping("/ai/create-series")
    public ResponseEntity<?> createEventSeries(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) request.get("eventData");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> dates = (List<Map<String, String>>) request.get("dates");

        if (eventData == null || dates == null || dates.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "eventData and dates are required"));
        }

        var baseRequest = new com.goblinscheduler.dto.CreateEventRequest(
                (String) eventData.get("title"),
                (String) eventData.get("description"),
                (String) eventData.get("timezone"),
                ((Number) eventData.get("slotMinutes")).intValue(),
                ((Number) eventData.get("durationMinutes")).intValue(),
                (String) eventData.get("startDate"),
                (String) eventData.get("endDate"),
                (String) eventData.get("dailyStartTime"),
                (String) eventData.get("dailyEndTime"),
                (String) eventData.get("location"),
                (String) eventData.get("meetingUrl"),
                (String) eventData.get("resultsVisibility"),
                (String) eventData.get("deadline"),
                eventData.get("autoFinalize") != null ? (Boolean) eventData.get("autoFinalize") : null,
                eventData.get("agentEnabled") != null ? (Boolean) eventData.get("agentEnabled") : null
        );

        var responses = eventService.createEventSeries(baseRequest, dates);
        return ResponseEntity.ok(responses);
    }

    // ── AI Status ───────────────────────────────────────────────────────────

    @GetMapping("/ai/status")
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        return ResponseEntity.ok(Map.of("available", aiService.isAvailable()));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String extractJsonArray(String text) {
        if (text == null) return "[]";
        int start = text.indexOf('[');
        if (start == -1) return "[]";
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return text.substring(start, i + 1); }
        }
        return "[]";
    }
}
