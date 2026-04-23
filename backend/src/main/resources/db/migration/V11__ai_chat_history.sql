CREATE TABLE ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_chat_event_id ON ai_chat_messages(event_id);
CREATE INDEX idx_ai_chat_created ON ai_chat_messages(event_id, created_at);
