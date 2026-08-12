package com.huashi.eftransfer.ai.modules.rag.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class IngestionJobRepositoryTest {

    private static final String JOB_TYPE = "KNOWLEDGE_REINDEX";
    private static final OffsetDateTime EARLIER = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LATER = OffsetDateTime.of(2026, 3, 2, 0, 0, 0, 0, ZoneOffset.UTC);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ef_transfer_ai_test")
            .withUsername("ef_ai")
            .withPassword("ef_ai_password");

    private static JdbcTemplate jdbcTemplate;
    private static IngestionJobRepository ingestionJobRepository;

    @BeforeAll
    static void beforeAll() {
        POSTGRES.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
        ingestionJobRepository = new IngestionJobRepository(jdbcTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("TRUNCATE ingestion_job RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("TRUNCATE ingestion_job RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldIgnoreSubsetJobsWhenFindingLatestSuccessfulWatermark() {
        Long comprehensiveJob = ingestionJobRepository.createPendingJob(
                JOB_TYPE, "FULL", Set.of(KnowledgeSourceTypes.LEXICAL_PAIR), Set.of());
        ingestionJobRepository.markSucceeded(comprehensiveJob, EARLIER, Map.of());

        Long subsetJob = ingestionJobRepository.createPendingJob(
                JOB_TYPE, "FULL", Set.of(KnowledgeSourceTypes.LEXICAL_PAIR), Set.of("1001"));
        ingestionJobRepository.markSucceeded(subsetJob, LATER, Map.of());

        assertThat(ingestionJobRepository.findLatestSuccessfulWatermark(JOB_TYPE, KnowledgeSourceTypes.LEXICAL_PAIR))
                .isEqualTo(EARLIER);
    }

    @Test
    void shouldReturnNullWhenOnlySubsetJobsSucceeded() {
        Long subsetJob = ingestionJobRepository.createPendingJob(
                JOB_TYPE, "FULL", Set.of(KnowledgeSourceTypes.LEXICAL_PAIR), Set.of("1001"));
        ingestionJobRepository.markSucceeded(subsetJob, LATER, Map.of());

        assertThat(ingestionJobRepository.findLatestSuccessfulWatermark(JOB_TYPE, KnowledgeSourceTypes.LEXICAL_PAIR))
                .isNull();
    }
}
