-- V3: domain CHECK constraints.
--
-- Audit summary (run against a fresh V1+V2 schema):
--   events.public_id         — UNIQUE, implicit btree index ✓
--   events.host_token        — UNIQUE, implicit btree index ✓
--   participants.token       — UNIQUE, implicit btree index ✓
--   participants(event_id)   — idx_participants_event_id ✓
--   participants(event_id, lower(email)) — idx_participants_event_email_unique (V2) ✓
--   availability(participant_id, slot_start_utc) — UNIQUE, implicit index ✓
--   availability(event_id, slot_start_utc) — idx_availability_event_slot ✓
--   final_selection.event_id — UNIQUE, implicit btree index ✓ (Phase 1.1 flagged this as
--                              potentially missing; the UNIQUE constraint already provides it)
--   event_stats.event_id     — PRIMARY KEY, implicit index ✓
--
-- No missing indexes were found. V3 is therefore purely a CHECK-constraint hardening that
-- mirrors the in-app validation in EventService.validateEventRequest so an invalid row is
-- rejected at the database layer as well as at the service layer.

alter table events
    add constraint events_results_visibility_chk
        check (results_visibility in ('aggregate_public', 'host_only'));

alter table events
    add constraint events_duration_minutes_chk
        check (duration_minutes in (30, 60, 90));
