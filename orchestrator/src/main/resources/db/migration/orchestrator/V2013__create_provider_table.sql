CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ======================
-- 1. provider table
-- ======================
CREATE TABLE IF NOT EXISTS provider (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100) NOT NULL,
    description             TEXT,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    base_url                VARCHAR(200) NOT NULL,
    chat_completions_path   VARCHAR(100) NOT NULL DEFAULT '/v1/chat/completions',
    embeddings_path         VARCHAR(100) NOT NULL DEFAULT '/v1/embeddings',
    settings                JSONB DEFAULT '{}'::jsonb,

    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ,
    updated_by          UUID REFERENCES users(id)
);

-- ======================
-- 2. provider_embedding_model
-- ======================
CREATE TABLE IF NOT EXISTS provider_embedding_model (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id             UUID NOT NULL,
    embedding_name          VARCHAR(150) NOT NULL,
    dimension               INT NOT NULL CHECK (dimension > 0),
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    settings                JSONB NOT NULL DEFAULT '{}'::jsonb,

    UNIQUE (provider_id, embedding_name),

    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ,
    updated_by          UUID REFERENCES users(id),

    CONSTRAINT fk_provider_embedding_model
        FOREIGN KEY (provider_id) REFERENCES provider(id)
);

-- ======================
-- 3. provider_model
-- ======================
CREATE TABLE IF NOT EXISTS provider_model (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id                 UUID NOT NULL,
    model_name                  VARCHAR(150) NOT NULL,
    type                        VARCHAR(30) NOT NULL,   -- chat / completion
    context_window              INT,

    active                      BOOLEAN NOT NULL DEFAULT TRUE,

    -- LLM parameters
    default_temperature         NUMERIC(3,2) DEFAULT 0.70 CHECK (default_temperature >= 0 AND default_temperature <= 2),
    default_top_p               NUMERIC(3,2) DEFAULT 1.00 CHECK (default_top_p >= 0 AND default_top_p <= 1),
    default_max_tokens          INT DEFAULT 2048 CHECK (default_max_tokens > 0),
    default_frequency_penalty   NUMERIC(3,2) DEFAULT 0.00 CHECK (default_frequency_penalty >= -2 AND default_frequency_penalty <= 2),
    default_presence_penalty    NUMERIC(3,2) DEFAULT 0.00 CHECK (default_presence_penalty >= -2 AND default_presence_penalty <= 2),

    -- FK to embedding model
    default_embedding_model_id UUID REFERENCES provider_embedding_model(id),
    default_dimension           INT DEFAULT 1536 CHECK (default_dimension > 0),

    settings                    JSONB NOT NULL DEFAULT '{}'::jsonb,

    UNIQUE (provider_id, model_name),

    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          UUID REFERENCES users(id),
    updated_at          TIMESTAMPTZ,
    updated_by          UUID REFERENCES users(id),

    CONSTRAINT fk_provider_model
        FOREIGN KEY (provider_id) REFERENCES provider(id)
);

UPDATE agent
SET provider = 'Github Model'
WHERE name = 'Default agent';

INSERT INTO provider (id, name, description, active, base_url, chat_completions_path, embeddings_path, settings)
VALUES
	('90d97336-d461-4ed1-a19f-73b5591bc1d9','Google', 'Optional Description', true,
	'https://generativelanguage.googleapis.com', '/v1beta/openai/chat/completions', '/v1beta/openai/embeddings', '{}'::jsonb),
    ('8d1e67b9-b858-4b50-acfc-08ac324a9fa6','Github Model', 'OpenAI public API', true,
     'https://models.github.ai/inference', '/v1/chat/completions', '/embeddings', '{}'::jsonb);


INSERT INTO provider_embedding_model (provider_id, embedding_name, dimension, active, settings)
VALUES
	('90d97336-d461-4ed1-a19f-73b5591bc1d9', 'text-embedding-004', 768, true, '{}'::jsonb),
    ('8d1e67b9-b858-4b50-acfc-08ac324a9fa6', 'openai/text-embedding-3-small', 1536, true, '{}'::jsonb);

INSERT INTO provider_model (
    provider_id,
    model_name,
    type,
    context_window,
    default_temperature,
    default_top_p,
    default_max_tokens,
    default_frequency_penalty,
    default_presence_penalty,
    settings,
    active,
    default_dimension
)
VALUES
-- Google Gemini
(
    '90d97336-d461-4ed1-a19f-73b5591bc1d9',
    'gemini-2.5-flash',
    'chat',
    128000,
    0.5,
    0.9,
    2048,
    0,
    0,
    '{}'::jsonb,
    TRUE,
    768
),

-- OpenAI GPT-4o
(
    '8d1e67b9-b858-4b50-acfc-08ac324a9fa6',
    'gpt-4o',
    'chat',
    128000,
    0.7,
    1.0,
    2048,
    0,
    0,
    '{}'::jsonb,
    TRUE,
    1536
),

-- OpenAI GPT-4o-mini (lighter / cheaper model)
(
    '8d1e67b9-b858-4b50-acfc-08ac324a9fa6',
    'gpt-4o-mini',
    'chat',
    128000,
    0.7,
    1.0,
    4096,
    0,
    0,
    '{}'::jsonb,
    TRUE,
    1536
);
