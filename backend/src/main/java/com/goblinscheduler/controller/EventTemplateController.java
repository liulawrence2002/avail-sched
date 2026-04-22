package com.goblinscheduler.controller;

import com.goblinscheduler.model.EventTemplate;
import com.goblinscheduler.repository.EventTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EventTemplateController {
    private final EventTemplateRepository templateRepository;

    public EventTemplateController(EventTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public record CreateTemplateRequest(
        @NotBlank String name,
        String description,
        String timezone,
        Integer slotMinutes,
        Integer durationMinutes,
        String dailyStartTime,
        String dailyEndTime,
        String resultsVisibility,
        String location,
        String meetingUrl
    ) {}

    @GetMapping("/api/templates")
    public ResponseEntity<List<EventTemplate>> listTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @PostMapping("/api/templates")
    public ResponseEntity<EventTemplate> createTemplate(@Valid @RequestBody CreateTemplateRequest req) {
        EventTemplate t = new EventTemplate();
        t.setName(req.name());
        t.setDescription(req.description());
        t.setTimezone(req.timezone());
        t.setSlotMinutes(req.slotMinutes());
        t.setDurationMinutes(req.durationMinutes());
        t.setDailyStartTime(req.dailyStartTime());
        t.setDailyEndTime(req.dailyEndTime());
        t.setResultsVisibility(req.resultsVisibility());
        t.setLocation(req.location());
        t.setMeetingUrl(req.meetingUrl());
        return ResponseEntity.ok(templateRepository.save(t));
    }

    @GetMapping("/api/templates/{id}")
    public ResponseEntity<EventTemplate> getTemplate(@PathVariable Long id) {
        EventTemplate t = templateRepository.findById(id);
        if (t == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(t);
    }

    @DeleteMapping("/api/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateRepository.delete(id);
        return ResponseEntity.noContent().build();
    }
}
