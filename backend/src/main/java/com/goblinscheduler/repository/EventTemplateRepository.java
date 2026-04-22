package com.goblinscheduler.repository;

import com.goblinscheduler.model.EventTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventTemplateRepository {
    private final JdbcTemplate jdbcTemplate;

    public EventTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<EventTemplate> mapper = (rs, rowNum) -> {
        EventTemplate t = new EventTemplate();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setDescription(rs.getString("description"));
        t.setTimezone(rs.getString("timezone"));
        t.setSlotMinutes(rs.getObject("slot_minutes", Integer.class));
        t.setDurationMinutes(rs.getObject("duration_minutes", Integer.class));
        t.setDailyStartTime(rs.getString("daily_start_time"));
        t.setDailyEndTime(rs.getString("daily_end_time"));
        t.setResultsVisibility(rs.getString("results_visibility"));
        t.setLocation(rs.getString("location"));
        t.setMeetingUrl(rs.getString("meeting_url"));
        t.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return t;
    };

    public List<EventTemplate> findAll() {
        return jdbcTemplate.query("SELECT * FROM event_templates ORDER BY created_at DESC", mapper);
    }

    public EventTemplate findById(Long id) {
        List<EventTemplate> list = jdbcTemplate.query("SELECT * FROM event_templates WHERE id = ?", mapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public EventTemplate save(EventTemplate t) {
        String sql = "INSERT INTO event_templates (name, description, timezone, slot_minutes, duration_minutes, daily_start_time, daily_end_time, results_visibility, location, meeting_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) RETURNING id, created_at";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            t.setId(rs.getLong("id"));
            t.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return t;
        }, t.getName(), t.getDescription(), t.getTimezone(), t.getSlotMinutes(), t.getDurationMinutes(), t.getDailyStartTime(), t.getDailyEndTime(), t.getResultsVisibility(), t.getLocation(), t.getMeetingUrl());
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM event_templates WHERE id = ?", id);
    }
}
