CREATE TABLE IF NOT EXISTS event_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    timezone VARCHAR(100),
    slot_minutes INTEGER,
    duration_minutes INTEGER,
    daily_start_time VARCHAR(10),
    daily_end_time VARCHAR(10),
    results_visibility VARCHAR(50),
    location VARCHAR(500),
    meeting_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_templates_name ON event_templates(name);
