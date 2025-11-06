-- ====================================================================
-- Purpose: Create the agent table to support AI Agent Management
-- Database: PostgreSQL
-- ====================================================================

-- === UP Migration ===

-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS agent (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    model               VARCHAR(100) NOT NULL,
    description         TEXT,
    temperature         NUMERIC(3,2) DEFAULT 0.7 CHECK (temperature >= 0 AND temperature <= 2),
    max_tokens          INTEGER DEFAULT 2048 CHECK (max_tokens > 0),
    top_p               NUMERIC(3,2) DEFAULT 1.0 CHECK (top_p >= 0 AND top_p <= 1),
    frequency_penalty   NUMERIC(3,2) DEFAULT 0.0 CHECK (frequency_penalty >= -2 AND frequency_penalty <= 2),
    presence_penalty    NUMERIC(3,2) DEFAULT 0.0 CHECK (presence_penalty >= -2 AND presence_penalty <= 2),
    active              BOOLEAN DEFAULT TRUE,
    provider            VARCHAR(50),
    settings            JSONB,                 -- for flexible configuration or fine-tuning params
    version             INTEGER DEFAULT 0,     -- optimistic locking / version tracking
    deleted_at          TIMESTAMPTZ,           -- soft deletion timestamp
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- === Trigger to auto-update updated_at column ===
CREATE OR REPLACE FUNCTION update_agent_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_agent_updated_at
BEFORE UPDATE ON agent
FOR EACH ROW
EXECUTE FUNCTION update_agent_timestamp();

-- === Optional indexes for query performance ===
CREATE INDEX IF NOT EXISTS idx_agent_name ON agent(name);
CREATE INDEX IF NOT EXISTS idx_agent_model ON agent(model);
CREATE INDEX IF NOT EXISTS idx_agent_provider ON agent(provider);
CREATE INDEX IF NOT EXISTS idx_agent_active ON agent(active);

-- === Optional index for soft deletion cleanup or filtering ===
CREATE INDEX IF NOT EXISTS idx_agent_deleted_at ON agent(deleted_at);

-- ====================================================================
-- === DOWN Migration (Manual Rollback) ===
-- DROP TRIGGER IF EXISTS trg_agent_updated_at ON agent;
-- DROP FUNCTION IF EXISTS update_agent_timestamp();
-- DROP INDEX IF EXISTS idx_agent_deleted_at;
-- DROP INDEX IF EXISTS idx_agent_active;
-- DROP INDEX IF EXISTS idx_agent_provider;
-- DROP INDEX IF EXISTS idx_agent_model;
-- DROP INDEX IF EXISTS idx_agent_name;
-- DROP TABLE IF EXISTS agent;
-- ====================================================================
