package com.goblinscheduler.repository;

import com.goblinscheduler.model.Event;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.Optional;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Event> eventRowMapper = (rs, rowNum) -> {
        Event e = new Event();
        e.setId(rs.getLong("id"));
        e.setPublicId(rs.getString("public_id"));
        e.setHostToken(rs.getString("host_token"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setTimezone(rs.getString("timezone"));
        e.setSlotMinutes(rs.getInt("slot_minutes"));
        e.setDurationMinutes(rs.getInt("duration_minutes"));
        e.setStartDate(rs.getDate("start_date").toLocalDate());
        e.setEndDate(rs.getDate("end_date").toLocalDate());
        e.setDailyStartTime(rs.getTime("daily_start_time").toLocalTime());
        e.setDailyEndTime(rs.getTime("daily_end_time").toLocalTime());
        e.setResultsVisibility(rs.getString("results_visibility"));
        e.setViewCount(rs.getInt("view_count"));
        e.setRespondentCount(rs.getInt("respondent_count"));
        java.sql.Timestamp finalSlot = rs.getTimestamp("final_slot_start");
        e.setFinalSlotStart(finalSlot != null ? finalSlot.toInstant() : null);
        java.sql.Timestamp finalizedAt = rs.getTimestamp("finalized_at");
        e.setFinalizedAt(finalizedAt != null ? finalizedAt.toInstant() : null);
        e.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return e;
    };

    public Event save(Event event) {
        String sql = """
            INSERT INTO events (public_id, host_token, title, description, timezone, slot_minutes,
                duration_minutes, start_date, end_date, daily_start_time, daily_end_time,
                results_visibility, view_count, respondent_count, final_slot_start, finalized_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, event.getPublicId());
            ps.setString(2, event.getHostToken());
            ps.setString(3, event.getTitle());
            ps.setString(4, event.getDescription());
            ps.setString(5, event.getTimezone());
            ps.setInt(6, event.getSlotMinutes());
            ps.setInt(7, event.getDurationMinutes());
            ps.setDate(8, Date.valueOf(event.getStartDate()));
            ps.setDate(9, Date.valueOf(event.getEndDate()));
            ps.setTime(10, Time.valueOf(event.getDailyStartTime()));
            ps.setTime(11, Time.valueOf(event.getDailyEndTime()));
            ps.setString(12, event.getResultsVisibility());
            ps.setInt(13, event.getViewCount());
            ps.setInt(14, event.getRespondentCount());
            ps.setTimestamp(15, event.getFinalSlotStart() != null ? Timestamp.from(event.getFinalSlotStart()) : null);
            ps.setTimestamp(16, event.getFinalizedAt() != null ? Timestamp.from(event.getFinalizedAt()) : null);
            ps.setTimestamp(17, event.getCreatedAt() != null ? Timestamp.from(event.getCreatedAt()) : Timestamp.from(java.time.Instant.now()));
            return ps;
        }, keyHolder);

        event.setId(keyHolder.getKey().longValue());
        return event;
    }

    public Optional<Event> findByPublicId(String publicId) {
        String sql = "SELECT * FROM events WHERE public_id = ?";
        try {
            Event event = jdbcTemplate.queryForObject(sql, eventRowMapper, publicId);
            return Optional.of(event);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Event> findByHostToken(String hostToken) {
        String sql = "SELECT * FROM events WHERE host_token = ?";
        try {
            Event event = jdbcTemplate.queryForObject(sql, eventRowMapper, hostToken);
            return Optional.of(event);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void incrementViewCount(String publicId) {
        String sql = "UPDATE events SET view_count = view_count + 1 WHERE public_id = ?";
        jdbcTemplate.update(sql, publicId);
    }

    public void incrementRespondentCount(Long eventId) {
        String sql = "UPDATE events SET respondent_count = respondent_count + 1 WHERE id = ?";
        jdbcTemplate.update(sql, eventId);
    }

    public void finalizeEvent(Long eventId, java.time.Instant slotStart, java.time.Instant finalizedAt) {
        String sql = "UPDATE events SET final_slot_start = ?, finalized_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, Timestamp.from(slotStart), Timestamp.from(finalizedAt), eventId);
    }
}
