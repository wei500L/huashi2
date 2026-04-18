package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RagSchemaDimensionGuard {

    private static final int FIXED_VECTOR_DIMENSION = 1024;
    private static final Pattern VECTOR_TYPE_PATTERN = Pattern.compile("vector\\((\\d+)\\)");
    private static final String CHUNK_EMBEDDING_TYPE_SQL = """
            SELECT format_type(attribute.atttypid, attribute.atttypmod)
            FROM pg_attribute attribute
            JOIN pg_class relation ON relation.oid = attribute.attrelid
            JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE namespace.nspname = current_schema()
              AND relation.relname = 'chunk_embedding'
              AND attribute.attname = 'embedding'
              AND attribute.attnum > 0
              AND NOT attribute.attisdropped
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AiRuntimeConfigService runtimeConfigService;
    private final int vectorStoreDimension;

    public RagSchemaDimensionGuard(
            JdbcTemplate jdbcTemplate,
            AiRuntimeConfigService runtimeConfigService,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}") int vectorStoreDimension
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeConfigService = runtimeConfigService;
        this.vectorStoreDimension = vectorStoreDimension;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void verifyOnStartup() {
        verifyDimensions();
    }

    void verifyDimensions() {
        int schemaDimension = readSchemaDimension();
        verifyFixedDimension("chunk_embedding.embedding", schemaDimension);
        verifyFixedDimension("spring.ai.vectorstore.pgvector.dimensions", vectorStoreDimension);

        AiRuntimeBundle bundle = runtimeConfigService.current();
        if (bundle == null || bundle.config() == null || bundle.config().provider() == null) {
            throw new IllegalStateException("AI runtime configuration is unavailable during pgvector schema validation");
        }

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, AiOpsProviderDefinition> entry : bundle.config().provider().providers().entrySet()) {
            AiOpsProviderDefinition definition = entry.getValue();
            Integer dimension = definition == null || definition.embedding() == null ? null : definition.embedding().dimension();
            if (dimension == null || dimension != FIXED_VECTOR_DIMENSION) {
                mismatches.add(entry.getKey() + "=" + dimension);
            }
        }
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException(
                    "Provider embedding dimensions must match pgvector schema dimension %d: %s"
                            .formatted(FIXED_VECTOR_DIMENSION, String.join(", ", mismatches))
            );
        }
    }

    private void verifyFixedDimension(String source, int actualDimension) {
        if (actualDimension != FIXED_VECTOR_DIMENSION) {
            throw new IllegalStateException(
                    "%s is %d but the pgvector schema is fixed at %d dimensions"
                            .formatted(source, actualDimension, FIXED_VECTOR_DIMENSION)
            );
        }
    }

    private int readSchemaDimension() {
        String columnType = jdbcTemplate.queryForObject(CHUNK_EMBEDDING_TYPE_SQL, String.class);
        if (columnType == null || columnType.isBlank()) {
            throw new IllegalStateException("chunk_embedding.embedding vector column was not found");
        }
        Matcher matcher = VECTOR_TYPE_PATTERN.matcher(columnType);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unexpected chunk_embedding.embedding type: " + columnType);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
