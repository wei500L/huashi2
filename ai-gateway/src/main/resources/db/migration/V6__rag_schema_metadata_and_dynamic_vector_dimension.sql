CREATE TABLE IF NOT EXISTS rag_schema_metadata
(
    id                   SMALLINT PRIMARY KEY,
    embedding_dimension  INTEGER      NOT NULL,
    hnsw_m               INTEGER      NOT NULL,
    hnsw_ef_construction INTEGER      NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_rag_schema_metadata_singleton CHECK (id = 1)
);

DO
$$
    DECLARE
        current_dimension INTEGER;
    BEGIN
        SELECT substring(format_type(attribute.atttypid, attribute.atttypmod) FROM 'vector\((\d+)\)')::INTEGER
        INTO current_dimension
        FROM pg_attribute attribute
                 JOIN pg_class relation ON relation.oid = attribute.attrelid
                 JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
        WHERE namespace.nspname = current_schema()
          AND relation.relname = 'chunk_embedding'
          AND attribute.attname = 'embedding'
          AND attribute.attnum > 0
          AND NOT attribute.attisdropped;

        IF current_dimension IS NULL THEN
            RAISE EXCEPTION 'chunk_embedding.embedding vector column was not found';
        END IF;

        IF current_dimension <> ${rag_embedding_dimension} THEN
            DELETE FROM chunk_embedding;

            EXECUTE format(
                    'ALTER TABLE chunk_embedding ALTER COLUMN embedding TYPE VECTOR(%1$s) USING embedding::VECTOR(%1$s)',
                    ${rag_embedding_dimension}
            );

            UPDATE knowledge_chunk
            SET embedding_status = 'PENDING',
                embedded_at      = NULL,
                updated_at       = CURRENT_TIMESTAMP
            WHERE active = TRUE;
        END IF;
    END
$$;

DROP INDEX IF EXISTS idx_chunk_embedding_vector_hnsw;

CREATE INDEX idx_chunk_embedding_vector_hnsw
    ON chunk_embedding
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = ${rag_hnsw_m}, ef_construction = ${rag_hnsw_ef_construction});

INSERT INTO rag_schema_metadata (id, embedding_dimension, hnsw_m, hnsw_ef_construction, updated_at)
VALUES (1, ${rag_embedding_dimension}, ${rag_hnsw_m}, ${rag_hnsw_ef_construction}, CURRENT_TIMESTAMP)
ON CONFLICT (id)
    DO UPDATE SET embedding_dimension  = EXCLUDED.embedding_dimension,
                  hnsw_m               = EXCLUDED.hnsw_m,
                  hnsw_ef_construction = EXCLUDED.hnsw_ef_construction,
                  updated_at           = CURRENT_TIMESTAMP;
