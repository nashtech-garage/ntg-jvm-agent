CREATE TABLE IF NOT EXISTS ingestion_job (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id           UUID NOT NULL REFERENCES agent(id) ON DELETE CASCADE,
    agent_knowledge_id UUID NOT NULL REFERENCES agent_knowledge(id) ON DELETE CASCADE,
    status             VARCHAR(30) NOT NULL,
    attempts           SMALLINT NOT NULL DEFAULT 0,
    max_attempts       SMALLINT NOT NULL DEFAULT 3,
    error_message      TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ,
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,

    CONSTRAINT chk_ingestion_job_status CHECK (
        status IN ('PENDING','RUNNING','SUCCESS','FAILED')
    )
);

CREATE INDEX IF NOT EXISTS idx_ingestion_job_status ON ingestion_job(status);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_status_created ON ingestion_job (status, created_at);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_knowledge_id ON ingestion_job (agent_knowledge_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_agent_id ON ingestion_job (agent_id);
