CREATE TABLE embedding_tier_config (
    id                     SERIAL PRIMARY KEY,
    tier_name              VARCHAR(50) UNIQUE NOT NULL,   -- free, paid, enterprise, custom, etc.
    max_calls_per_interval INT NOT NULL,
    interval_seconds       INT NOT NULL,
    max_retries            INT NOT NULL,
    base_backoff_ms        INT NOT NULL,
    max_backoff_ms         INT NOT NULL,
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Insert defaults
INSERT INTO embedding_tier_config (tier_name, max_calls_per_interval, interval_seconds, max_retries, base_backoff_ms, max_backoff_ms)
VALUES
  ('free', 1, 10, 3, 2000, 20000),
  ('paid', 30, 1, 2, 500, 5000),
  ('enterprise', 200, 1, 1, 200, 2000);
