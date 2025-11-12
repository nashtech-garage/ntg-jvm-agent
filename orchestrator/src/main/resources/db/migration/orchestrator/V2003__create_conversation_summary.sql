CREATE TABLE conversation_summary (
    conversation_id UUID PRIMARY KEY,
    summary_text TEXT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_conversation
      FOREIGN KEY(conversation_id) REFERENCES conversation(id)
);

