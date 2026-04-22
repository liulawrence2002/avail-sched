package com.goblinscheduler.service;

import com.goblinscheduler.model.Event;
import com.goblinscheduler.model.Participant;
import com.goblinscheduler.repository.EventRepository;
import com.goblinscheduler.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReminderService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EmailService emailService;
    private final ResultsService resultsService;
    private final EventService eventService;

    public ReminderService(EventRepository eventRepository,
                           ParticipantRepository participantRepository,
                           EmailService emailService,
                           ResultsService resultsService,
                           EventService eventService) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.emailService = emailService;
        this.resultsService = resultsService;
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 0 */6 * * ?", zone = "UTC")
    public void remindNonRespondents() {
        try {
            List<Event> events = eventRepository.findEventsWithUpcomingDeadlines(24);
            for (Event event : events) {
                List<Participant> participants = participantRepository.findByEventId(event.getId());
                Set<Long> respondents = participantRepository.findAllAvailabilityByEventId(event.getId())
                        .stream()
                        .filter(a -> a.getSlotStart() != null)
                        .map(a -> a.getParticipantId())
                        .collect(Collectors.toSet());

                int reminded = 0;
                for (Participant p : participants) {
                    if (!respondents.contains(p.getId())) {
                        try {
                            emailService.sendReminder(event, p);
                            reminded++;
                        } catch (Exception e) {
                            logger.warn("Failed to send reminder to {} for event {}", p.getEmail(), event.getPublicId());
                        }
                    }
                }
                if (reminded > 0) {
                    eventRepository.markReminderSent(event.getId());
                    logger.info("Sent {} reminders for event {}", reminded, event.getPublicId());
                }
            }
        } catch (Exception e) {
            logger.error("Reminder job failed", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = "UTC")
    public void sendHostDigest() {
        try {
            // Find all active events created in the last 30 days that are not finalized
            // This is a simplified digest — in production you'd group by host
            logger.debug("Host digest job ran");
        } catch (Exception e) {
            logger.error("Host digest job failed", e);
        }
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "UTC")
    public void autoFinalizeEvents() {
        try {
            List<Event> events = eventRepository.findEventsPastDeadline();
            for (Event event : events) {
                try {
                    var results = resultsService.getHostResults(event.getHostToken());
                    var topSlots = results.topSlots();
                    if (topSlots != null && !topSlots.isEmpty()) {
                        String bestSlot = topSlots.get(0).slotStartUtc();
                        eventService.finalizeEvent(event.getPublicId(), event.getHostToken(),
                                new com.goblinscheduler.dto.FinalizeEventRequest(bestSlot));
                        logger.info("Auto-finalized event {} to slot {}", event.getPublicId(), bestSlot);
                    }
                } catch (Exception e) {
                    logger.error("Auto-finalize failed for event {}", event.getPublicId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Auto-finalize job failed", e);
        }
    }
}
