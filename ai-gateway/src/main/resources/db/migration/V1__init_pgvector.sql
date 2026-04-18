CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS rag_knowledge_document
(
    id               BIGSERIAL PRIMARY KEY,
    knowledge_type   VARCHAR(64)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    content          TEXT         NOT NULL,
    source_uri       VARCHAR(512) NULL,
    source_label     VARCHAR(255) NULL,
    embedding        VECTOR(1024) NULL,
    metadata         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rag_knowledge_document_type ON rag_knowledge_document (knowledge_type);
CREATE INDEX IF NOT EXISTS idx_rag_knowledge_document_source ON rag_knowledge_document (source_uri);
CREATE INDEX IF NOT EXISTS idx_rag_knowledge_document_embedding
    ON rag_knowledge_document
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 128);
