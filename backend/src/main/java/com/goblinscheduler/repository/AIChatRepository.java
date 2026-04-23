package com.goblinscheduler.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class AIChatRepository {

    private final JdbcTemplate jdbcTemplate;

    public AIChatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveMessage(Long eventId, String role, String content) {
        String sql = "INSERT INTO ai_chat_messages (event_id, role, content, created_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, eventId, role, content, Timestamp.from(Instant.now()));
    }

    public List<Map<String, String>> getHistory(Long eventId, int limit) {
        String sql = """
            SELECT role, content FROM ai_chat_messages
            WHERE event_id = ?
            ORDER BY created_at ASC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> Map.of(
                "role", rs.getString("role"),
                "content", rs.getString("content")
        ), eventId, limit);
    }

    public void deleteByEventId(Long eventId) {
        jdbcTemplate.update("DELETE FROM ai_chat_messages WHERE event_id = ?", eventId);
    }
}
