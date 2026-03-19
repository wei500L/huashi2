CREATE TABLE IF NOT EXISTS lexical_pair
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    english_word            VARCHAR(128)  NOT NULL,
    french_word             VARCHAR(128)  NOT NULL,
    chinese_gloss           VARCHAR(255)  NOT NULL,
    lexical_pair_type       VARCHAR(32)   NOT NULL,
    semantic_overlap_score  DECIMAL(5, 4) NOT NULL,
    false_friend_risk       DECIMAL(5, 4) NOT NULL,
    default_context_support VARCHAR(16)   NOT NULL,
    difficulty_level        INT           NOT NULL,
    notes                   TEXT          NULL,
    source                  VARCHAR(255)  NULL,
    searchable_text         TEXT          NOT NULL,
    knowledge_status        VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    embedding_status        VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    last_embedded_at        TIMESTAMP     NULL,
    active                  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT        NULL,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT        NULL,
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_lexical_pair_word_pair UNIQUE (english_word, french_word, deleted)
);

CREATE TABLE IF NOT EXISTS lexical_pair_sense
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    lexical_pair_id    BIGINT       NOT NULL,
    sort_order         INT          NOT NULL DEFAULT 1,
    english_definition VARCHAR(500) NULL,
    french_definition  VARCHAR(500) NULL,
    chinese_definition VARCHAR(500) NULL,
    searchable_text    TEXT         NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         BIGINT       NULL,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT       NULL,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_pair_sense_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id)
);

CREATE TABLE IF NOT EXISTS lexical_pair_example
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    lexical_pair_sense_id BIGINT       NOT NULL,
    sort_order            INT          NOT NULL DEFAULT 1,
    english_example       VARCHAR(1000) NULL,
    french_example        VARCHAR(1000) NULL,
    chinese_translation   VARCHAR(1000) NULL,
    context_support_level VARCHAR(16)  NOT NULL,
    source                VARCHAR(255) NULL,
    searchable_text       TEXT         NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT       NULL,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT       NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_pair_example_sense_id FOREIGN KEY (lexical_pair_sense_id) REFERENCES lexical_pair_sense (id)
);

CREATE TABLE IF NOT EXISTS lexical_tag
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name    VARCHAR(64)  NOT NULL,
    description VARCHAR(255) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT       NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT       NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_lexical_tag_name UNIQUE (tag_name, deleted)
);

CREATE TABLE IF NOT EXISTS lexical_pair_tag_rel
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    lexical_pair_id BIGINT    NOT NULL,
    lexical_tag_id  BIGINT    NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT    NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT    NULL,
    deleted         BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_pair_tag_rel_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT fk_lexical_pair_tag_rel_tag_id FOREIGN KEY (lexical_tag_id) REFERENCES lexical_tag (id),
    CONSTRAINT uk_lexical_pair_tag_rel UNIQUE (lexical_pair_id, lexical_tag_id, deleted)
);

CREATE TABLE IF NOT EXISTS lexical_list
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    list_name     VARCHAR(128) NOT NULL,
    description   VARCHAR(255) NULL,
    owner_user_id BIGINT       NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT       NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT       NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_list_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS lexical_list_item
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    lexical_list_id BIGINT       NOT NULL,
    lexical_pair_id BIGINT       NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 1,
    notes           VARCHAR(255) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lexical_list_item_list_id FOREIGN KEY (lexical_list_id) REFERENCES lexical_list (id),
    CONSTRAINT fk_lexical_list_item_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT uk_lexical_list_item UNIQUE (lexical_list_id, lexical_pair_id, deleted)
);

CREATE INDEX idx_lexical_pair_type_active ON lexical_pair (lexical_pair_type, active);
CREATE INDEX idx_lexical_pair_context_active ON lexical_pair (default_context_support, active);
CREATE INDEX idx_lexical_pair_risk ON lexical_pair (false_friend_risk);
CREATE INDEX idx_lexical_pair_knowledge_embedding ON lexical_pair (knowledge_status, embedding_status);
CREATE INDEX idx_lexical_pair_english_word ON lexical_pair (english_word);
CREATE INDEX idx_lexical_pair_french_word ON lexical_pair (french_word);
CREATE INDEX idx_lexical_pair_chinese_gloss ON lexical_pair (chinese_gloss);
CREATE INDEX idx_lexical_pair_sense_pair_id ON lexical_pair_sense (lexical_pair_id, sort_order);
CREATE INDEX idx_lexical_pair_example_sense_id ON lexical_pair_example (lexical_pair_sense_id, sort_order);
CREATE INDEX idx_lexical_pair_tag_rel_pair_id ON lexical_pair_tag_rel (lexical_pair_id);
CREATE INDEX idx_lexical_list_owner_user_id ON lexical_list (owner_user_id, active);
CREATE INDEX idx_lexical_list_item_list_id ON lexical_list_item (lexical_list_id, sort_order);
