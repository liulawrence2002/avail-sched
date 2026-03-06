package com.goblin.scheduler.repo;

import com.goblin.scheduler.model.FinalSelection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class FinalSelectionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<FinalSelection> mapper = (rs, rowNum) -> new FinalSelection(
        rs.getLong("event_id"),
        rs.getTimestamp("slot_start_utc").toInstant(),
        rs.getTimestamp("finalized_at").toInstant()
    );

    public FinalSelectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FinalSelection upsert(long eventId, Instant slotStartUtc) {
        jdbcTemplate.update("""
            insert into final_selection (event_id, slot_start_utc, finalized_at)
            values (?, ?, now())
            on conflict (event_id) do update
            set slot_start_utc = excluded.slot_start_utc, finalized_at = now()
            """, eventId, Timestamp.from(slotStartUtc));
        return findByEventId(eventId).orElseThrow();
    }

    public Optional<FinalSelection> findByEventId(long eventId) {
        return jdbcTemplate.query("select * from final_selection where event_id = ?", mapper, eventId).stream().findFirst();
    }
}

