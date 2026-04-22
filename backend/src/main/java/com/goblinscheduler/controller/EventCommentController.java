package com.goblinscheduler.controller;

import com.goblinscheduler.dto.CreateCommentRequest;
import com.goblinscheduler.model.EventComment;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.EventCommentRepository;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class EventCommentController {
    private final EventCommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    public EventCommentController(EventCommentRepository commentRepository, EventRepository eventRepository, ParticipantRepository participantRepository) {
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
    }

    @GetMapping("/api/events/{publicId}/comments")
    public ResponseEntity<List<EventComment>> getComments(@PathVariable String publicId) {
        var eventOpt = eventRepository.findByPublicId(publicId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(commentRepository.findByEventId(eventOpt.get().getId()));
    }

    @PostMapping("/api/events/{publicId}/comments")
    public ResponseEntity<EventComment> createComment(
            @PathVariable String publicId,
            @RequestHeader(value = "X-Participant-Token", required = false) String participantToken,
            @RequestHeader(value = "X-Host-Token", required = false) String hostToken,
            @Valid @RequestBody CreateCommentRequest request) {
        var eventOpt = eventRepository.findByPublicId(publicId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var event = eventOpt.get();

        boolean isHost = hostToken != null && hostToken.equals(event.getHostToken());

        String authorName;
        Long participantId = null;
        if (isHost) {
            authorName = event.getTitle() + " (Host)";
        } else if (participantToken != null) {
            var participantOpt = participantRepository.findByToken(participantToken);
            if (participantOpt.isPresent() && participantOpt.get().getEventId().equals(event.getId())) {
                var p = participantOpt.get();
                authorName = p.getDisplayName();
                participantId = p.getId();
            } else {
                return ResponseEntity.status(401).build();
            }
        } else {
            return ResponseEntity.status(401).build();
        }

        EventComment comment = new EventComment();
        comment.setEventId(event.getId());
        comment.setParticipantId(participantId);
        comment.setAuthorName(authorName);
        comment.setContent(request.content().trim());
        return ResponseEntity.ok(commentRepository.save(comment));
    }
}
