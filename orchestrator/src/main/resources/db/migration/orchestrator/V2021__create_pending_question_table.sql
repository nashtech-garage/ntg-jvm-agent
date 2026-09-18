CREATE TABLE pending_question (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL,
    conversation_id UUID REFERENCES conversation(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    agent_id        UUID NOT NULL REFERENCES agent(id),
    correlation_id  VARCHAR(255) NOT NULL,
    questions_json  TEXT NOT NULL,
    answers_json    TEXT,
    status          VARCHAR(20) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    answered_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT chk_pending_question_status CHECK (
        status IN ('PENDING', 'ANSWERING', 'RESOLVED', 'EXPIRED', 'CANCELLED')
    )
);

CREATE UNIQUE INDEX uk_pending_question_session_active
    ON pending_question (session_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_pending_question_conversation_status
    ON pending_question (conversation_id, status);

CREATE INDEX idx_pending_question_expiry
    ON pending_question (expires_at)
    WHERE status IN ('PENDING', 'ANSWERING');
