package com.goblin.scheduler.service;

import com.goblin.scheduler.dto.FinalSelectionResponse;
import com.goblin.scheduler.dto.FinalizeRequest;
import com.goblin.scheduler.model.Event;
import com.goblin.scheduler.model.FinalSelection;
import com.goblin.scheduler.repo.FinalSelectionRepository;
import com.goblin.scheduler.util.IcsWriter;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Finalizes an event to a single slot and renders the downloadable ICS. Pulled out of the old
 * {@code EventService} in Phase 2.2.
 */
@Service
@Transactional(readOnly = true)
public class FinalizationService {

  private static final String DEFAULT_ICS_DESCRIPTION = "Scheduled with Goblin Scheduler";

  private final EventQueryService eventQueryService;
  private final FinalSelectionRepository finalSelectionRepository;
  private final SlotService slotService;
  private final ResultCache resultCache;

  public FinalizationService(
      EventQueryService eventQueryService,
      FinalSelectionRepository finalSelectionRepository,
      SlotService slotService,
      ResultCache resultCache) {
    this.eventQueryService = eventQueryService;
    this.finalSelectionRepository = finalSelectionRepository;
    this.slotService = slotService;
    this.resultCache = resultCache;
  }

  @Transactional
  public FinalSelectionResponse finalizeEvent(
      String publicId, String hostToken, FinalizeRequest request) {
    Event event = eventQueryService.requireEvent(publicId);
    if (!event.hostToken().equals(hostToken)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid host token");
    }
    if (finalSelectionRepository.findByEventId(event.id()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This event has already been finalized");
    }
    if (!slotService.generateCandidateSlots(event).contains(request.slotStartUtc())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot is not valid for this event");
    }
    FinalSelection selection = finalSelectionRepository.save(event.id(), request.slotStartUtc());
    resultCache.evictAfterCommit(event.id());
    return new FinalSelectionResponse(publicId, selection.slotStartUtc(), selection.finalizedAt());
  }

  public FinalSelectionResponse getFinalSelection(String publicId) {
    Event event = eventQueryService.requireEvent(publicId);
    FinalSelection selection =
        finalSelectionRepository
            .findByEventId(event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));
    return new FinalSelectionResponse(publicId, selection.slotStartUtc(), selection.finalizedAt());
  }

  public String getIcs(String publicId) {
    Event event = eventQueryService.requireEvent(publicId);
    FinalSelection selection =
        finalSelectionRepository
            .findByEventId(event.id())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No finalized slot yet"));
    Instant end = selection.slotStartUtc().plusSeconds(event.durationMinutes() * 60L);
    String description =
        (event.description() == null || event.description().isBlank())
            ? DEFAULT_ICS_DESCRIPTION
            : event.description();
    return IcsWriter.writeVEvent(
        event.publicId() + "@goblin-scheduler",
        event.title(),
        description,
        selection.slotStartUtc(),
        end,
        Instant.now());
  }
}
