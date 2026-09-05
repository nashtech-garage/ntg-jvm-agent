ALTER TABLE conversation
    ADD COLUMN session_id UUID;

CREATE UNIQUE INDEX idx_conversation_session_id
    ON conversation (session_id)
    WHERE session_id IS NOT NULL;
