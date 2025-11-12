-- ====================================================================
-- Purpose: Create schema for AI Agent Management
-- Database: PostgreSQL
-- ====================================================================

-- === UP Migration ===

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- =======================
-- 1. agent
-- =======================
CREATE TABLE IF NOT EXISTS agent (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(100) NOT NULL,
    model               VARCHAR(100) NOT NULL,
    description         TEXT,
    temperature         NUMERIC(3,2) NOT NULL DEFAULT 0.7 CHECK (temperature >= 0 AND temperature <= 2),
    max_tokens          INTEGER NOT NULL DEFAULT 2048 CHECK (max_tokens > 0),
    top_p               NUMERIC(3,2) NOT NULL DEFAULT 1.0 CHECK (top_p >= 0 AND top_p <= 1),
    frequency_penalty   NUMERIC(3,2) NOT NULL DEFAULT 0.0 CHECK (frequency_penalty BETWEEN -2 AND 2),
    presence_penalty    NUMERIC(3,2) NOT NULL DEFAULT 0.0 CHECK (presence_penalty BETWEEN -2 AND 2),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    provider            VARCHAR(50),
    settings            JSONB,
    version             INTEGER NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          TIMESTAMPTZ
);

-- Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_agent_updated_at
BEFORE UPDATE ON agent
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

-- Indexes
CREATE INDEX IF NOT EXISTS idx_agent_name ON agent(name);
CREATE INDEX IF NOT EXISTS idx_agent_model ON agent(model);
CREATE INDEX IF NOT EXISTS idx_agent_provider ON agent(provider);
CREATE INDEX IF NOT EXISTS idx_agent_active ON agent(active);
CREATE INDEX IF NOT EXISTS idx_agent_deleted_at ON agent(deleted_at);
CREATE INDEX IF NOT EXISTS idx_agent_active_not_deleted ON agent (active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_agent_settings_jsonb ON agent USING gin (settings jsonb_ops);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_name_not_deleted ON agent (name) WHERE deleted_at IS NULL;

-- =======================
-- 2. agent_tool
-- =======================
CREATE TABLE IF NOT EXISTS agent_tool (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    type         VARCHAR(50),
    description  TEXT,
    config       JSONB,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   TIMESTAMPTZ
);

CREATE TRIGGER trg_agent_tool_updated_at
BEFORE UPDATE ON agent_tool
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE INDEX IF NOT EXISTS idx_agent_tool_name ON agent_tool(name);
CREATE INDEX IF NOT EXISTS idx_agent_tool_type ON agent_tool(type);
CREATE INDEX IF NOT EXISTS idx_agent_tool_active ON agent_tool(active);
CREATE INDEX IF NOT EXISTS idx_agent_tool_deleted_at ON agent_tool(deleted_at);
CREATE INDEX IF NOT EXISTS idx_agent_tool_config_jsonb ON agent_tool USING gin (config jsonb_ops);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_tool_name_not_deleted ON agent_tool (name) WHERE deleted_at IS NULL;

-- =======================
-- 3. agent_knowledge
-- =======================
CREATE TABLE IF NOT EXISTS agent_knowledge (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    source_type     VARCHAR(50),
    source_uri      TEXT UNIQUE,
    metadata        JSONB,
    embedding_model VARCHAR(100),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMPTZ
);

CREATE TRIGGER trg_agent_knowledge_updated_at
BEFORE UPDATE ON agent_knowledge
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE INDEX IF NOT EXISTS idx_agent_knowledge_name ON agent_knowledge(name);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_source_type ON agent_knowledge(source_type);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_deleted_at ON agent_knowledge(deleted_at);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_active_not_deleted ON agent_knowledge (active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_metadata_jsonb ON agent_knowledge USING gin (metadata jsonb_ops);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_name_not_deleted ON agent_knowledge (name) WHERE deleted_at IS NULL;

-- =======================
-- 4. knowledge_chunk (pgvector)
-- =======================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_id  UUID NOT NULL,
    content       TEXT NOT NULL,
    metadata      JSONB,
    embedding     vector(1536),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_knowledge_chunk_knowledge_id FOREIGN KEY (knowledge_id)
        REFERENCES agent_knowledge(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding
  ON knowledge_chunk USING ivfflat (embedding vector_l2_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_knowledge_id ON knowledge_chunk (knowledge_id);
CREATE INDEX IF NOT EXISTS idx_chunk_metadata_jsonb ON knowledge_chunk USING gin (metadata jsonb_ops);

-- =======================
-- 5. agent_tool_mapping
-- =======================
CREATE TABLE IF NOT EXISTS agent_tool_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    tool_id UUID NOT NULL REFERENCES agent_tool(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_agent_tool UNIQUE (agent_id, tool_id)
);

CREATE TRIGGER trg_agent_tool_mapping_updated_at
BEFORE UPDATE ON agent_tool_mapping
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE INDEX IF NOT EXISTS idx_agent_tool_mapping_agent_id ON agent_tool_mapping(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_tool_mapping_tool_id ON agent_tool_mapping(tool_id);

-- =======================
-- 6. agent_knowledge_mapping
-- =======================
CREATE TABLE IF NOT EXISTS agent_knowledge_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    knowledge_id UUID NOT NULL REFERENCES agent_knowledge(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_agent_knowledge UNIQUE (agent_id, knowledge_id)
);

CREATE TRIGGER trg_agent_knowledge_mapping_updated_at
BEFORE UPDATE ON agent_knowledge_mapping
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

CREATE INDEX IF NOT EXISTS idx_agent_knowledge_mapping_agent_id ON agent_knowledge_mapping(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_mapping_knowledge_id ON agent_knowledge_mapping(knowledge_id);

-- ====================================================================
-- === DOWN Migration (Manual Rollback) ===
-- ====================================================================
-- DROP TRIGGER IF EXISTS trg_agent_knowledge_mapping_updated_at ON agent_knowledge_mapping;
-- DROP TRIGGER IF EXISTS trg_agent_tool_mapping_updated_at ON agent_tool_mapping;
-- DROP TRIGGER IF EXISTS trg_agent_knowledge_updated_at ON agent_knowledge;
-- DROP TRIGGER IF EXISTS trg_agent_tool_updated_at ON agent_tool;
-- DROP TRIGGER IF EXISTS trg_agent_updated_at ON agent;
-- DROP FUNCTION IF EXISTS update_timestamp;
-- DROP TABLE IF EXISTS agent_knowledge_mapping;
-- DROP TABLE IF EXISTS agent_tool_mapping;
-- DROP TABLE IF EXISTS knowledge_chunk;
-- DROP TABLE IF EXISTS agent_knowledge;
-- DROP TABLE IF EXISTS agent_tool;
-- DROP TABLE IF EXISTS agent;
-- DROP EXTENSION IF EXISTS vector;
-- DROP EXTENSION IF EXISTS pgcrypto;
-- ====================================================================
