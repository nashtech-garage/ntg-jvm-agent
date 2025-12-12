CREATE TABLE IF NOT EXISTS embedding_job (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id           UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    agent_knowledge_id UUID NOT NULL REFERENCES agent_knowledge(id) ON DELETE CASCADE,
    chunk_id           UUID NOT NULL REFERENCES knowledge_chunk(id) ON DELETE CASCADE,
    status             VARCHAR(30) NOT NULL,
    attempts           SMALLINT NOT NULL DEFAULT 0,
    max_attempts       SMALLINT NOT NULL DEFAULT 3,
    error_message      TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ,
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,
    next_attempt_at    TIMESTAMPTZ NULL,

    CONSTRAINT chk_embedding_job_status CHECK (
        status IN ('PENDING','RUNNING','SUCCESS','FAILED')
    )
);

CREATE INDEX IF NOT EXISTS idx_embedding_job_status_created
    ON embedding_job (status, created_at);

CREATE INDEX IF NOT EXISTS idx_embedding_job_chunk_id
    ON embedding_job (chunk_id);

CREATE INDEX IF NOT EXISTS idx_embedding_job_agent_knowledge_id
    ON embedding_job (agent_knowledge_id);

CREATE INDEX IF NOT EXISTS idx_embedding_job_agent_id
    ON embedding_job (agent_id);
