CREATE TABLE IF NOT EXISTS users
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP    NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT       NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT       NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    role_code  VARCHAR(32) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT      NULL,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT      NULL,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_user_role_user_id_role_code UNIQUE (user_id, role_code)
);

CREATE TABLE IF NOT EXISTS student_profile
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    student_no      VARCHAR(64)  NOT NULL,
    grade_name      VARCHAR(64)  NOT NULL,
    english_level   VARCHAR(16)  NOT NULL,
    french_level    VARCHAR(16)  NOT NULL,
    composite_score INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_student_profile_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_student_profile_user_id UNIQUE (user_id),
    CONSTRAINT uk_student_profile_student_no UNIQUE (student_no)
);

CREATE TABLE IF NOT EXISTS teacher_profile
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    employee_no VARCHAR(64)  NOT NULL,
    department  VARCHAR(128) NOT NULL,
    title       VARCHAR(128) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT       NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT       NULL,
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_teacher_profile_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_teacher_profile_user_id UNIQUE (user_id),
    CONSTRAINT uk_teacher_profile_employee_no UNIQUE (employee_no)
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_user_role_user_id ON user_role (user_id);
