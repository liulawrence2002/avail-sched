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
        e.setLocation(rs.getString("location"));
        e.setMeetingUrl(rs.getString("meeting_url"));
        e.setResultsVisibility(rs.getString("results_visibility"));
        e.setViewCount(rs.getInt("view_count"));
        e.setRespondentCount(rs.getInt("respondent_count"));
        java.sql.Timestamp finalSlot = rs.getTimestamp("final_slot_start");
        e.setFinalSlotStart(finalSlot != null ? finalSlot.toInstant() : null);
        java.sql.Timestamp finalizedAt = rs.getTimestamp("finalized_at");
        e.setFinalizedAt(finalizedAt != null ? finalizedAt.toInstant() : null);
        e.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        java.sql.Timestamp deletedAt = rs.getTimestamp("deleted_at");
        e.setDeletedAt(deletedAt != null ? deletedAt.toInstant() : null);
        java.sql.Timestamp deadline = rs.getTimestamp("deadline");
        e.setDeadline(deadline != null ? deadline.toInstant() : null);
        e.setAutoFinalize(rs.getBoolean("auto_finalize"));
        java.sql.Timestamp reminderSent = rs.getTimestamp("reminder_sent_at");
        e.setReminderSentAt(reminderSent != null ? reminderSent.toInstant() : null);
        e.setAgentEnabled(rs.getBoolean("agent_enabled"));
        e.setSeriesId(rs.getString("series_id"));
        int seriesIdx = rs.getInt("series_index");
        e.setSeriesIndex(rs.wasNull() ? null : seriesIdx);
        return e;
    };

    public Event save(Event event) {
        String sql = """
            INSERT INTO events (public_id, host_token, title, description, timezone, slot_minutes,
                duration_minutes, start_date, end_date, daily_start_time, daily_end_time,
                location, meeting_url, results_visibility, view_count, respondent_count, final_slot_start, finalized_at, created_at, deadline, auto_finalize, reminder_sent_at,
                agent_enabled, series_id, series_index)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setString(12, event.getLocation());
            ps.setString(13, event.getMeetingUrl());
            ps.setString(14, event.getResultsVisibility());
            ps.setInt(15, event.getViewCount());
            ps.setInt(16, event.getRespondentCount());
            ps.setTimestamp(17, event.getFinalSlotStart() != null ? Timestamp.from(event.getFinalSlotStart()) : null);
            ps.setTimestamp(18, event.getFinalizedAt() != null ? Timestamp.from(event.getFinalizedAt()) : null);
            ps.setTimestamp(19, event.getCreatedAt() != null ? Timestamp.from(event.getCreatedAt()) : Timestamp.from(java.time.Instant.now()));
            ps.setTimestamp(20, event.getDeadline() != null ? Timestamp.from(event.getDeadline()) : null);
            ps.setBoolean(21, event.isAutoFinalize());
            ps.setTimestamp(22, event.getReminderSentAt() != null ? Timestamp.from(event.getReminderSentAt()) : null);
            ps.setBoolean(23, event.isAgentEnabled());
            ps.setString(24, event.getSeriesId());
            if (event.getSeriesIndex() != null) {
                ps.setInt(25, event.getSeriesIndex());
            } else {
                ps.setNull(25, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);

        event.setId(keyHolder.getKey().longValue());
        return event;
    }

    public Optional<Event> findByPublicId(String publicId) {
        String sql = "SELECT * FROM events WHERE public_id = ? AND deleted_at IS NULL";
        try {
            Event event = jdbcTemplate.queryForObject(sql, eventRowMapper, publicId);
            return Optional.of(event);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Event> findByHostToken(String hostToken) {
        String sql = "SELECT * FROM events WHERE host_token = ? AND deleted_at IS NULL";
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

    public void updateEvent(Event event) {
        String sql = """
            UPDATE events SET title = ?, description = ?, timezone = ?, slot_minutes = ?,
                duration_minutes = ?, start_date = ?, end_date = ?, daily_start_time = ?, daily_end_time = ?,
                location = ?, meeting_url = ?, results_visibility = ?,
                deadline = ?, auto_finalize = ?,
                agent_enabled = ?, series_id = ?, series_index = ?
            WHERE id = ? AND deleted_at IS NULL
            """;
        jdbcTemplate.update(sql,
                event.getTitle(),
                event.getDescription(),
                event.getTimezone(),
                event.getSlotMinutes(),
                event.getDurationMinutes(),
                Date.valueOf(event.getStartDate()),
                Date.valueOf(event.getEndDate()),
                Time.valueOf(event.getDailyStartTime()),
                Time.valueOf(event.getDailyEndTime()),
                event.getLocation(),
                event.getMeetingUrl(),
                event.getResultsVisibility(),
                event.getDeadline() != null ? Timestamp.from(event.getDeadline()) : null,
                event.isAutoFinalize(),
                event.isAgentEnabled(),
                event.getSeriesId(),
                event.getSeriesIndex(),
                event.getId()
        );
    }

    public void softDelete(Long eventId) {
        String sql = "UPDATE events SET deleted_at = ? WHERE id = ? AND deleted_at IS NULL";
        jdbcTemplate.update(sql, Timestamp.from(java.time.Instant.now()), eventId);
    }

    public List<Event> findByHostTokens(List<String> hostTokens) {
        if (hostTokens == null || hostTokens.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(hostTokens.size(), "?"));
        String sql = "SELECT * FROM events WHERE host_token IN (" + placeholders + ") AND deleted_at IS NULL ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, eventRowMapper, hostTokens.toArray());
    }

    public int hardDeleteExpired(int retentionDays) {
        String sql = "DELETE FROM events WHERE deleted_at < NOW() - INTERVAL '? days'";
        return jdbcTemplate.update(sql, retentionDays);
    }

    public int archiveStale(int staleDays) {
        String sql = """
            UPDATE events SET deleted_at = NOW()
            WHERE created_at < NOW() - INTERVAL '? days'
              AND respondent_count = 0
              AND deleted_at IS NULL
              AND finalized_at IS NULL
            """;
        return jdbcTemplate.update(sql, staleDays);
    }

    public List<Event> findEventsWithUpcomingDeadlines(int hoursAhead) {
        String sql = """
            SELECT * FROM events
            WHERE deadline IS NOT NULL
              AND deadline <= NOW() + INTERVAL '? hours'
              AND deadline > NOW()
              AND finalized_at IS NULL
              AND deleted_at IS NULL
              AND (reminder_sent_at IS NULL OR reminder_sent_at < deadline - INTERVAL '12 hours')
            """;
        return jdbcTemplate.query(sql, eventRowMapper, hoursAhead);
    }

    public List<Event> findEventsPastDeadline() {
        String sql = """
            SELECT * FROM events
            WHERE deadline IS NOT NULL
              AND deadline <= NOW()
              AND finalized_at IS NULL
              AND deleted_at IS NULL
              AND auto_finalize = TRUE
              AND respondent_count > 0
            """;
        return jdbcTemplate.query(sql, eventRowMapper);
    }

    public void markReminderSent(Long eventId) {
        String sql = "UPDATE events SET reminder_sent_at = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, eventId);
    }

    public List<Event> findAgentEnabledEvents() {
        String sql = """
            SELECT * FROM events
            WHERE agent_enabled = TRUE
              AND finalized_at IS NULL
              AND deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, eventRowMapper);
    }

    public void updateAgentEnabled(Long eventId, boolean enabled) {
        String sql = "UPDATE events SET agent_enabled = ? WHERE id = ?";
        jdbcTemplate.update(sql, enabled, eventId);
    }
}
