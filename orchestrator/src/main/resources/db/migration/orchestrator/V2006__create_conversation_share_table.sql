-- ====================================================================
-- Purpose: Create the conversation_share table for sharing conversations
-- Database: PostgreSQL
-- ====================================================================

-- conversation_share table
CREATE TABLE IF NOT EXISTS conversation_share (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id         UUID NOT NULL,
    shared_by_username      VARCHAR(255) NOT NULL,
    share_token             VARCHAR(255) UNIQUE NOT NULL,
    is_expired              BOOLEAN DEFAULT FALSE,
    expires_at              TIMESTAMPTZ,
    shared_message_ids      JSONB DEFAULT '[]'::JSONB,
    created_at              TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_share_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE
);

-- === Indexes for query performance ===
CREATE INDEX IF NOT EXISTS idx_conversation_share_conversation_id ON conversation_share(conversation_id);

