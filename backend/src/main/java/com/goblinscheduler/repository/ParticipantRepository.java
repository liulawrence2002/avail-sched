package com.goblinscheduler.repository;

import com.goblinscheduler.model.AvailabilityItem;
import com.goblinscheduler.model.Participant;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class ParticipantRepository {

    private final JdbcTemplate jdbcTemplate;

    public ParticipantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Participant> participantRowMapper = (rs, rowNum) -> {
        Participant p = new Participant();
        p.setId(rs.getLong("id"));
        p.setEventId(rs.getLong("event_id"));
        p.setToken(rs.getString("token"));
        p.setDisplayName(rs.getString("display_name"));
        p.setEmail(rs.getString("email"));
        p.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return p;
    };

    private final RowMapper<AvailabilityItem> itemRowMapper = (rs, rowNum) -> {
        AvailabilityItem i = new AvailabilityItem();
        i.setId(rs.getLong("id"));
        i.setParticipantId(rs.getLong("participant_id"));
        i.setSlotStart(rs.getTimestamp("slot_start").toInstant());
        i.setWeight(rs.getBigDecimal("weight"));
        return i;
    };

    public Participant save(Participant participant) {
        String sql = """
            INSERT INTO participants (event_id, token, display_name, email, created_at)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

        var keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, participant.getEventId());
            ps.setString(2, participant.getToken());
            ps.setString(3, participant.getDisplayName());
            ps.setString(4, participant.getEmail());
            ps.setTimestamp(5, Timestamp.from(java.time.Instant.now()));
            return ps;
        }, keyHolder);

        participant.setId(keyHolder.getKey().longValue());
        return participant;
    }

    public Optional<Participant> findByToken(String token) {
        String sql = "SELECT * FROM participants WHERE token = ?";
        try {
            Participant p = jdbcTemplate.queryForObject(sql, participantRowMapper, token);
            return Optional.of(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Participant> findByEventIdAndEmail(Long eventId, String email) {
        String sql = "SELECT * FROM participants WHERE event_id = ? AND email = ?";
        try {
            Participant p = jdbcTemplate.queryForObject(sql, participantRowMapper, eventId, email);
            return Optional.of(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Participant> findByEventId(Long eventId) {
        String sql = "SELECT * FROM participants WHERE event_id = ?";
        return jdbcTemplate.query(sql, participantRowMapper, eventId);
    }

    public List<AvailabilityItem> findItemsByParticipantId(Long participantId) {
        String sql = "SELECT * FROM availability_items WHERE participant_id = ?";
        return jdbcTemplate.query(sql, itemRowMapper, participantId);
    }

    public void deleteItemsByParticipantId(Long participantId) {
        String sql = "DELETE FROM availability_items WHERE participant_id = ?";
        jdbcTemplate.update(sql, participantId);
    }

    public void saveItems(Long participantId, List<AvailabilityItem> items) {
        if (items.isEmpty()) return;
        String sql = "INSERT INTO availability_items (participant_id, slot_start, weight) VALUES (?, ?, ?) ON CONFLICT (participant_id, slot_start) DO UPDATE SET weight = EXCLUDED.weight";
        for (AvailabilityItem item : items) {
            jdbcTemplate.update(sql, participantId, Timestamp.from(item.getSlotStart()), item.getWeight());
        }
    }

    public List<ParticipantAvailabilityView> findAllAvailabilityByEventId(Long eventId) {
        String sql = """
            SELECT p.id as participant_id, p.display_name, p.email, a.slot_start, a.weight
            FROM participants p
            LEFT JOIN availability_items a ON p.id = a.participant_id
            WHERE p.event_id = ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ParticipantAvailabilityView v = new ParticipantAvailabilityView();
            v.setParticipantId(rs.getLong("participant_id"));
            v.setDisplayName(rs.getString("display_name"));
            v.setEmail(rs.getString("email"));
            v.setSlotStart(rs.getTimestamp("slot_start") != null ? rs.getTimestamp("slot_start").toInstant() : null);
            v.setWeight(rs.getBigDecimal("weight"));
            return v;
        }, eventId);
    }

    public static class ParticipantAvailabilityView {
        private Long participantId;
        private String displayName;
        private String email;
        private java.time.Instant slotStart;
        private java.math.BigDecimal weight;

        public Long getParticipantId() { return participantId; }
        public void setParticipantId(Long participantId) { this.participantId = participantId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public java.time.Instant getSlotStart() { return slotStart; }
        public void setSlotStart(java.time.Instant slotStart) { this.slotStart = slotStart; }
        public java.math.BigDecimal getWeight() { return weight; }
        public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }
    }
}
