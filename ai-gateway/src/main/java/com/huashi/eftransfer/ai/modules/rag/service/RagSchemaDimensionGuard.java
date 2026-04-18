package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RagSchemaDimensionGuard {

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
    private static final String SCHEMA_METADATA_SQL = """
            SELECT embedding_dimension, hnsw_m, hnsw_ef_construction
            FROM rag_schema_metadata
            WHERE id = 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public RagSchemaDimensionGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void verifyConfig(AiOpsConfigPayload payload) {
        if (payload == null || payload.provider() == null || payload.provider().providers() == null) {
            throw new IllegalStateException("AI runtime configuration is unavailable during pgvector schema validation");
        }

        RagSchemaMetadata metadata = readSchemaMetadata();
        int schemaDimension = readSchemaDimension();
        if (schemaDimension != metadata.embeddingDimension()) {
            throw new IllegalStateException(
                    "chunk_embedding.embedding is %d but rag_schema_metadata expects %d"
                            .formatted(schemaDimension, metadata.embeddingDimension())
            );
        }

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, AiOpsProviderDefinition> entry : payload.provider().providers().entrySet()) {
            AiOpsProviderDefinition definition = entry.getValue();
            Integer dimension = definition == null || definition.embedding() == null ? null : definition.embedding().dimension();
            if (dimension == null || dimension != metadata.embeddingDimension()) {
                mismatches.add(entry.getKey() + "=" + dimension);
            }
        }
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException(
                    "Provider embedding dimensions must match pgvector schema dimension %d: %s"
                            .formatted(metadata.embeddingDimension(), String.join(", ", mismatches))
            );
        }
    }

    private RagSchemaMetadata readSchemaMetadata() {
        return jdbcTemplate.query(SCHEMA_METADATA_SQL, rs -> {
            if (!rs.next()) {
                throw new IllegalStateException("rag_schema_metadata row was not found");
            }
            return new RagSchemaMetadata(
                    rs.getInt("embedding_dimension"),
                    rs.getInt("hnsw_m"),
                    rs.getInt("hnsw_ef_construction")
            );
        });
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

    private record RagSchemaMetadata(
            int embeddingDimension,
            int hnswM,
            int hnswEfConstruction
    ) {
    }
}
