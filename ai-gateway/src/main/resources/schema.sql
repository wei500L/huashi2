CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;








CREATE TABLE public.chunk_embedding (
    id bigint NOT NULL,
    chunk_id bigint NOT NULL,
    embedding_model character varying(128) NOT NULL,
    embedding_dimension integer NOT NULL,
    embedding public.vector(1024) NOT NULL,
    content_hash character varying(128) NOT NULL,
    is_current boolean DEFAULT true NOT NULL,
    embedded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_chunk_embedding_dimension CHECK ((embedding_dimension = 1024) AND (public.vector_dims(embedding) = embedding_dimension))
);



CREATE SEQUENCE public.chunk_embedding_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.chunk_embedding_id_seq OWNED BY public.chunk_embedding.id;



CREATE TABLE public.ingestion_job (
    id bigint NOT NULL,
    job_type character varying(64) NOT NULL,
    mode character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    requested_by character varying(128),
    source_types jsonb DEFAULT '[]'::jsonb NOT NULL,
    source_ids jsonb DEFAULT '[]'::jsonb NOT NULL,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    last_cursor character varying(512),
    last_source_updated_at timestamp with time zone,
    stats jsonb DEFAULT '{}'::jsonb NOT NULL,
    error_message text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



CREATE SEQUENCE public.ingestion_job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.ingestion_job_id_seq OWNED BY public.ingestion_job.id;



CREATE TABLE public.integration_consume_record (
    id bigint NOT NULL,
    consumer_name character varying(128) NOT NULL,
    event_id character varying(128) NOT NULL,
    event_type character varying(128) NOT NULL,
    status character varying(32) NOT NULL,
    error_message text,
    processed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



CREATE SEQUENCE public.integration_consume_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.integration_consume_record_id_seq OWNED BY public.integration_consume_record.id;



CREATE TABLE public.knowledge_chunk (
    id bigint NOT NULL,
    document_id bigint NOT NULL,
    chunk_key character varying(160) NOT NULL,
    chunk_order integer DEFAULT 0 NOT NULL,
    source_type character varying(64) NOT NULL,
    source_id character varying(128) NOT NULL,
    title character varying(255) NOT NULL,
    content text NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    embedding_status character varying(32) NOT NULL,
    embedded_at timestamp with time zone,
    content_hash character varying(128) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_knowledge_chunk_embedding_status CHECK (embedding_status IN ('PENDING', 'EMBEDDED', 'FAILED'))
);



CREATE SEQUENCE public.knowledge_chunk_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.knowledge_chunk_id_seq OWNED BY public.knowledge_chunk.id;



CREATE TABLE public.knowledge_document (
    id bigint NOT NULL,
    source_type character varying(64) NOT NULL,
    source_id character varying(128) NOT NULL,
    title character varying(255) NOT NULL,
    source_updated_at timestamp with time zone,
    active boolean DEFAULT true NOT NULL,
    content_hash character varying(128) NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);



CREATE SEQUENCE public.knowledge_document_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.knowledge_document_id_seq OWNED BY public.knowledge_document.id;



CREATE TABLE public.rag_schema_metadata (
    id smallint NOT NULL,
    embedding_dimension integer NOT NULL,
    hnsw_m integer NOT NULL,
    hnsw_ef_construction integer NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_rag_schema_metadata_singleton CHECK ((id = 1))
);



ALTER TABLE ONLY public.chunk_embedding ALTER COLUMN id SET DEFAULT nextval('public.chunk_embedding_id_seq'::regclass);



ALTER TABLE ONLY public.ingestion_job ALTER COLUMN id SET DEFAULT nextval('public.ingestion_job_id_seq'::regclass);



ALTER TABLE ONLY public.integration_consume_record ALTER COLUMN id SET DEFAULT nextval('public.integration_consume_record_id_seq'::regclass);



ALTER TABLE ONLY public.knowledge_chunk ALTER COLUMN id SET DEFAULT nextval('public.knowledge_chunk_id_seq'::regclass);



ALTER TABLE ONLY public.knowledge_document ALTER COLUMN id SET DEFAULT nextval('public.knowledge_document_id_seq'::regclass);



ALTER TABLE ONLY public.chunk_embedding
    ADD CONSTRAINT chunk_embedding_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.ingestion_job
    ADD CONSTRAINT ingestion_job_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.integration_consume_record
    ADD CONSTRAINT integration_consume_record_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.knowledge_chunk
    ADD CONSTRAINT knowledge_chunk_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.knowledge_document
    ADD CONSTRAINT knowledge_document_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.rag_schema_metadata
    ADD CONSTRAINT rag_schema_metadata_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.integration_consume_record
    ADD CONSTRAINT uk_integration_consume_record UNIQUE (consumer_name, event_id);



ALTER TABLE ONLY public.knowledge_chunk
    ADD CONSTRAINT uk_knowledge_chunk_key UNIQUE (document_id, chunk_key);



ALTER TABLE ONLY public.knowledge_document
    ADD CONSTRAINT uk_knowledge_document_source UNIQUE (source_type, source_id);



CREATE INDEX idx_chunk_embedding_current ON public.chunk_embedding USING btree (is_current);



CREATE UNIQUE INDEX idx_chunk_embedding_current_unique ON public.chunk_embedding USING btree (chunk_id) WHERE (is_current = true);



CREATE INDEX idx_chunk_embedding_vector_hnsw ON public.chunk_embedding USING hnsw (embedding public.vector_cosine_ops) WITH (m='16', ef_construction='128') WHERE (is_current = true);



CREATE INDEX idx_ingestion_job_finished_at ON public.ingestion_job USING btree (finished_at);



CREATE INDEX idx_ingestion_job_status ON public.ingestion_job USING btree (status);



CREATE INDEX idx_integration_consume_record_status ON public.integration_consume_record USING btree (consumer_name, status, processed_at DESC);



CREATE INDEX idx_knowledge_chunk_active ON public.knowledge_chunk USING btree (active);



CREATE INDEX idx_knowledge_chunk_document_active_order ON public.knowledge_chunk USING btree (document_id, active, chunk_order);



CREATE INDEX idx_knowledge_chunk_embedding_status ON public.knowledge_chunk USING btree (embedding_status);



CREATE INDEX idx_knowledge_chunk_metadata_gin ON public.knowledge_chunk USING gin (metadata);



CREATE INDEX idx_knowledge_chunk_source ON public.knowledge_chunk USING btree (source_type, source_id);



CREATE INDEX idx_knowledge_chunk_source_active_id ON public.knowledge_chunk USING btree (source_type, active, source_id);



CREATE INDEX idx_knowledge_document_active ON public.knowledge_document USING btree (active);



CREATE INDEX idx_knowledge_document_source_active_id ON public.knowledge_document USING btree (source_type, active, source_id);



CREATE INDEX idx_knowledge_document_source_type ON public.knowledge_document USING btree (source_type);



ALTER TABLE ONLY public.chunk_embedding
    ADD CONSTRAINT chunk_embedding_chunk_id_fkey FOREIGN KEY (chunk_id) REFERENCES public.knowledge_chunk(id) ON DELETE CASCADE;



ALTER TABLE ONLY public.knowledge_chunk
    ADD CONSTRAINT knowledge_chunk_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.knowledge_document(id) ON DELETE CASCADE;

INSERT INTO public.rag_schema_metadata (id, embedding_dimension, hnsw_m, hnsw_ef_construction, updated_at)
VALUES (1, 1024, 16, 128, CURRENT_TIMESTAMP)
ON CONFLICT (id)
    DO UPDATE SET embedding_dimension = EXCLUDED.embedding_dimension,
                  hnsw_m = EXCLUDED.hnsw_m,
                  hnsw_ef_construction = EXCLUDED.hnsw_ef_construction,
                  updated_at = CURRENT_TIMESTAMP;
