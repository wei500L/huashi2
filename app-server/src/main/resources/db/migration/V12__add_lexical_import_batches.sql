CREATE TABLE IF NOT EXISTS lexical_import_batch
(
    id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id          BIGINT       NOT NULL,
    original_filename      VARCHAR(255) NOT NULL,
    content_type           VARCHAR(255) NULL,
    file_size_bytes        BIGINT       NOT NULL,
    source_format          VARCHAR(16)  NOT NULL,
    status                 VARCHAR(32)  NOT NULL,
    total_rows             INT          NOT NULL DEFAULT 0,
    ready_rows             INT          NOT NULL DEFAULT 0,
    invalid_rows           INT          NOT NULL DEFAULT 0,
    skipped_rows           INT          NOT NULL DEFAULT 0,
    imported_rows          INT          NOT NULL DEFAULT 0,
    error_message          TEXT         NULL,
    parser_job_started_at  TIMESTAMP    NULL,
    parser_job_finished_at TIMESTAMP    NULL,
    import_job_started_at  TIMESTAMP    NULL,
    import_job_finished_at TIMESTAMP    NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             BIGINT       NULL,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             BIGINT       NULL,
    deleted                BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_import_batch_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS lexical_import_file
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id          BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(255) NULL,
    file_size_bytes   BIGINT       NOT NULL,
    sha256            VARCHAR(64)  NOT NULL,
    file_content      LONGBLOB     NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT       NULL,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT       NULL,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_import_file_batch_id FOREIGN KEY (batch_id) REFERENCES lexical_import_batch (id),
    CONSTRAINT uk_lexical_import_file_batch UNIQUE (batch_id, deleted)
);

CREATE TABLE IF NOT EXISTS lexical_import_row
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id                BIGINT      NOT NULL,
    row_number              INT         NOT NULL,
    row_status              VARCHAR(32) NOT NULL,
    draft_json              LONGTEXT    NOT NULL,
    validation_errors_json  LONGTEXT    NULL,
    imported_lexical_pair_id BIGINT     NULL,
    import_message          TEXT        NULL,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT      NULL,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT      NULL,
    deleted                 BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_import_row_batch_id FOREIGN KEY (batch_id) REFERENCES lexical_import_batch (id),
    CONSTRAINT fk_lexical_import_row_pair_id FOREIGN KEY (imported_lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT uk_lexical_import_row_batch_row UNIQUE (batch_id, row_number, deleted)
);

CREATE INDEX idx_lexical_import_batch_owner_status_created
    ON lexical_import_batch (owner_user_id, status, created_at);
CREATE INDEX idx_lexical_import_batch_status_created
    ON lexical_import_batch (status, created_at);
CREATE INDEX idx_lexical_import_row_batch_status_row
    ON lexical_import_row (batch_id, row_status, row_number);
