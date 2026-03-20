CREATE TABLE IF NOT EXISTS knowledge_document
(
    id                BIGSERIAL PRIMARY KEY,
    source_type       VARCHAR(64)  NOT NULL,
    source_id         VARCHAR(128) NOT NULL,
    title             VARCHAR(255) NOT NULL,
    source_updated_at TIMESTAMPTZ  NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    content_hash      VARCHAR(128) NOT NULL,
    metadata          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_knowledge_document_source UNIQUE (source_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_document_source_type ON knowledge_document (source_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_active ON knowledge_document (active);

CREATE TABLE IF NOT EXISTS knowledge_chunk
(
    id               BIGSERIAL PRIMARY KEY,
    document_id      BIGINT       NOT NULL REFERENCES knowledge_document (id) ON DELETE CASCADE,
    chunk_key        VARCHAR(160) NOT NULL,
    chunk_order      INTEGER      NOT NULL DEFAULT 0,
    source_type      VARCHAR(64)  NOT NULL,
    source_id        VARCHAR(128) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    content          TEXT         NOT NULL,
    metadata         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    embedding_status VARCHAR(32)  NOT NULL,
    embedded_at      TIMESTAMPTZ  NULL,
    content_hash     VARCHAR(128) NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_knowledge_chunk_key UNIQUE (document_id, chunk_key)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_source ON knowledge_chunk (source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_status ON knowledge_chunk (embedding_status);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_active ON knowledge_chunk (active);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_metadata_gin ON knowledge_chunk USING gin (metadata);

CREATE TABLE IF NOT EXISTS chunk_embedding
(
    id                  BIGSERIAL PRIMARY KEY,
    chunk_id            BIGINT       NOT NULL REFERENCES knowledge_chunk (id) ON DELETE CASCADE,
    embedding_model     VARCHAR(128) NOT NULL,
    embedding_dimension INTEGER      NOT NULL,
    embedding           VECTOR(1024) NOT NULL,
    content_hash        VARCHAR(128) NOT NULL,
    is_current          BOOLEAN      NOT NULL DEFAULT TRUE,
    embedded_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_chunk_embedding_current_unique ON chunk_embedding (chunk_id) WHERE is_current = TRUE;
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_current ON chunk_embedding (is_current);
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_vector_hnsw
    ON chunk_embedding
    USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS ingestion_job
(
    id                   BIGSERIAL PRIMARY KEY,
    job_type             VARCHAR(64)  NOT NULL,
    mode                 VARCHAR(32)  NOT NULL,
    status               VARCHAR(32)  NOT NULL,
    requested_by         VARCHAR(128) NULL,
    source_types         JSONB        NOT NULL DEFAULT '[]'::jsonb,
    source_ids           JSONB        NOT NULL DEFAULT '[]'::jsonb,
    started_at           TIMESTAMPTZ  NULL,
    finished_at          TIMESTAMPTZ  NULL,
    last_cursor          VARCHAR(512) NULL,
    last_source_updated_at TIMESTAMPTZ NULL,
    stats                JSONB        NOT NULL DEFAULT '{}'::jsonb,
    error_message        TEXT         NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ingestion_job_status ON ingestion_job (status);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_finished_at ON ingestion_job (finished_at);
