package com.goblinscheduler.service;

import com.goblinscheduler.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class HousekeepingService {
    private static final Logger logger = LoggerFactory.getLogger(HousekeepingService.class);

    private final EventRepository eventRepository;
    private final boolean enabled;
    private final int retentionDays;
    private final int staleDays;

    public HousekeepingService(EventRepository eventRepository,
                               @Value("${goblin.housekeeping.enabled:true}") boolean enabled,
                               @Value("${goblin.housekeeping.retention-days:30}") int retentionDays,
                               @Value("${goblin.housekeeping.stale-days:90}") int staleDays) {
        this.eventRepository = eventRepository;
        this.enabled = enabled;
        this.retentionDays = retentionDays;
        this.staleDays = staleDays;
    }

    @Scheduled(cron = "0 0 3 * * ?", zone = "UTC")
    public void hardDeleteExpiredEvents() {
        if (!enabled) return;
        try {
            int deleted = eventRepository.hardDeleteExpired(retentionDays);
            if (deleted > 0) {
                logger.info("Housekeeping: hard-deleted {} events older than {} days", deleted, retentionDays);
            }
        } catch (Exception e) {
            logger.error("Housekeeping: hard delete failed", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?", zone = "UTC")
    public void archiveStaleEvents() {
        if (!enabled) return;
        try {
            int archived = eventRepository.archiveStale(staleDays);
            if (archived > 0) {
                logger.info("Housekeeping: auto-archived {} stale events older than {} days", archived, staleDays);
            }
        } catch (Exception e) {
            logger.error("Housekeeping: stale archive failed", e);
        }
    }
}
