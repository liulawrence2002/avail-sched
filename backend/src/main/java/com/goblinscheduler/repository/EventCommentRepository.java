package com.goblinscheduler.repository;

import com.goblinscheduler.model.EventComment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventCommentRepository {
    private final JdbcTemplate jdbcTemplate;

    public EventCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<EventComment> mapper = (rs, rowNum) -> {
        EventComment c = new EventComment();
        c.setId(rs.getLong("id"));
        c.setEventId(rs.getLong("event_id"));
        Long pid = rs.getObject("participant_id", Long.class);
        c.setParticipantId(pid);
        c.setAuthorName(rs.getString("author_name"));
        c.setContent(rs.getString("content"));
        c.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return c;
    };

    public List<EventComment> findByEventId(Long eventId) {
        String sql = "SELECT * FROM event_comments WHERE event_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, mapper, eventId);
    }

    public EventComment save(EventComment comment) {
        String sql = "INSERT INTO event_comments (event_id, participant_id, author_name, content, created_at) VALUES (?, ?, ?, ?, NOW()) RETURNING id, created_at";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            comment.setId(rs.getLong("id"));
            comment.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return comment;
        }, comment.getEventId(), comment.getParticipantId(), comment.getAuthorName(), comment.getContent());
    }
}
