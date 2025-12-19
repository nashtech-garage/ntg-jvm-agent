CREATE TABLE token_usage_log (
    id                  UUID PRIMARY KEY,
    user_id             UUID REFERENCES users(id),
    agent_id            UUID REFERENCES agent(id),
    organization_id     UUID,

    provider            VARCHAR(32) NOT NULL,-- OPENAI, GITHUB, LOCAL
    model               VARCHAR(128) NOT NULL,

    operation           VARCHAR(30) NOT NULL,
    tool_name           VARCHAR(128),-- optional (for MCP / function tools)

    prompt_tokens       INT NOT NULL,
    completion_tokens   INT NOT NULL,
    total_tokens        INT NOT NULL,

    correlation_id      VARCHAR(128) NOT NULL,-- idempotency / retries

    conversation_id     UUID,
    message_id          UUID,
    job_id              UUID,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CHECK (total_tokens = prompt_tokens + completion_tokens),
    CONSTRAINT chk_token_usage_log_operation CHECK (
        operation IN ('CHAT','EMBEDDING','TOOL','RERANK','SUMMARIZATION')
    )
);

CREATE UNIQUE INDEX uniq_token_usage_corr_op
ON token_usage_log (correlation_id, operation);

CREATE INDEX idx_token_usage_user_time
ON token_usage_log (user_id, created_at);

CREATE INDEX idx_token_usage_agent_time
ON token_usage_log (agent_id, created_at);

CREATE INDEX idx_token_usage_correlation
ON token_usage_log (correlation_id);


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


