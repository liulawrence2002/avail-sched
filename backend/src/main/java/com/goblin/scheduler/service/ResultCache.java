package com.goblin.scheduler.service;

import com.goblin.scheduler.dto.ResultsResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ResultCache {
  private static final Duration TTL = Duration.ofSeconds(30);
  private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

  public ResultsResponse get(long eventId, boolean includeParticipantDetails) {
    CacheKey key = new CacheKey(eventId, includeParticipantDetails);
    CacheEntry entry = cache.get(key);
    if (entry == null || Instant.now().isAfter(entry.createdAt.plus(TTL))) {
      cache.remove(key);
      return null;
    }
    return entry.value;
  }

  public void put(long eventId, boolean includeParticipantDetails, ResultsResponse response) {
    cache.put(
        new CacheKey(eventId, includeParticipantDetails), new CacheEntry(response, Instant.now()));
  }

  public void evict(long eventId) {
    cache.keySet().removeIf(key -> key.eventId == eventId);
  }

  private record CacheKey(long eventId, boolean includeParticipantDetails) {}

  private record CacheEntry(ResultsResponse value, Instant createdAt) {}
}
