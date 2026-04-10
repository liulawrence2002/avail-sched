package com.goblin.scheduler.web;

import com.goblin.scheduler.dto.AvailabilityItem;
import com.goblin.scheduler.dto.CreateEventResponse;
import com.goblin.scheduler.dto.EventDetailResponse;
import com.goblin.scheduler.dto.FinalSelectionResponse;
import com.goblin.scheduler.dto.JoinParticipantResponse;
import com.goblin.scheduler.dto.ParticipantAvailabilityResponse;
import com.goblin.scheduler.dto.ResultsResponse;
import com.goblin.scheduler.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void createEvent_returnsResponse() throws Exception {
        when(eventService.createEvent(any())).thenReturn(new CreateEventResponse("pub123", "host456", "http://localhost/host/host456"));

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Team Sync","timezone":"America/New_York","slotMinutes":30,
                     "durationMinutes":60,"startDate":"2026-04-01","endDate":"2026-04-02",
                     "dailyStartTime":"09:00","dailyEndTime":"17:00","resultsVisibility":"aggregate_public"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"))
            .andExpect(jsonPath("$.hostToken").value("host456"));
    }

    @Test
    void getEvent_returnsDetail() throws Exception {
        EventDetailResponse response = new EventDetailResponse(
            "pub123", "Title", "Desc", "UTC", 30, 60,
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2),
            LocalTime.of(9, 0), LocalTime.of(17, 0),
            "aggregate_public", List.of(), new EventDetailResponse.StatsView(5, 2), null
        );
        when(eventService.getEvent("pub123")).thenReturn(response);

        mockMvc.perform(get("/api/events/pub123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"))
            .andExpect(jsonPath("$.resultsVisibility").value("aggregate_public"));
    }

    @Test
    void join_returnsTokenAndParticipantLink() throws Exception {
        when(eventService.joinParticipant(eq("pub123"), any())).thenReturn(new JoinParticipantResponse("tok789", "http://localhost/e/pub123?token=tok789", false));

        mockMvc.perform(post("/api/events/pub123/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"displayName":"Alice","email":"alice@example.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantToken").value("tok789"))
            .andExpect(jsonPath("$.participantLink").value("http://localhost/e/pub123?token=tok789"));
    }

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
    void getParticipantAvailability_returnsSavedItems() throws Exception {
        when(eventService.getParticipantAvailability("pub123", "tok789"))
            .thenReturn(new ParticipantAvailabilityResponse(
                "Alice",
                "alice@example.com",
                List.of(new AvailabilityItem(Instant.parse("2026-04-01T09:00:00Z"), 1.0))
            ));

        mockMvc.perform(get("/api/events/pub123/participants/tok789/availability"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.items[0].weight").value(1.0));
    }

    @Test
    void getResults_returnsAggregateResponse() throws Exception {
        when(eventService.getResults("pub123")).thenReturn(new ResultsResponse("pub123", "UTC", 3, 2L, null, false, List.of()));

        mockMvc.perform(get("/api/events/pub123/results"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantCount").value(3))
            .andExpect(jsonPath("$.participantDetailsVisible").value(false));
    }

    @Test
    void getResults_hostOnly_returnsForbidden() throws Exception {
        when(eventService.getResults("pub123")).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Results are only available to the host for this event"));

        mockMvc.perform(get("/api/events/pub123/results"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getHostResults_returnsNamedResponse() throws Exception {
        when(eventService.getHostResults("host456")).thenReturn(new ResultsResponse("pub123", "UTC", 3, 2L, null, true, List.of()));

        mockMvc.perform(get("/api/host/host456/results"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantDetailsVisible").value(true));
    }

    @Test
    void finalizeEvent_returnsResponse() throws Exception {
        Instant slot = Instant.parse("2026-04-01T09:00:00Z");
        when(eventService.finalizeEvent(eq("pub123"), eq("host456"), any()))
            .thenReturn(new FinalSelectionResponse("pub123", slot, Instant.now()));

        mockMvc.perform(post("/api/events/pub123/finalize?hostToken=host456")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"slotStartUtc":"2026-04-01T09:00:00Z"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicId").value("pub123"));
    }

    @Test
    void downloadIcs_returnsCalendarContentType() throws Exception {
        when(eventService.getIcs("pub123")).thenReturn("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n");

        mockMvc.perform(get("/api/events/pub123/final.ics"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/calendar"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"goblin-scheduler.ics\""));
    }
}
