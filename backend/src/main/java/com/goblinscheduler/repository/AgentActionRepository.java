package com.goblinscheduler.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class AgentActionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgentActionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long eventId, String actionType, String status, String payload, String result) {
        String sql = """
            INSERT INTO agent_actions (event_id, action_type, status, payload, result, created_at, executed_at)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
            """;
        Instant now = Instant.now();
        jdbcTemplate.update(sql, eventId, actionType, status,
                payload, result, Timestamp.from(now),
                "completed".equals(status) ? Timestamp.from(now) : null);
    }

    public List<Map<String, Object>> findByEventId(Long eventId, int limit) {
        String sql = """
            SELECT id, action_type, status, payload::text as payload, result, created_at, executed_at
            FROM agent_actions
            WHERE event_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", rs.getLong("id"));
            map.put("actionType", rs.getString("action_type"));
            map.put("status", rs.getString("status"));
            map.put("payload", rs.getString("payload"));
            map.put("result", rs.getString("result"));
            map.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
            Timestamp executed = rs.getTimestamp("executed_at");
            map.put("executedAt", executed != null ? executed.toInstant().toString() : null);
            return map;
        }, eventId, limit);
    }

    public Instant getLastActionTime(Long eventId) {
        String sql = "SELECT MAX(created_at) FROM agent_actions WHERE event_id = ?";
        Timestamp ts = jdbcTemplate.queryForObject(sql, Timestamp.class, eventId);
        return ts != null ? ts.toInstant() : null;
    }
}
