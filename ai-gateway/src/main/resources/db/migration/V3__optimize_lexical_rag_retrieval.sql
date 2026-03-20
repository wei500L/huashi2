CREATE INDEX IF NOT EXISTS idx_knowledge_document_source_active_id
    ON knowledge_document (source_type, active, source_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document_active_order
    ON knowledge_chunk (document_id, active, chunk_order);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_source_active_id
    ON knowledge_chunk (source_type, active, source_id);
