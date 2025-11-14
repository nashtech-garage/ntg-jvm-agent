-- ====================================================================
-- Purpose: Create the conversation, chat_message  tables
-- Database: PostgreSQL
-- ====================================================================

-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- conversation table
CREATE TABLE IF NOT EXISTS conversation (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username            VARCHAR(255) NOT NULL,
    title               TEXT,
    is_active           BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- === Trigger to auto-update updated_at column ===
CREATE OR REPLACE FUNCTION update_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_conversation_updated_at
BEFORE UPDATE ON conversation
FOR EACH ROW
EXECUTE FUNCTION update_conversation_timestamp();

-- === Optional indexes for query performance ===
CREATE INDEX IF NOT EXISTS idx_conversation_username ON conversation(username);

-- chat_message table
CREATE TABLE IF NOT EXISTS chat_message (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type                INT4 NOT NULL,
    content             TEXT,
    conversation_id     UUID NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id)

);

-- === Trigger to auto-update updated_at column ===
CREATE OR REPLACE FUNCTION update_chat_message_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_chat_message_updated_at
BEFORE UPDATE ON chat_message
FOR EACH ROW
EXECUTE FUNCTION update_chat_message_timestamp();


CREATE INDEX IF NOT EXISTS idx_chat_message_conversation_id ON chat_message(conversation_id);


