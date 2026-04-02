CREATE TABLE IF NOT EXISTS assessment_paper
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_code        VARCHAR(64)   NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    description       VARCHAR(1000) NULL,
    owner_user_id     BIGINT        NOT NULL,
    status            VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    duration_minutes  INT           NOT NULL DEFAULT 30,
    question_count    INT           NOT NULL DEFAULT 0,
    total_score       INT           NOT NULL DEFAULT 0,
    latest_publish_at TIMESTAMP     NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT        NULL,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT        NULL,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_assessment_paper_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT uk_assessment_paper_code UNIQUE (paper_code)
);

CREATE TABLE IF NOT EXISTS assessment_question
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id            BIGINT        NOT NULL,
    question_type       VARCHAR(32)   NOT NULL,
    sort_order          INT           NOT NULL DEFAULT 1,
    stem_text           VARCHAR(2000) NOT NULL,
    prompt_text         VARCHAR(1000) NULL,
    options_json        LONGTEXT      NULL,
    correct_answer_json LONGTEXT      NOT NULL,
    explanation_text    VARCHAR(1000) NULL,
    score               INT           NOT NULL DEFAULT 10,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT        NULL,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT        NULL,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_assessment_question_paper_id FOREIGN KEY (paper_id) REFERENCES assessment_paper (id)
);

CREATE TABLE IF NOT EXISTS assessment_publish
(
    id                         BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id                   BIGINT        NOT NULL,
    teaching_class_id          BIGINT        NOT NULL,
    published_by               BIGINT        NOT NULL,
    status                     VARCHAR(32)   NOT NULL DEFAULT 'PUBLISHED',
    paper_title_snapshot       VARCHAR(255)  NOT NULL,
    paper_description_snapshot VARCHAR(1000) NULL,
    question_count_snapshot    INT           NOT NULL DEFAULT 0,
    total_score_snapshot       INT           NOT NULL DEFAULT 0,
    duration_minutes           INT           NOT NULL,
    instructions_text          VARCHAR(1000) NULL,
    starts_at                  TIMESTAMP     NULL,
    due_at                     TIMESTAMP     NULL,
    published_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT        NULL,
    updated_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 BIGINT        NULL,
    deleted                    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_assessment_publish_paper_id FOREIGN KEY (paper_id) REFERENCES assessment_paper (id),
    CONSTRAINT fk_assessment_publish_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_assessment_publish_published_by FOREIGN KEY (published_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS assessment_attempt
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    publish_id       BIGINT      NOT NULL,
    paper_id         BIGINT      NOT NULL,
    student_user_id  BIGINT      NOT NULL,
    status           VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       TIMESTAMP   NOT NULL,
    submitted_at     TIMESTAMP   NULL,
    last_saved_at    TIMESTAMP   NULL,
    answered_count   INT         NOT NULL DEFAULT 0,
    objective_score  INT         NOT NULL DEFAULT 0,
    total_score      INT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT      NULL,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       BIGINT      NULL,
    deleted          BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_assessment_attempt_publish_id FOREIGN KEY (publish_id) REFERENCES assessment_publish (id),
    CONSTRAINT fk_assessment_attempt_paper_id FOREIGN KEY (paper_id) REFERENCES assessment_paper (id),
    CONSTRAINT fk_assessment_attempt_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id),
    CONSTRAINT uk_assessment_attempt_publish_student UNIQUE (publish_id, student_user_id, deleted)
);

CREATE TABLE IF NOT EXISTS assessment_attempt_answer
(
    id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_id                BIGINT        NOT NULL,
    question_id               BIGINT        NOT NULL,
    question_order            INT           NOT NULL,
    question_type             VARCHAR(32)   NOT NULL,
    stem_text_snapshot        VARCHAR(2000) NOT NULL,
    prompt_text_snapshot      VARCHAR(1000) NULL,
    options_json_snapshot     LONGTEXT      NULL,
    correct_answer_json       LONGTEXT      NOT NULL,
    explanation_text_snapshot VARCHAR(1000) NULL,
    question_score            INT           NOT NULL DEFAULT 0,
    response_json             LONGTEXT      NULL,
    answered                  BOOLEAN       NOT NULL DEFAULT FALSE,
    correct                   BOOLEAN       NULL,
    score_awarded             INT           NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                BIGINT        NULL,
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                BIGINT        NULL,
    deleted                   BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_assessment_attempt_answer_attempt_id FOREIGN KEY (attempt_id) REFERENCES assessment_attempt (id),
    CONSTRAINT fk_assessment_attempt_answer_question_id FOREIGN KEY (question_id) REFERENCES assessment_question (id),
    CONSTRAINT uk_assessment_attempt_answer_order UNIQUE (attempt_id, question_order, deleted)
);

CREATE INDEX idx_assessment_paper_owner_status ON assessment_paper (owner_user_id, status, updated_at);
CREATE INDEX idx_assessment_question_paper_order ON assessment_question (paper_id, sort_order);
CREATE INDEX idx_assessment_publish_class_status ON assessment_publish (teaching_class_id, status, due_at);
CREATE INDEX idx_assessment_publish_paper_time ON assessment_publish (paper_id, published_at);
CREATE INDEX idx_assessment_attempt_student_status ON assessment_attempt (student_user_id, status, started_at);
CREATE INDEX idx_assessment_attempt_publish_status ON assessment_attempt (publish_id, status, started_at);
CREATE INDEX idx_assessment_attempt_answer_attempt_order ON assessment_attempt_answer (attempt_id, question_order);
