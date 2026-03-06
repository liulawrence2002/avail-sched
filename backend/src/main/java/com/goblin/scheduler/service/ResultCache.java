package com.goblin.scheduler.service;

import com.goblin.scheduler.dto.ResultsResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ResultCache {
    private static final Duration TTL = Duration.ofSeconds(30);
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public ResultsResponse get(long eventId) {
        CacheEntry entry = cache.get(eventId);
        if (entry == null || Instant.now().isAfter(entry.createdAt.plus(TTL))) {
            cache.remove(eventId);
            return null;
        }
        return entry.value;
    }

    public void put(long eventId, ResultsResponse response) {
        cache.put(eventId, new CacheEntry(response, Instant.now()));
    }

    public void evict(long eventId) {
        cache.remove(eventId);
    }

    private record CacheEntry(ResultsResponse value, Instant createdAt) {}
}

