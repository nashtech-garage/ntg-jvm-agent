CREATE TABLE token_usage_log (
    id                  UUID PRIMARY KEY,
    user_id             UUID REFERENCES users(id),-- it's NULL for background embedding jobs
    agent_id            UUID NOT NULL REFERENCES agent(id),
    organization_id     UUID,

    provider            VARCHAR(32) NOT NULL,-- OPENAI, GITHUB, LOCAL
    model               VARCHAR(128) NOT NULL,

    operation           VARCHAR(30) NOT NULL,
    tool_name           VARCHAR(128),-- optional (for MCP / function tools)

    prompt_tokens       BIGINT NOT NULL,
    completion_tokens   BIGINT NOT NULL,
    total_tokens        BIGINT NOT NULL,

    correlation_id      VARCHAR(128) NOT NULL,-- idempotency / retries

    conversation_id     UUID,
    message_id          UUID,
    job_id              UUID,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    usage_date DATE NOT NULL GENERATED ALWAYS AS (
        (created_at AT TIME ZONE 'UTC')::DATE
    ) STORED,

    CONSTRAINT chk_token_usage_log_token_math CHECK (
        total_tokens = prompt_tokens + completion_tokens
    ),
    CONSTRAINT chk_token_usage_log_operation CHECK (
        operation IN ('CHAT','EMBEDDING','TOOL','RERANK','SUMMARIZATION')
    )
);

-- correlation_id represents a single logical execution.
-- Retries MUST reuse the same correlation_id and MUST NOT
-- produce additional token usage rows.
CREATE UNIQUE INDEX uniq_token_usage_corr_op
ON token_usage_log (correlation_id, operation);

CREATE INDEX idx_token_usage_user_time
ON token_usage_log (user_id, created_at);

CREATE INDEX idx_token_usage_agent_time
ON token_usage_log (agent_id, created_at);

CREATE INDEX idx_token_usage_correlation
ON token_usage_log (correlation_id);

CREATE INDEX idx_token_usage_log_usage_date
ON token_usage_log (usage_date);

CREATE INDEX idx_token_usage_log_daily_agg
ON token_usage_log (
    usage_date,
    agent_id,
    user_id,
    provider,
    operation,
    model
);

CREATE TABLE user_token_quota (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),

    daily_limit     BIGINT NOT NULL CHECK (daily_limit > 0),

    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMPTZ,
    updated_by      UUID REFERENCES users(id),

    CONSTRAINT uniq_user_token_quota_user UNIQUE (user_id)
);

CREATE TABLE token_usage_daily (
    usage_date        DATE NOT NULL,
    agent_id          UUID NOT NULL,
    user_id           UUID NULL,

    provider          VARCHAR(32) NOT NULL,
    model             VARCHAR(100) NOT NULL,
    operation         VARCHAR(50) NOT NULL,

    prompt_tokens     BIGINT NOT NULL DEFAULT 0,
    completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens      BIGINT NOT NULL DEFAULT 0,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (usage_date, agent_id, user_id, provider, operation, model),
    CONSTRAINT chk_token_usage_daily_token_math
        CHECK (total_tokens = prompt_tokens + completion_tokens)
);

CREATE TABLE usage_aggregation_state (
    id SMALLINT PRIMARY KEY,
    last_processed_date DATE NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT usage_aggregation_state_singleton CHECK (id = 1)
);

INSERT INTO usage_aggregation_state (id, last_processed_date)
VALUES (1, CURRENT_DATE - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;



