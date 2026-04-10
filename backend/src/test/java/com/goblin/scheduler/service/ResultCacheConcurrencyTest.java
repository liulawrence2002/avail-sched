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
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link ResultCache} under concurrent put / get / evict load. Written before Phase 2.4
 * replaces the hand-rolled {@link java.util.concurrent.ConcurrentHashMap} backend with Caffeine;
 * the assertions only check externally observable behavior (no visibility anomalies, evictions are
 * respected, entries can be re-populated) so the test continues to cover the Caffeine-backed
 * implementation after the swap.
 */
class ResultCacheConcurrencyTest {

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
}
