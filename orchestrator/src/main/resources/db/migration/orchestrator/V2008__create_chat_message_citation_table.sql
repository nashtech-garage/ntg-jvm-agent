CREATE TABLE IF NOT EXISTS chat_message_citation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    char_start INT NOT NULL,
    char_end INT NOT NULL,
    chat_message_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id),

    CONSTRAINT fk_chat_message
        FOREIGN KEY(chat_message_id)
        REFERENCES chat_message(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_message_citation_message_id
    ON chat_message_citation(chat_message_id);
