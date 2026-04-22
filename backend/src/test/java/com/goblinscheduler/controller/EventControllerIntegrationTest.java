package com.goblinscheduler.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("goblin_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullLifecycleTest() throws Exception {
        // 1. Create event
        String createJson = """
            {
                "title": "Team dinner",
                "description": "Optional context",
                "timezone": "America/New_York",
                "slotMinutes": 30,
                "durationMinutes": 60,
                "startDate": "2026-05-01",
                "endDate": "2026-05-03",
                "dailyStartTime": "09:00",
                "dailyEndTime": "18:00",
                "resultsVisibility": "aggregate_public"
            }
            """;

        String createResponse = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.hostToken").exists())
                .andExpect(jsonPath("$.hostLink").exists())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode createNode = objectMapper.readTree(createResponse);
        String publicId = createNode.get("publicId").asText();
        String hostToken = createNode.get("hostToken").asText();

        // 2. Get event detail
        mockMvc.perform(get("/api/events/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.title").value("Team dinner"))
                .andExpect(jsonPath("$.candidateSlotsUtc").isArray())
                .andExpect(jsonPath("$.stats.viewCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.finalSelection").isEmpty());

        // 3. Join participant
        String joinJson = """
            {
                "displayName": "Avery",
                "email": "avery@example.com"
            }
            """;

        String joinResponse = mockMvc.perform(post("/api/events/{publicId}/participants", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantToken").exists())
                .andExpect(jsonPath("$.existingParticipant").value(false))
                .andReturn().getResponse().getContentAsString();

        String participantToken = objectMapper.readTree(joinResponse).get("participantToken").asText();

        // Join same email again -> existing participant
        mockMvc.perform(post("/api/events/{publicId}/participants", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existingParticipant").value(true))
                .andExpect(jsonPath("$.participantToken").value(participantToken));

        // 4. Put availability
        String availJson = """
            {
                "items": [
                    {"slotStartUtc": "2026-05-01T13:00:00Z", "weight": 1.0}
                ]
            }
            """;

        mockMvc.perform(put("/api/events/{publicId}/participants/{token}/availability", publicId, participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availJson))
                .andExpect(status().isNoContent());

        // 5. Get participant availability
        mockMvc.perform(get("/api/events/{publicId}/participants/{token}/availability", publicId, participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Avery"))
                .andExpect(jsonPath("$.items[0].slotStartUtc").value("2026-05-01T13:00:00Z"))
                .andExpect(jsonPath("$.items[0].weight").value(1.0));

        // 6. Public results
        mockMvc.perform(get("/api/events/{publicId}/results", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participantDetailsVisible").value(false))
                .andExpect(jsonPath("$.topSlots").isArray());

        // 7. Host results
        mockMvc.perform(get("/api/host/{hostToken}/results", hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId))
                .andExpect(jsonPath("$.participantDetailsVisible").value(true));

        // 8. Finalize event
        String finalizeJson = """
            {"slotStartUtc": "2026-05-01T13:00:00Z"}
            """;

        mockMvc.perform(post("/api/events/{publicId}/finalize", publicId)
                        .header("X-Host-Token", hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(finalizeJson))
                .andExpect(status().isOk());

        // Finalize again -> 409
        mockMvc.perform(post("/api/events/{publicId}/finalize", publicId)
                        .header("X-Host-Token", hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(finalizeJson))
                .andExpect(status().isConflict());

        // 9. Get final selection
        mockMvc.perform(get("/api/events/{publicId}/final", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalSelection.slotStartUtc").exists())
                .andExpect(jsonPath("$.finalSelection.finalizedAt").exists());

        // 10. Download ICS
        mockMvc.perform(get("/api/events/{publicId}/final.ics", publicId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/calendar")))
                .andExpect(content().string(containsString("BEGIN:VCALENDAR")));
    }

    @Test
    void createEventValidationFails() throws Exception {
        String invalidJson = """
            {
                "title": "",
                "timezone": "InvalidZone",
                "slotMinutes": 5,
                "durationMinutes": 100,
                "startDate": "2026-05-01",
                "endDate": "2026-04-01",
                "dailyStartTime": "09:00",
                "dailyEndTime": "08:00"
            }
            """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void hostOnlyResultsForbidden() throws Exception {
        String createJson = """
            {
                "title": "Secret meeting",
                "timezone": "UTC",
                "slotMinutes": 30,
                "durationMinutes": 60,
                "startDate": "2026-05-01",
                "endDate": "2026-05-01",
                "dailyStartTime": "09:00",
                "dailyEndTime": "18:00",
                "resultsVisibility": "host_only"
            }
            """;

        String resp = mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andReturn().getResponse().getContentAsString();

        String publicId = objectMapper.readTree(resp).get("publicId").asText();

        mockMvc.perform(get("/api/events/{publicId}/results", publicId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEventNotFound() throws Exception {
        mockMvc.perform(get("/api/events/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
