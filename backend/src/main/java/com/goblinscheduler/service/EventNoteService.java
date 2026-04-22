package com.goblinscheduler.service;

import com.goblinscheduler.dto.NoteResponse;
import com.goblinscheduler.dto.SaveNoteRequest;
import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.EventNote;
import com.goblinscheduler.repository.EventNoteRepository;
import com.goblinscheduler.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class EventNoteService {

    private final EventNoteRepository noteRepository;
    private final EventRepository eventRepository;

    public EventNoteService(EventNoteRepository noteRepository, EventRepository eventRepository) {
        this.noteRepository = noteRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public Optional<NoteResponse> getNoteByEventPublicId(String publicId) {
        Event event = eventRepository.findByPublicId(publicId)
                .orElseThrow(() -> new com.goblinscheduler.exception.NotFoundException("Event not found"));
        return noteRepository.findByEventId(event.getId())
                .map(this::toResponse);
    }

    @Transactional
    public NoteResponse saveOrUpdateNote(String hostToken, SaveNoteRequest request) {
        Event event = eventRepository.findByHostToken(hostToken)
                .orElseThrow(() -> new com.goblinscheduler.exception.NotFoundException("Event not found"));

        Optional<EventNote> existing = noteRepository.findByEventId(event.getId());
        EventNote note;
        if (existing.isPresent()) {
            note = existing.get();
            note.setContent(request.content().trim());
            note.setUpdatedAt(Instant.now());
            noteRepository.update(note);
        } else {
            note = new EventNote();
            note.setEventId(event.getId());
            note.setContent(request.content().trim());
            note.setCreatedAt(Instant.now());
            note.setUpdatedAt(Instant.now());
            noteRepository.save(note);
        }
        return toResponse(note);
    }

    private NoteResponse toResponse(EventNote note) {
        return new NoteResponse(
                note.getId(),
                note.getContent(),
                note.getCreatedAt().toString(),
                note.getUpdatedAt().toString()
        );
    }
}
