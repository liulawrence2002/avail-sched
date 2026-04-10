package com.goblin.scheduler.service;

import com.goblin.scheduler.config.AppProperties;
import com.goblin.scheduler.dto.AvailabilityItem;
import com.goblin.scheduler.dto.JoinParticipantRequest;
import com.goblin.scheduler.dto.JoinParticipantResponse;
import com.goblin.scheduler.dto.ParticipantAvailabilityResponse;
import com.goblin.scheduler.dto.UpdateAvailabilityRequest;
import com.goblin.scheduler.model.AvailabilityRecord;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.Participant;
import com.goblin.scheduler.repo.AvailabilityRepository;
import com.goblin.scheduler.repo.EventStatsRepository;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import com.goblin.scheduler.repo.ParticipantRepository;
import com.goblin.scheduler.util.TextSanitizer;
import com.goblin.scheduler.util.TokenGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles participant join, availability read, and availability update. Pulled out of the old
 * {@code EventService} in Phase 2.2.
 */
@Service
public class ParticipantService {

  private final EventQueryService eventQueryService;
  private final ParticipantRepository participantRepository;
  private final AvailabilityRepository availabilityRepository;
  private final FinalSelectionRepository finalSelectionRepository;
  private final EventStatsRepository eventStatsRepository;
  private final SlotService slotService;
  private final TokenGenerator tokenGenerator;
  private final ResultCache resultCache;
  private final AppProperties appProperties;

  public ParticipantService(
      EventQueryService eventQueryService,
      ParticipantRepository participantRepository,
      AvailabilityRepository availabilityRepository,
      FinalSelectionRepository finalSelectionRepository,
      EventStatsRepository eventStatsRepository,
      SlotService slotService,
      TokenGenerator tokenGenerator,
      ResultCache resultCache,
      AppProperties appProperties) {
    this.eventQueryService = eventQueryService;
    this.participantRepository = participantRepository;
    this.availabilityRepository = availabilityRepository;
    this.finalSelectionRepository = finalSelectionRepository;
    this.eventStatsRepository = eventStatsRepository;
    this.slotService = slotService;
    this.tokenGenerator = tokenGenerator;
    this.resultCache = resultCache;
    this.appProperties = appProperties;
  }

  public JoinParticipantResponse joinParticipant(String publicId, JoinParticipantRequest request) {
    Event event = eventQueryService.requireEvent(publicId);
    ensureVotingOpen(event.id());

    String displayName = TextSanitizer.sanitize(request.displayName());
    String email = normalizeEmail(request.email());
    if (email != null) {
      Participant existing =
          participantRepository.findByEmailAndEventId(email, event.id()).orElse(null);
      if (existing != null) {
        if (!existing.displayName().equals(displayName) || !email.equals(existing.email())) {
          participantRepository.updateIdentity(existing.id(), displayName, email);
        }
        return new JoinParticipantResponse(
            existing.token(), participantLink(event.publicId(), existing.token()), true);
      }
    }

    Participant participant =
        participantRepository.save(
            new Participant(
                0L,
                event.id(),
                tokenGenerator.randomUrlToken(),
                displayName,
                email,
                Instant.now()));
    return new JoinParticipantResponse(
        participant.token(), participantLink(event.publicId(), participant.token()), false);
  }

  public ParticipantAvailabilityResponse getParticipantAvailability(String publicId, String token) {
    Event event = eventQueryService.requireEvent(publicId);
    Participant participant =
        participantRepository
            .findByTokenAndEventId(token, event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
    List<AvailabilityItem> items =
        availabilityRepository.findByParticipantAndEventId(participant.id(), event.id()).stream()
            .map(record -> new AvailabilityItem(record.slotStartUtc(), record.weight()))
            .toList();
    return new ParticipantAvailabilityResponse(
        participant.displayName(), participant.email(), items);
  }

  public void updateAvailability(String publicId, String token, UpdateAvailabilityRequest request) {
    Event event = eventQueryService.requireEvent(publicId);
    ensureVotingOpen(event.id());
    Participant participant =
        participantRepository
            .findByTokenAndEventId(token, event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
    Set<Instant> validSlots = Set.copyOf(slotService.generateCandidateSlots(event));
    List<AvailabilityRecord> records =
        request.items().stream()
            .filter(item -> validSlots.contains(item.slotStartUtc()))
            .map(
                item ->
                    new AvailabilityRecord(
                        participant.id(), event.id(), item.slotStartUtc(), item.weight()))
            .toList();
    availabilityRepository.replaceForParticipant(event.id(), participant.id(), records);
    eventStatsRepository.setRespondentCount(
        event.id(), availabilityRepository.countParticipantsWithAvailability(event.id()));
    resultCache.evict(event.id());
  }

  private void ensureVotingOpen(long eventId) {
    if (finalSelectionRepository.findByEventId(eventId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting is closed for this event");
    }
  }

  private String normalizeEmail(String requestedEmail) {
    if (requestedEmail == null || requestedEmail.isBlank()) {
      return null;
    }
    return requestedEmail.trim().toLowerCase(Locale.ROOT);
  }

  private String participantLink(String publicId, String token) {
    return appProperties.baseUrl() + "/e/" + publicId + "?token=" + token;
  }
}
