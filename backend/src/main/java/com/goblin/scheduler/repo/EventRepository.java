package com.goblin.scheduler.repo;

import com.goblin.scheduler.model.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class EventRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Event> mapper = (rs, rowNum) -> new Event(
        rs.getLong("id"),
        rs.getString("public_id"),
        rs.getString("host_token"),
        rs.getString("title"),
        rs.getString("description"),
        rs.getString("timezone"),
        rs.getInt("slot_minutes"),
        rs.getInt("duration_minutes"),
        rs.getDate("start_date").toLocalDate(),
        rs.getDate("end_date").toLocalDate(),
        rs.getTime("daily_start_time").toLocalTime(),
        rs.getTime("daily_end_time").toLocalTime(),
        rs.getTimestamp("created_at").toInstant()
    );

    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Event save(Event event) {
        Long id = jdbcTemplate.queryForObject("""
            insert into events (
                public_id, host_token, title, description, timezone, slot_minutes, duration_minutes,
                start_date, end_date, daily_start_time, daily_end_time, created_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            returning id
            """, Long.class,
            event.publicId(),
            event.hostToken(),
            event.title(),
            event.description(),
            event.timezone(),
            event.slotMinutes(),
            event.durationMinutes(),
            Date.valueOf(event.startDate()),
            Date.valueOf(event.endDate()),
            Time.valueOf(event.dailyStartTime()),
            Time.valueOf(event.dailyEndTime()),
            Timestamp.from(event.createdAt())
        );
        return new Event(id, event.publicId(), event.hostToken(), event.title(), event.description(), event.timezone(),
            event.slotMinutes(), event.durationMinutes(), event.startDate(), event.endDate(),
            event.dailyStartTime(), event.dailyEndTime(), event.createdAt());
    }

    public Optional<Event> findByPublicId(String publicId) {
        return jdbcTemplate.query("select * from events where public_id = ?", mapper, publicId).stream().findFirst();
    }

    public Optional<Event> findByHostToken(String hostToken) {
        return jdbcTemplate.query("select * from events where host_token = ?", mapper, hostToken).stream().findFirst();
    }
}
