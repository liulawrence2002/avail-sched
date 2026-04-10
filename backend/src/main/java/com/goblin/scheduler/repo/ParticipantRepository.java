package com.goblin.scheduler.repo;

import com.goblin.scheduler.model.Participant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ParticipantRepository {
  private final JdbcTemplate jdbcTemplate;
  private final RowMapper<Participant> mapper =
      (rs, rowNum) ->
          new Participant(
              rs.getLong("id"),
              rs.getLong("event_id"),
              rs.getString("token"),
              rs.getString("display_name"),
              rs.getString("email"),
              rs.getTimestamp("created_at").toInstant());

  public ParticipantRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Participant save(Participant participant) {
    Long id =
        jdbcTemplate.queryForObject(
            """
            insert into participants (event_id, token, display_name, email, created_at)
            values (?, ?, ?, ?, ?)
            returning id
            """,
            Long.class,
            participant.eventId(),
            participant.token(),
            participant.displayName(),
            participant.email(),
            Timestamp.from(participant.createdAt()));
    return new Participant(
        id,
        participant.eventId(),
        participant.token(),
        participant.displayName(),
        participant.email(),
        participant.createdAt());
  }

  public Optional<Participant> findByTokenAndEventId(String token, long eventId) {
    return jdbcTemplate
        .query(
            "select * from participants where token = ? and event_id = ?", mapper, token, eventId)
        .stream()
        .findFirst();
  }

  public List<Participant> findByEventId(long eventId) {
    return jdbcTemplate.query(
        "select * from participants where event_id = ? order by created_at asc", mapper, eventId);
  }

  public Optional<Participant> findByEmailAndEventId(String email, long eventId) {
    return jdbcTemplate
        .query(
            "select * from participants where event_id = ? and lower(email) = lower(?)",
            mapper,
            eventId,
            email)
        .stream()
        .findFirst();
  }

  public void updateIdentity(long participantId, String displayName, String email) {
    jdbcTemplate.update(
        "update participants set display_name = ?, email = ? where id = ?",
        displayName,
        email,
        participantId);
  }
}
