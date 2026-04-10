package com.goblin.scheduler.repo;

import com.goblin.scheduler.model.AvailabilityRecord;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AvailabilityRepository {
  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<AvailabilityRecord> mapper =
      (rs, rowNum) ->
          new AvailabilityRecord(
              rs.getLong("participant_id"),
              rs.getLong("event_id"),
              rs.getTimestamp("slot_start_utc").toInstant(),
              rs.getDouble("weight"));

  public AvailabilityRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void replaceForParticipant(
      long eventId, long participantId, List<AvailabilityRecord> items) {
    jdbcTemplate.update(
        "delete from availability where participant_id = ? and event_id = ?",
        participantId,
        eventId);
    if (items.isEmpty()) {
      return;
    }
    jdbcTemplate.batchUpdate(
        """
            insert into availability (participant_id, event_id, slot_start_utc, weight)
            values (?, ?, ?, ?)
            """,
        items,
        items.size(),
        (ps, item) -> {
          ps.setLong(1, item.participantId());
          ps.setLong(2, item.eventId());
          ps.setTimestamp(3, Timestamp.from(item.slotStartUtc()));
          ps.setDouble(4, item.weight());
        });
  }

  public List<AvailabilityRecord> findByEventId(long eventId) {
    return jdbcTemplate.query(
        "select participant_id, event_id, slot_start_utc, weight from availability where event_id = ?",
        mapper,
        eventId);
  }

  public List<AvailabilityRecord> findByParticipantAndEventId(long participantId, long eventId) {
    return jdbcTemplate.query(
        "select participant_id, event_id, slot_start_utc, weight from availability where participant_id = ? and event_id = ? order by slot_start_utc asc",
        mapper,
        participantId,
        eventId);
  }

  public long countParticipantsWithAvailability(long eventId) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(distinct participant_id) from availability where event_id = ?",
            Long.class,
            eventId);
    return count == null ? 0L : count;
  }
}
