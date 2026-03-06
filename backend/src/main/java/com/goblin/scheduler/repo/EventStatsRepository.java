package com.goblin.scheduler.repo;

import com.goblin.scheduler.model.EventStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EventStatsRepository {
    private final JdbcTemplate jdbcTemplate;

    public EventStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void init(long eventId) {
        jdbcTemplate.update("insert into event_stats (event_id, view_count, response_count) values (?, 0, 0) on conflict (event_id) do nothing", eventId);
    }

    public void incrementView(long eventId) {
        jdbcTemplate.update("update event_stats set view_count = view_count + 1 where event_id = ?", eventId);
    }

    public void incrementResponse(long eventId) {
        jdbcTemplate.update("update event_stats set response_count = response_count + 1 where event_id = ?", eventId);
    }

    public Optional<EventStats> findByEventId(long eventId) {
        return jdbcTemplate.query("select event_id, view_count, response_count from event_stats where event_id = ?", (rs, rowNum) ->
            new EventStats(rs.getLong("event_id"), rs.getLong("view_count"), rs.getLong("response_count")), eventId).stream().findFirst();
    }
}
