ALTER TABLE events ADD COLUMN series_id VARCHAR(16);
ALTER TABLE events ADD COLUMN series_index INTEGER;

CREATE INDEX idx_events_series ON events(series_id) WHERE series_id IS NOT NULL;
