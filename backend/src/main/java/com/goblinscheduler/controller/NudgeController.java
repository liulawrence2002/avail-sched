package com.goblinscheduler.controller;

import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import com.goblinscheduler.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class NudgeController {
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EmailService emailService;

    public NudgeController(EventRepository eventRepository, ParticipantRepository participantRepository, EmailService emailService) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.emailService = emailService;
    }

    @PostMapping("/api/events/{publicId}/nudge")
    public ResponseEntity<Map<String, Object>> nudgeNonRespondents(
            @PathVariable String publicId,
            @RequestHeader("X-Host-Token") String hostToken) {
        var eventOpt = eventRepository.findByPublicId(publicId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Event event = eventOpt.get();
        if (!hostToken.equals(event.getHostToken())) {
            return ResponseEntity.status(403).build();
        }

        List<Participant> participants = participantRepository.findByEventId(event.getId());
        Set<Long> respondents = participantRepository.findAllAvailabilityByEventId(event.getId())
                .stream()
                .filter(a -> a.getSlotStart() != null)
                .map(a -> a.getParticipantId())
                .collect(Collectors.toSet());

        int sent = 0;
        for (Participant p : participants) {
            if (!respondents.contains(p.getId())) {
                emailService.sendReminder(event, p);
                sent++;
            }
        }

        return ResponseEntity.ok(Map.of("sent", sent));
    }
}
