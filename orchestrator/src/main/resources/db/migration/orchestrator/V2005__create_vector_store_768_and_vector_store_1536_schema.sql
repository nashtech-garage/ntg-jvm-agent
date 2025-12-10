DROP TABLE IF EXISTS vector_store;

CREATE TABLE IF NOT EXISTS vector_store_768 (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(768)
);

CREATE TABLE IF NOT EXISTS vector_store_1536 (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);
