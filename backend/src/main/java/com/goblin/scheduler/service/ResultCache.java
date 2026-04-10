package com.goblin.scheduler.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.goblin.scheduler.dto.ResultsResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Short-TTL cache for {@link ResultsResponse} keyed on {@code (eventId,
 * includeParticipantDetails)}.
 *
 * <p>Phase 2.4 of the launch-readiness plan replaced the hand-rolled {@link
 * java.util.concurrent.ConcurrentHashMap} backend with Caffeine so entries are bounded in both
 * count and time, and so thread-safety is delegated to a battle-tested implementation. The public
 * API is unchanged (Phase 0.4 concurrency and eviction tests still pass without modification).
 *
 * <p>Phase 2.3 added {@link #evictAfterCommit(long)}, which registers a {@link
 * TransactionSynchronization} so a cache eviction only fires after the surrounding transaction
 * commits. Callers that evict on a rolled-back transaction would otherwise leave the cache in a
 * "stale-evicted" state (the stale value would be recomputed from the rolled-back DB on the next
 * read, then re-cached). When no transaction is active, {@code evictAfterCommit} falls back to
 * immediate eviction.
 */
@Component
public class ResultCache {

  private static final Duration TTL = Duration.ofSeconds(30);
  private static final long MAX_SIZE = 10_000L;

  private final Cache<CacheKey, ResultsResponse> cache;

  public ResultCache() {
    this.cache = Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(MAX_SIZE).build();
  }

  public ResultsResponse get(long eventId, boolean includeParticipantDetails) {
    return cache.getIfPresent(new CacheKey(eventId, includeParticipantDetails));
  }

  public void put(long eventId, boolean includeParticipantDetails, ResultsResponse response) {
    cache.put(new CacheKey(eventId, includeParticipantDetails), response);
  }

  public void evict(long eventId) {
    cache.asMap().keySet().removeIf(key -> key.eventId == eventId);
  }

  /**
   * Evict all variants for {@code eventId} after the current transaction commits. If no Spring
   * transaction is active (e.g. a unit test calling directly), evict immediately — this keeps the
   * semantics predictable in test code and in any code path that does not participate in a wrapping
   * {@code @Transactional}.
   */
  public void evictAfterCommit(long eventId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      evict(eventId);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            evict(eventId);
          }
        });
  }

  private record CacheKey(long eventId, boolean includeParticipantDetails) {}
}
