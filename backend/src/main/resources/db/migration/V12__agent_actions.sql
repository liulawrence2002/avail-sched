ALTER TABLE events ADD COLUMN agent_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE agent_actions (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    payload JSONB,
    result TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    executed_at TIMESTAMP
);

CREATE INDEX idx_agent_actions_event ON agent_actions(event_id);
CREATE INDEX idx_agent_actions_created ON agent_actions(event_id, created_at DESC);
