package com.goblin.scheduler.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class EventFlowIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetDatabase() {
    jdbcTemplate.execute("TRUNCATE TABLE events RESTART IDENTITY CASCADE");
  }

  @Test
  void eventLifecycleWorksAgainstPostgres() throws Exception {
    String createResponse =
        mockMvc
            .perform(
                post("/api/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                    {
                      "title": "Integration Test Event",
                      "description": "Verify the full flow",
                      "timezone": "UTC",
                      "slotMinutes": 30,
                      "durationMinutes": 60,
                      "startDate": "2026-04-01",
                      "endDate": "2026-04-01",
                      "dailyStartTime": "09:00",
                      "dailyEndTime": "12:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").isNotEmpty())
            .andExpect(jsonPath("$.hostToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode created = objectMapper.readTree(createResponse);
    String publicId = created.get("publicId").asText();
    String hostToken = created.get("hostToken").asText();
    assertThat(created.get("hostLink").asText()).endsWith("/host/" + hostToken);

    String eventResponse =
        mockMvc
            .perform(get("/api/events/{publicId}", publicId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value(publicId))
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode event = objectMapper.readTree(eventResponse);
    JsonNode candidateSlots = event.get("candidateSlotsUtc");
    assertThat(candidateSlots.size()).isGreaterThan(1);
    String firstSlot = candidateSlots.get(0).asText();
    String secondSlot = candidateSlots.get(1).asText();

    String joinResponse =
        mockMvc
            .perform(
                post("/api/events/{publicId}/participants", publicId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                    {
                      "displayName": "Avery"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String participantToken = objectMapper.readTree(joinResponse).get("participantToken").asText();

    mockMvc
        .perform(
            put(
                    "/api/events/{publicId}/participants/{token}/availability",
                    publicId,
                    participantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "items": [
                        {"slotStartUtc": "%s", "weight": 1.0},
                        {"slotStartUtc": "%s", "weight": 1.0}
                      ]
                    }
                    """
                        .formatted(firstSlot, secondSlot)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(
                "/api/events/{publicId}/participants/{token}/availability",
                publicId,
                participantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Avery"))
        .andExpect(jsonPath("$.items.length()").value(2));

    mockMvc
        .perform(get("/api/events/{publicId}", publicId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stats.respondentCount").value(1));

    mockMvc
        .perform(
            put(
                    "/api/events/{publicId}/participants/{token}/availability",
                    publicId,
                    participantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "items": []
                    }
                    """))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(
                "/api/events/{publicId}/participants/{token}/availability",
                publicId,
                participantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));

    mockMvc
        .perform(get("/api/events/{publicId}", publicId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stats.respondentCount").value(0));

    mockMvc
        .perform(get("/api/events/{publicId}/results", publicId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value(publicId))
        .andExpect(jsonPath("$.participantCount").value(1))
        .andExpect(jsonPath("$.topSlots[0].slotStartUtc").isNotEmpty());

    mockMvc
        .perform(
            post("/api/events/{publicId}/finalize", publicId)
                .queryParam("hostToken", hostToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "slotStartUtc": "%s"
                    }
                    """
                        .formatted(firstSlot)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value(publicId))
        .andExpect(jsonPath("$.slotStartUtc").value(firstSlot));

    mockMvc
        .perform(get("/api/events/{publicId}/final", publicId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slotStartUtc").value(firstSlot));

    mockMvc
        .perform(get("/api/events/{publicId}/final.ics", publicId))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/calendar"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")));

    mockMvc
        .perform(get("/api/host/{hostToken}", hostToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicId").value(publicId))
        .andExpect(jsonPath("$.stats.respondentCount").value(0));

    mockMvc
        .perform(
            post("/api/events/{publicId}/finalize", publicId)
                .queryParam("hostToken", hostToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "slotStartUtc": "%s"
                    }
                    """
                        .formatted(firstSlot)))
        .andExpect(status().isConflict());
  }
}
