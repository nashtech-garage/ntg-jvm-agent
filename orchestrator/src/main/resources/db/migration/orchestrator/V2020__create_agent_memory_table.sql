ALTER TABLE users
    ADD COLUMN memory_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE agent_memory (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    agent_id     UUID REFERENCES agent(id),
    type         VARCHAR(20) NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  TEXT NOT NULL,
    content      TEXT NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    version      INTEGER NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by   UUID REFERENCES users(id),
    updated_at   TIMESTAMPTZ,
    updated_by   UUID REFERENCES users(id),
    CONSTRAINT chk_agent_memory_type CHECK (
        type IN ('user', 'feedback', 'project', 'reference')
    ),
    CONSTRAINT chk_agent_memory_name CHECK (
        name ~ '^[a-z0-9]+(-[a-z0-9]+)*$'
    )
);

CREATE UNIQUE INDEX uk_agent_memory_scope_name_not_deleted
    ON agent_memory (user_id, agent_id, name) NULLS NOT DISTINCT
    WHERE deleted_at IS NULL;

CREATE INDEX idx_agent_memory_user_type
    ON agent_memory (user_id, type);

CREATE INDEX idx_agent_memory_user_agent
    ON agent_memory (user_id, agent_id)
    WHERE deleted_at IS NULL;
