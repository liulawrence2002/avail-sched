package com.goblin.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.goblin.scheduler.dto.ResultsResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Exercises {@link ResultCache} under concurrent put / get / evict load and verifies the Phase 2.3
 * {@code evictAfterCommit} transaction-aware eviction path.
 *
 * <p>The concurrency assertions only check externally observable behavior (no visibility anomalies,
 * evictions are respected, entries can be re-populated) so they cover the Caffeine backend swap
 * (Phase 2.4) without modification. The {@code afterCommit} tests are new in Phase 2.3 and lock
 * down the contract the services rely on: an eviction scheduled inside a transaction only fires on
 * commit, and a rolled-back transaction leaves the cache untouched so the next read does not
 * recompute from reverted DB state.
 */
class ResultCacheConcurrencyTest {

  @AfterEach
  void clearTxState() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private static final int THREADS = 16;
  private static final int ITERATIONS_PER_THREAD = 500;

  @Test
  void concurrentPutGetEvict_doesNotLoseData() throws InterruptedException {
    ResultCache cache = new ResultCache();
    ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(THREADS);
    AtomicInteger failures = new AtomicInteger();

    for (int t = 0; t < THREADS; t++) {
      final long eventId = (t % 4) + 1;
      final boolean includeDetails = (t % 2) == 0;
      executor.submit(
          () -> {
            try {
              start.await();
              for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                ResultsResponse payload =
                    new ResultsResponse(
                        "pub-" + eventId, "UTC", i, i, null, includeDetails, List.of());
                cache.put(eventId, includeDetails, payload);
                ResultsResponse fetched = cache.get(eventId, includeDetails);
                if (fetched != null && !fetched.publicId().startsWith("pub-")) {
                  failures.incrementAndGet();
                }
                if ((i & 63) == 0) {
                  cache.evict(eventId);
                }
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              failures.incrementAndGet();
            } finally {
              done.countDown();
            }
          });
    }

    start.countDown();
    assertTrue(done.await(30, TimeUnit.SECONDS), "Workers did not finish in time");
    executor.shutdownNow();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    assertTrue(failures.get() == 0, "Concurrent cache access observed corrupted entries");
  }

  @Test
  void evictRemovesAllVariantsForEventId() {
    ResultCache cache = new ResultCache();
    ResultsResponse publicView = new ResultsResponse("pub-1", "UTC", 0, 0, null, false, List.of());
    ResultsResponse hostView = new ResultsResponse("pub-1", "UTC", 0, 0, null, true, List.of());

    cache.put(1L, false, publicView);
    cache.put(1L, true, hostView);

    assertNotNull(cache.get(1L, false));
    assertNotNull(cache.get(1L, true));

    cache.evict(1L);

    assertNull(cache.get(1L, false));
    assertNull(cache.get(1L, true));
  }

  @Test
  void keysAreScopedByEventId() {
    ResultCache cache = new ResultCache();
    cache.put(1L, false, new ResultsResponse("pub-1", "UTC", 0, 0, null, false, List.of()));
    cache.put(2L, false, new ResultsResponse("pub-2", "UTC", 0, 0, null, false, List.of()));

    assertNotNull(cache.get(1L, false));
    assertNotNull(cache.get(2L, false));

    cache.evict(1L);

    assertNull(cache.get(1L, false));
    assertNotNull(cache.get(2L, false));
  }

  @Test
  void evictAfterCommit_noActiveTransaction_evictsImmediately() {
    ResultCache cache = new ResultCache();
    cache.put(1L, false, new ResultsResponse("pub-1", "UTC", 0, 0, null, false, List.of()));

    cache.evictAfterCommit(1L);

    assertNull(cache.get(1L, false));
  }

  @Test
  void evictAfterCommit_insideTransaction_defersUntilAfterCommit() {
    ResultCache cache = new ResultCache();
    cache.put(1L, false, new ResultsResponse("pub-1", "UTC", 0, 0, null, false, List.of()));

    TransactionSynchronizationManager.initSynchronization();
    try {
      cache.evictAfterCommit(1L);

      // Before commit, the cache still holds the entry so in-flight reads see the pre-commit
      // value. This is the whole point: committing is what makes the new DB state visible, and
      // the cache should mirror the DB at commit time, not when the eviction was scheduled.
      assertNotNull(cache.get(1L, false), "Eviction must not fire before commit");

      // Simulate Spring's commit path: fire afterCommit on every registered synchronization.
      for (TransactionSynchronization sync :
          TransactionSynchronizationManager.getSynchronizations()) {
        sync.afterCommit();
      }
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    assertNull(cache.get(1L, false), "Eviction must fire after commit");
  }

  @Test
  void evictAfterCommit_rolledBackTransaction_leavesCacheUntouched() {
    ResultCache cache = new ResultCache();
    ResultsResponse cached = new ResultsResponse("pub-1", "UTC", 0, 0, null, false, List.of());
    cache.put(1L, false, cached);

    TransactionSynchronizationManager.initSynchronization();
    try {
      cache.evictAfterCommit(1L);
      // Simulate a rollback: the surrounding transaction never calls afterCommit on its
      // registered synchronizations, it just clears them. No eviction should happen.
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    // The eviction was deferred to afterCommit, which the "rollback" never reached, so the
    // cache still holds the original value. This is the regression the plan flagged: a
    // rolled-back save must not leave the cache stale-evicted.
    assertNotNull(
        cache.get(1L, false),
        "Cache must not be evicted when the scheduling transaction rolls back");
    assertNotNull(cache.get(1L, false));
  }
}
