-- ====================================================================
-- Purpose: Create the conversation, chat_message  tables
-- Database: PostgreSQL
-- ====================================================================

-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- conversation table
CREATE TABLE IF NOT EXISTS conversation (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               TEXT,
    is_active           BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by          UUID REFERENCES users(id)
);

-- chat_message table
CREATE TABLE IF NOT EXISTS chat_message (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type                VARCHAR(50) NOT NULL,
    content             TEXT,
    conversation_id     UUID NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by          UUID REFERENCES users(id),
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id)
);

CREATE INDEX IF NOT EXISTS idx_chat_message_conversation_id ON chat_message(conversation_id);


