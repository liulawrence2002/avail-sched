-- Performance indexes for housekeeping jobs
CREATE INDEX IF NOT EXISTS idx_events_deleted_at_cleanup ON events(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_created_at_cleanup ON events(created_at, deleted_at, respondent_count, finalized_at);
