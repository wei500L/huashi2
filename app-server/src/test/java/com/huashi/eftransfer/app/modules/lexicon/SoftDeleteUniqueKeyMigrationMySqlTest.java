package com.huashi.eftransfer.app.modules.lexicon;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class SoftDeleteUniqueKeyMigrationMySqlTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.8")
            .withDatabaseName("ef_transfer_app_test")
            .withUsername("ef_user")
            .withPassword("ef_password");

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void beforeAll() {
        MYSQL.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(MYSQL.getDriverClassName());
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUsername(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());

        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void afterAll() {
        MYSQL.stop();
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM lexical_tag");
    }

    @Test
    void shouldAllowSoftDeletedDuplicatesWhileRejectingActiveDuplicates() {
        jdbcTemplate.update("""
                INSERT INTO lexical_tag (id, tag_name, description, active, created_at, updated_at, deleted)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, 1L, "noun", "active tag", true, false);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO lexical_tag (id, tag_name, description, active, created_at, updated_at, deleted)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                        """, 2L, "noun", "duplicate active tag", true, false))
                .isInstanceOf(Exception.class);

        jdbcTemplate.update("""
                INSERT INTO lexical_tag (id, tag_name, description, active, created_at, updated_at, deleted)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, 3L, "noun", "soft deleted tag", false, true);
        jdbcTemplate.update("""
                INSERT INTO lexical_tag (id, tag_name, description, active, created_at, updated_at, deleted)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """, 4L, "noun", "another soft deleted tag", false, true);

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lexical_tag WHERE tag_name = ?",
                Integer.class,
                "noun"
        );
        Integer activeTagIndexColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'lexical_tag'
                  AND index_name = 'uk_lexical_tag_name'
                  AND column_name = 'active_tag_name'
                """, Integer.class);
        Integer activeTagColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'lexical_tag'
                  AND column_name = 'active_tag_name'
                """, Integer.class);

        assertThat(rowCount).isEqualTo(3);
        assertThat(activeTagColumnCount).isEqualTo(1);
        assertThat(activeTagIndexColumnCount).isEqualTo(1);
    }
}
