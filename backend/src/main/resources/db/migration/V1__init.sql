CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(16) NOT NULL UNIQUE,
    host_token VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000),
    timezone VARCHAR(64) NOT NULL,
    slot_minutes INTEGER NOT NULL,
    duration_minutes INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    daily_start_time TIME NOT NULL,
    daily_end_time TIME NOT NULL,
    results_visibility VARCHAR(32) NOT NULL DEFAULT 'aggregate_public',
    view_count INTEGER NOT NULL DEFAULT 0,
    respondent_count INTEGER NOT NULL DEFAULT 0,
    final_slot_start TIMESTAMP WITH TIME ZONE,
    finalized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS participants (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    token VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    email VARCHAR(320),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, email)
);

CREATE TABLE IF NOT EXISTS availability_items (
    id BIGSERIAL PRIMARY KEY,
    participant_id BIGINT NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    slot_start TIMESTAMP WITH TIME ZONE NOT NULL,
    weight DECIMAL(3,2) NOT NULL,
    UNIQUE(participant_id, slot_start)
);

CREATE INDEX idx_events_public_id ON events(public_id);
CREATE INDEX idx_events_host_token ON events(host_token);
CREATE INDEX idx_participants_event_id ON participants(event_id);
CREATE INDEX idx_participants_token ON participants(token);
CREATE INDEX idx_availability_participant ON availability_items(participant_id);
