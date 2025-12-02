CREATE TABLE IF NOT EXISTS knowledge_files (
    id UUID PRIMARY KEY,
    knowledge_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_path TEXT NOT NULL,
    file_size BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id),

    CONSTRAINT fk_knowledge_files_knowledge
        FOREIGN KEY (knowledge_id)
        REFERENCES agent_knowledge(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_knowledge_files_knowledge_id
    ON knowledge_files (knowledge_id);
