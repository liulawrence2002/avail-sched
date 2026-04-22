ALTER TABLE events ADD COLUMN IF NOT EXISTS deadline TIMESTAMP WITH TIME ZONE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS auto_finalize BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS reminder_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_events_deadline ON events(deadline) WHERE deadline IS NOT NULL AND finalized_at IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_events_reminder ON events(reminder_sent_at) WHERE reminder_sent_at IS NOT NULL;
