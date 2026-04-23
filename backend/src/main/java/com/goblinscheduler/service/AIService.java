package com.goblinscheduler.service;

import com.goblinscheduler.config.ClaudeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.*;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private static final int MAX_RETRIES = 2;

    private final RestClient geminiRestClient;
    private final ClaudeConfig geminiConfig;

    public AIService(RestClient claudeRestClient, ClaudeConfig claudeConfig) {
        this.geminiRestClient = claudeRestClient;
        this.geminiConfig = claudeConfig;
    }

    public boolean isAvailable() {
        return geminiConfig.isEnabled();
    }

    /**
     * Parse a natural language event description into structured fields.
     */
    public Map<String, Object> parseEventDescription(String text) {
        String systemPrompt = String.format("""
            You are a scheduling assistant. Today's date is %s.
            Parse the user's event description into structured JSON fields.
            Return ONLY valid JSON with these optional fields:
            {
              "title": "string",
              "description": "string or null",
              "startDate": "YYYY-MM-DD",
              "endDate": "YYYY-MM-DD",
              "dailyStartTime": "HH:mm",
              "dailyEndTime": "HH:mm",
              "slotMinutes": 15|30|60,
              "durationMinutes": 30|60|90,
              "location": "string or null",
              "meetingUrl": "string or null"
            }
            Only include fields you can confidently extract. For relative dates like "next Tuesday",
            resolve them to actual dates. If a day range is given like "Tuesday-Thursday", set startDate
            to the Tuesday and endDate to the Thursday. If a time range like "12-2pm" is given, set
            dailyStartTime and dailyEndTime. Infer slotMinutes from context (e.g., "30 min slots" → 30).
            """, LocalDate.now());

        String response = callGemini(systemPrompt, text);
        if (response == null) return Map.of();

        return parseJsonResponse(response);
    }

    /**
     * Generate AI-enhanced reasoning for slot suggestions.
     */
    public String generateSuggestionReasoning(String eventTitle, List<Map<String, Object>> slotData) {
        String systemPrompt = """
            You are a scheduling advisor. Given event details and slot availability data,
            provide a concise, friendly analysis of each time slot. Mention specific participant
            names and patterns. Keep each slot explanation to 1-2 sentences. Return a JSON array
            of objects with "slotStartUtc" and "reasoning" fields.
            """;

        String userMessage = String.format("Event: %s\nSlot data: %s",
                eventTitle, toJson(slotData));

        return callGemini(systemPrompt, userMessage);
    }

    /**
     * Chat with context about an event — returns assistant response.
     */
    public String chat(String systemContext, List<Map<String, String>> messages) {
        String systemPrompt = """
            You are an AI scheduling assistant for Goblin Scheduler. You help event hosts
            manage their events. You can answer questions about participant availability,
            suggest times, draft messages, and provide actionable advice.
            Be concise and helpful. Use the event context provided.

            """ + systemContext;

        return callGeminiWithHistory(systemPrompt, messages);
    }

    /**
     * Generate meeting prep notes for a finalized event.
     */
    public String generateMeetingPrep(String eventTitle, String eventDescription,
                                       List<String> participants, String existingNotes,
                                       String finalTime) {
        String systemPrompt = """
            You are a meeting preparation assistant. Generate well-structured meeting prep notes
            in markdown format. Include:
            - Meeting overview (title, time, attendees)
            - Suggested agenda items (based on title/description)
            - Logistics checklist
            - Action items template
            Keep it professional and concise.
            """;

        String userMessage = String.format(
                "Title: %s\nDescription: %s\nParticipants: %s\nExisting notes: %s\nFinal time: %s",
                eventTitle,
                eventDescription != null ? eventDescription : "None",
                String.join(", ", participants),
                existingNotes != null ? existingNotes : "None",
                finalTime);

        return callGemini(systemPrompt, userMessage);
    }

    /**
     * Generate a follow-up message (confirmation or thank-you).
     */
    public Map<String, String> generateFollowup(String variant, String eventTitle,
                                                  String finalTime, List<String> participants) {
        String systemPrompt = String.format("""
            Generate a %s message for a scheduled meeting. Return JSON with "subject" and "body" fields.
            The body should be ready to copy-paste into email or Slack. Keep it warm but professional.
            Do not use markdown in the body — use plain text.
            """, variant.equals("confirmation") ? "confirmation/calendar invite" : "thank-you/follow-up");

        String userMessage = String.format("Event: %s\nFinal time: %s\nParticipants: %s",
                eventTitle, finalTime, String.join(", ", participants));

        String response = callGemini(systemPrompt, userMessage);
        if (response == null) {
            return Map.of("subject", eventTitle, "body", "");
        }
        return parseJsonStringMap(response);
    }

    /**
     * Agent decision: Given event state, decide what action to take.
     */
    public Map<String, Object> agentDecide(Map<String, Object> eventState) {
        String systemPrompt = """
            You are an autonomous scheduling agent. Given the current state of an event,
            decide if any action should be taken. Return JSON with:
            {
              "action": "none" | "send_reminder" | "suggest_finalize" | "status_update" | "stuck_alert",
              "reason": "why this action",
              "message": "message to include if applicable"
            }
            Only suggest actions that are genuinely helpful. Prefer "none" if the event
            is progressing normally. Consider:
            - Low response rate after significant time
            - Strong consensus on a slot (ready to finalize)
            - Event approaching deadline with no action
            - Stale event with no activity
            """;

        String response = callGemini(systemPrompt, toJson(eventState));
        if (response == null) {
            return Map.of("action", "none", "reason", "AI unavailable");
        }
        return parseJsonResponse(response);
    }

    /**
     * Parse a natural language recurrence pattern into concrete dates.
     */
    public List<Map<String, String>> parseRecurrence(String text, String timezone) {
        String systemPrompt = String.format("""
            You are a date parser. Today is %s, timezone %s.
            Parse the recurrence pattern into concrete event dates.
            Return a JSON array of objects with "startDate" (YYYY-MM-DD) and "endDate" (YYYY-MM-DD) fields.
            Each object represents one occurrence. Resolve all relative dates.
            Examples:
            - "every Friday for 4 weeks" → 4 objects, each with startDate = endDate = that Friday
            - "every other Tuesday from May to June" → objects for alternating Tuesdays
            """, LocalDate.now(), timezone);

        String response = callGemini(systemPrompt, text);
        if (response == null) return List.of();

        try {
            return parseJsonList(response);
        } catch (Exception e) {
            logger.warn("Failed to parse recurrence response", e);
            return List.of();
        }
    }

    // ── Gemini API helpers ───────────────────────────────────────────────────

    private String callGemini(String systemPrompt, String userMessage) {
        if (!geminiConfig.isEnabled()) {
            logger.debug("Gemini API not configured, skipping AI call");
            return null;
        }

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> body = buildGeminiRequest(systemPrompt,
                        List.of(Map.of("role", "user", "content", userMessage)));

                String uri = String.format("/v1beta/models/%s:generateContent?key=%s",
                        geminiConfig.getModel(), geminiConfig.getApiKey());

                String response = geminiRestClient.post()
                        .uri(uri)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                return extractGeminiText(response);
            } catch (Exception e) {
                logger.warn("Gemini API call failed (attempt {}): {}", attempt + 1, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    logger.error("Gemini API call exhausted retries", e);
                    return null;
                }
            }
        }
        return null;
    }

    private String callGeminiWithHistory(String systemPrompt, List<Map<String, String>> messages) {
        if (!geminiConfig.isEnabled()) return null;

        try {
            Map<String, Object> body = buildGeminiRequest(systemPrompt, messages);

            String uri = String.format("/v1beta/models/%s:generateContent?key=%s",
                    geminiConfig.getModel(), geminiConfig.getApiKey());

            String response = geminiRestClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractGeminiText(response);
        } catch (Exception e) {
            logger.error("Gemini chat API call failed", e);
            return null;
        }
    }

    /**
     * Build Gemini API request body.
     * Format: { systemInstruction: {parts: [{text}]}, contents: [{role, parts: [{text}]}], generationConfig }
     * Gemini uses "model" instead of "assistant" for the AI role.
     */
    private Map<String, Object> buildGeminiRequest(String systemPrompt, List<Map<String, String>> messages) {
        // System instruction
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );

        // Convert messages to Gemini format
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            String role = msg.get("role");
            // Gemini uses "model" for assistant role, "user" for user
            String geminiRole = "assistant".equals(role) ? "model" : "user";
            contents.add(Map.of(
                    "role", geminiRole,
                    "parts", List.of(Map.of("text", msg.get("content")))
            ));
        }

        // Generation config
        Map<String, Object> generationConfig = Map.of(
                "maxOutputTokens", geminiConfig.getMaxTokens()
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", systemInstruction);
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);
        return body;
    }

    /**
     * Extract text from Gemini API response JSON.
     * Response format: {"candidates": [{"content": {"parts": [{"text": "..."}]}}]}
     */
    private String extractGeminiText(String responseJson) {
        if (responseJson == null) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = mapper.readValue(responseJson, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            logger.warn("Failed to parse Gemini response: {}", e.getMessage());
            // Fallback to manual extraction
            return extractTextFallback(responseJson);
        }
    }

    /**
     * Fallback text extraction if Jackson parsing fails.
     */
    private String extractTextFallback(String responseJson) {
        int textStart = responseJson.indexOf("\"text\"");
        if (textStart == -1) return null;
        int colonPos = responseJson.indexOf(":", textStart);
        if (colonPos == -1) return null;
        int quoteStart = responseJson.indexOf("\"", colonPos + 1);
        if (quoteStart == -1) return null;

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = quoteStart + 1; i < responseJson.length(); i++) {
            char c = responseJson.charAt(i);
            if (escaped) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> { sb.append('\\'); sb.append(c); }
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ── JSON parsing helpers ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String text) {
        if (text == null) return Map.of();
        String json = extractJsonBlock(text);
        if (json == null) return Map.of();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            logger.warn("Failed to parse JSON response: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<String, String> parseJsonStringMap(String text) {
        Map<String, Object> map = parseJsonResponse(text);
        Map<String, String> result = new HashMap<>();
        map.forEach((k, v) -> result.put(k, v != null ? v.toString() : ""));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseJsonList(String text) {
        if (text == null) return List.of();
        String json = extractJsonBlock(text);
        if (json == null) return List.of();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, List.class);
        } catch (Exception e) {
            logger.warn("Failed to parse JSON list: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractJsonBlock(String text) {
        if (text == null) return null;
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');
        int start;
        char openChar, closeChar;

        if (objStart >= 0 && (arrStart < 0 || objStart < arrStart)) {
            start = objStart;
            openChar = '{';
            closeChar = '}';
        } else if (arrStart >= 0) {
            start = arrStart;
            openChar = '[';
            closeChar = ']';
        } else {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == openChar) depth++;
            else if (c == closeChar) { depth--; if (depth == 0) return text.substring(start, i + 1); }
        }
        return null;
    }

    private String toJson(Object obj) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
