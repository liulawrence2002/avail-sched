package com.goblin.scheduler.web;

import com.goblin.scheduler.dto.*;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    // --- POST /api/events ---

    @Test
    void createEvent_returnsResponse() throws Exception {
        when(eventService.createEvent(any())).thenReturn(new CreateEventResponse("pub123", "host456", "http://localhost/host/host456"));

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Team Sync","timezone":"America/New_York","slotMinutes":30,
                     "durationMinutes":60,"startDate":"2026-04-01","endDate":"2026-04-02",
                     "dailyStartTime":"09:00","dailyEndTime":"17:00"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"))
            .andExpect(jsonPath("$.hostToken").value("host456"));
    }

    @Test
    void createEvent_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"","timezone":"UTC","slotMinutes":30,
                     "durationMinutes":60,"startDate":"2026-04-01","endDate":"2026-04-02",
                     "dailyStartTime":"09:00","dailyEndTime":"17:00"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"timezone":"UTC","slotMinutes":30,
                     "durationMinutes":60,"startDate":"2026-04-01","endDate":"2026-04-02",
                     "dailyStartTime":"09:00","dailyEndTime":"17:00"}
                    """))
            .andExpect(status().isBadRequest());
    }

    // --- GET /api/events/{publicId} ---

    @Test
    void getEvent_returnsDetail() throws Exception {
        var response = new EventDetailResponse(
            "pub123", "Title", "Desc", "UTC", 30, 60,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2),
            LocalTime.of(9, 0), LocalTime.of(17, 0),
            List.of(), new EventDetailResponse.StatsView(5, 2), null
        );
        when(eventService.getEvent("pub123")).thenReturn(response);

        mockMvc.perform(get("/api/events/pub123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"))
            .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void getEvent_notFound_returns404() throws Exception {
        when(eventService.getEvent("nope")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        mockMvc.perform(get("/api/events/nope"))
            .andExpect(status().isNotFound());
    }

    // --- POST /api/events/{publicId}/participants ---

    @Test
    void join_returnsToken() throws Exception {
        when(eventService.joinParticipant(eq("pub123"), any())).thenReturn(new JoinParticipantResponse("tok789"));

        mockMvc.perform(post("/api/events/pub123/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Alice"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantToken").value("tok789"));
    }

    @Test
    void join_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/events/pub123/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":""}
                    """))
            .andExpect(status().isBadRequest());
    }

    // --- PUT /api/events/{publicId}/participants/{token}/availability ---

    @Test
    void updateAvailability_returns204() throws Exception {
        doNothing().when(eventService).updateAvailability(eq("pub123"), eq("tok789"), any());

        mockMvc.perform(put("/api/events/pub123/participants/tok789/availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items":[{"slotStartUtc":"2026-04-01T09:00:00Z","weight":1.0}]}
                    """))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateAvailability_emptyItems_returns400() throws Exception {
        mockMvc.perform(put("/api/events/pub123/participants/tok789/availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items":[]}
                    """))
            .andExpect(status().isBadRequest());
    }

    // --- GET /api/events/{publicId}/results ---

    @Test
    void getResults_returnsResponse() throws Exception {
        when(eventService.getResults("pub123")).thenReturn(new ResultsResponse("pub123", 3, List.of()));

        mockMvc.perform(get("/api/events/pub123/results"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantCount").value(3));
    }

    // --- POST /api/events/{publicId}/finalize ---

    @Test
    void finalizeEvent_returnsResponse() throws Exception {
        Instant slot = Instant.parse("2026-04-01T09:00:00Z");
        Instant now = Instant.now();
        when(eventService.finalizeEvent(eq("pub123"), eq("host456"), any()))
            .thenReturn(new FinalSelectionResponse("pub123", slot, now));

        mockMvc.perform(post("/api/events/pub123/finalize?hostToken=host456")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"slotStartUtc":"2026-04-01T09:00:00Z"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"));
    }

    @Test
    void finalizeEvent_forbidden_returns403() throws Exception {
        when(eventService.finalizeEvent(eq("pub123"), eq("bad"), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid host token"));

        mockMvc.perform(post("/api/events/pub123/finalize?hostToken=bad")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"slotStartUtc":"2026-04-01T09:00:00Z"}
                    """))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/events/{publicId}/final ---

    @Test
    void getFinal_returnsResponse() throws Exception {
        Instant slot = Instant.parse("2026-04-01T09:00:00Z");
        when(eventService.getFinalSelection("pub123")).thenReturn(new FinalSelectionResponse("pub123", slot, Instant.now()));

        mockMvc.perform(get("/api/events/pub123/final"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"));
    }

    @Test
    void getFinal_notFinalized_returns404() throws Exception {
        when(eventService.getFinalSelection("pub123")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));

        mockMvc.perform(get("/api/events/pub123/final"))
            .andExpect(status().isNotFound());
    }

    // --- GET /api/events/{publicId}/final.ics ---

    @Test
    void downloadIcs_returnsCalendarContentType() throws Exception {
        when(eventService.getIcs("pub123")).thenReturn("BEGIN:VCALENDAR\nEND:VCALENDAR");

        mockMvc.perform(get("/api/events/pub123/final.ics"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/calendar"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"goblin-scheduler.ics\""));
    }

    // --- GET /api/host/{hostToken} ---

    @Test
    void hostEvent_returnsEvent() throws Exception {
        Event event = new Event(1L, "pub123", "host456", "Title", null, "UTC", 30, 60,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2),
            LocalTime.of(9, 0), LocalTime.of(17, 0), Instant.now());
        when(eventService.requireEventByHostToken("host456")).thenReturn(event);

        mockMvc.perform(get("/api/host/host456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"));
    }
}
