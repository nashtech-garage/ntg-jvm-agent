-- ============================================================
-- Convert embedding column from vector(N) → float8[]
-- Compatible with Flyway
-- ============================================================

-- 1) Drop any index referencing embedding (vector index)
DO $$
DECLARE
    idx TEXT;
BEGIN
    FOR idx IN
        SELECT indexname
        FROM pg_indexes
        WHERE tablename = 'knowledge_chunk'
          AND indexdef LIKE '%embedding%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I', idx);
    END LOOP;
END $$;


-- 2) Create conversion function vector → float8[] (if missing)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_proc
        WHERE proname = 'vector_to_float8_array'
    ) THEN
        CREATE FUNCTION vector_to_float8_array(v vector)
        RETURNS float8[] AS $fn$
        BEGIN
            RETURN ARRAY(
                SELECT v[i]::float8
                FROM generate_subscripts(v, 1) AS i
            );
        END;
        $fn$ LANGUAGE plpgsql IMMUTABLE STRICT;
    END IF;
END $$;


-- 3) Convert embedding column type
ALTER TABLE knowledge_chunk
    ALTER COLUMN embedding TYPE float8[]
    USING vector_to_float8_array(embedding);


-- 4) Document change
COMMENT ON COLUMN knowledge_chunk.embedding IS
'Converted from pgvector to float8[] to support variable embedding dimensions.';
