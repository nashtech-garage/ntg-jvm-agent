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
    description         TEXT,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    provider            VARCHAR(50) NOT NULL,
    base_url            VARCHAR(150) NOT NULL,
    api_key             VARCHAR(200) NOT NULL,
    chat_completions_path VARCHAR(50) NOT NULL,
    model               VARCHAR(100) NOT NULL,
    embedding_model     VARCHAR(50) NOT NULL,
    dimension           INTEGER NOT NULL,
    embeddings_path     VARCHAR(50) NOT NULL,
    top_p               NUMERIC(3,2) NOT NULL DEFAULT 1.0 CHECK (top_p >= 0 AND top_p <= 1),
    temperature         NUMERIC(3,2) NOT NULL DEFAULT 0.7 CHECK (temperature >= 0 AND temperature <= 2),
    max_tokens          INTEGER NOT NULL DEFAULT 2048 CHECK (max_tokens > 0),
    frequency_penalty   NUMERIC(3,2) NOT NULL DEFAULT 0.0 CHECK (frequency_penalty BETWEEN -2 AND 2),
    presence_penalty    NUMERIC(3,2) NOT NULL DEFAULT 0.0 CHECK (presence_penalty BETWEEN -2 AND 2),
    settings            JSONB,
    version             INTEGER NOT NULL DEFAULT 0,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ,
    updated_by          UUID REFERENCES users(id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_agent_name ON agent(name);
CREATE INDEX IF NOT EXISTS idx_agent_model ON agent(model);
CREATE INDEX IF NOT EXISTS idx_agent_provider ON agent(provider);
CREATE INDEX IF NOT EXISTS idx_agent_active ON agent(active);
CREATE INDEX IF NOT EXISTS idx_agent_deleted_at ON agent(deleted_at);
CREATE INDEX IF NOT EXISTS idx_agent_active_not_deleted ON agent (active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_agent_settings_jsonb ON agent USING gin (settings);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_name_not_deleted ON agent (lower(name)) WHERE deleted_at IS NULL;

-- =======================
-- 2. tool
-- =======================
CREATE TABLE IF NOT EXISTS tool (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    type         VARCHAR(50),
    description  TEXT,
    definition   JSONB,
    base_url     VARCHAR(200) NULL,
    connection_config   JSONB NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by   UUID REFERENCES users(id),
    updated_at   TIMESTAMPTZ,
    updated_by   UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_tool_name ON tool(name);
CREATE INDEX IF NOT EXISTS idx_tool_type ON tool(type);
CREATE INDEX IF NOT EXISTS idx_tool_active ON tool(active);
CREATE INDEX IF NOT EXISTS idx_tool_deleted_at ON tool(deleted_at);
CREATE INDEX IF NOT EXISTS idx_tool_active_not_deleted ON tool (active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tool_definition_jsonb ON tool USING gin (definition);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tool_name_not_deleted ON tool (lower(name)) WHERE deleted_at IS NULL;

-- =======================
-- 3. agent_tool
-- =======================
CREATE TABLE IF NOT EXISTS agent_tool (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id   UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    tool_id    UUID NOT NULL REFERENCES tool(id) ON DELETE CASCADE,
    config     JSONB,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES users(id),
    CONSTRAINT uk_agent_tool UNIQUE (agent_id, tool_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_tool_agent_id ON agent_tool(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_tool_tool_id ON agent_tool(tool_id);
CREATE INDEX IF NOT EXISTS idx_agent_tool_active ON agent_tool(active);

-- =======================
-- 4. agent_knowledge
-- =======================
CREATE TABLE IF NOT EXISTS agent_knowledge (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id        UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    source_type     VARCHAR(50) NOT NULL,
    source_uri      TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ,
    updated_by      UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_agent_knowledge_agent_id ON agent_knowledge(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_name ON agent_knowledge(name);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_source_type ON agent_knowledge(source_type);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_deleted_at ON agent_knowledge(deleted_at);
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_active_not_deleted ON agent_knowledge (active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_agent_knowledge_metadata_jsonb ON agent_knowledge USING gin (metadata);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_name_per_agent ON agent_knowledge (agent_id, name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_source_uri_per_agent ON agent_knowledge (agent_id, source_uri) WHERE deleted_at IS NULL AND source_uri IS NOT NULL;

-- =======================
-- 5. knowledge_chunk (pgvector)
-- =======================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_id    UUID NOT NULL,
    chunk_order     INTEGER NOT NULL,
    content         TEXT NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding_768   vector(768),
    embedding_1536  vector(1536),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ,
    updated_by      UUID REFERENCES users(id),
    CONSTRAINT fk_knowledge_chunk_knowledge_id FOREIGN KEY (knowledge_id)
        REFERENCES agent_knowledge(id) ON DELETE CASCADE,
    CONSTRAINT uq_knowledge_chunk_order UNIQUE (knowledge_id, chunk_order)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_768
  ON knowledge_chunk USING ivfflat (embedding_768 vector_l2_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_1536
  ON knowledge_chunk USING ivfflat (embedding_1536 vector_l2_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_knowledge_id ON knowledge_chunk (knowledge_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_order ON knowledge_chunk (knowledge_id, chunk_order);
CREATE INDEX IF NOT EXISTS idx_chunk_metadata_jsonb ON knowledge_chunk USING gin (metadata);
