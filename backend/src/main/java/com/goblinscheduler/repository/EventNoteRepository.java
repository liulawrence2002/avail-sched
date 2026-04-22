package com.goblinscheduler.repository;

import com.goblinscheduler.model.EventNote;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class EventNoteRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventNoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<EventNote> rowMapper = (rs, rowNum) -> {
        EventNote n = new EventNote();
        n.setId(rs.getLong("id"));
        n.setEventId(rs.getLong("event_id"));
        n.setContent(rs.getString("content"));
        n.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        n.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return n;
    };

    public EventNote save(EventNote note) {
        String sql = "INSERT INTO event_notes (event_id, content, created_at, updated_at) VALUES (?, ?, ?, ?) RETURNING id";
        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, note.getEventId());
            ps.setString(2, note.getContent());
            ps.setTimestamp(3, Timestamp.from(note.getCreatedAt()));
            ps.setTimestamp(4, Timestamp.from(note.getUpdatedAt()));
            return ps;
        }, keyHolder);
        note.setId(keyHolder.getKey().longValue());
        return note;
    }

    public void update(EventNote note) {
        String sql = "UPDATE event_notes SET content = ?, updated_at = ? WHERE id = ? AND event_id = ?";
        jdbcTemplate.update(sql, note.getContent(), Timestamp.from(note.getUpdatedAt()), note.getId(), note.getEventId());
    }

    public Optional<EventNote> findByEventId(Long eventId) {
        String sql = "SELECT * FROM event_notes WHERE event_id = ? ORDER BY updated_at DESC LIMIT 1";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, rowMapper, eventId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
