package com.huashi.eftransfer.ai.modules.rag.repository;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeChunkPayload;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeDocumentPayload;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class KnowledgeStoreRepositoryTransactionalTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ef_transfer_ai_test")
            .withUsername("ef_ai")
            .withPassword("ef_ai_password");

    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbcTemplate;
    private static KnowledgeStoreRepository knowledgeStoreRepository;
    private static DataSource testDataSource;

    @BeforeAll
    static void beforeAll() {
        POSTGRES.start();
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("org.postgresql.Driver");
        driverManagerDataSource.setUrl(POSTGRES.getJdbcUrl());
        driverManagerDataSource.setUsername(POSTGRES.getUsername());
        driverManagerDataSource.setPassword(POSTGRES.getPassword());
        testDataSource = driverManagerDataSource;

        Flyway.configure()
                .dataSource(testDataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        jdbcTemplate = context.getBean(JdbcTemplate.class);
        knowledgeStoreRepository = context.getBean(KnowledgeStoreRepository.class);
    }

    @AfterAll
    static void afterAll() {
        context.close();
        POSTGRES.stop();
    }

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE chunk_embedding, knowledge_chunk, knowledge_document, ingestion_job RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldRollbackDocumentAndChunksWhenChunkUpsertFails() {
        KnowledgeDocumentPayload payload = new KnowledgeDocumentPayload(
                "LEXICAL_PAIR",
                "1001",
                "coin / coin",
                OffsetDateTime.now(ZoneOffset.UTC),
                true,
                Map.of("lexicalPairId", "1001"),
                List.of(
                        new KnowledgeChunkPayload(
                                "pair:1001",
                                0,
                                "LEXICAL_PAIR",
                                "1001",
                                "coin / coin",
                                "False friend pair guidance",
                                Map.of("chunkKind", "LEXICAL_PAIR"),
                                true
                        ),
                        new KnowledgeChunkPayload(
                                "sense:2001",
                                1,
                                "LEXICAL_SENSE",
                                "2001",
                                "coin / coin - Sense 1",
                                "Money sense definition",
                                Map.of("bad", new BrokenJsonValue()),
                                true
                        )
                )
        );

        assertThatThrownBy(() -> knowledgeStoreRepository.upsertDocument(payload, false, "doc-hash-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize JSON payload");

        assertThat(count("SELECT COUNT(*) FROM knowledge_document")).isZero();
        assertThat(count("SELECT COUNT(*) FROM knowledge_chunk")).isZero();
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    static class BrokenJsonValue {
        @JsonValue
        public String jsonValue() {
            throw new IllegalStateException("serialization failed");
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return testDataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        KnowledgeStoreRepository knowledgeStoreRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            return new KnowledgeStoreRepository(jdbcTemplate, objectMapper);
        }
    }
}
