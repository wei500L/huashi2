package com.huashi.eftransfer.app.modules.internal.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairTagRelEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalTagEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairExampleMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairSenseMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairTagRelMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalTagMapper;
import com.huashi.eftransfer.shared.ai.PracticeWordKnowledgeExportItem;
import com.huashi.eftransfer.shared.ai.PracticeWordKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.ai.LexicalPairEmbeddingStatusSyncItem;
import com.huashi.eftransfer.shared.ai.LexicalPairEmbeddingStatusSyncRequest;
import com.huashi.eftransfer.shared.ai.LexicalPairEmbeddingStatusSyncResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExampleItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeSenseItem;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.EmbeddingStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InternalKnowledgeService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;

    private final LexicalPairMapper lexicalPairMapper;
    private final LexicalPairSenseMapper lexicalPairSenseMapper;
    private final LexicalPairExampleMapper lexicalPairExampleMapper;
    private final LexicalPairTagRelMapper lexicalPairTagRelMapper;
    private final LexicalTagMapper lexicalTagMapper;
    private final JdbcTemplate jdbcTemplate;

    public InternalKnowledgeService(
            LexicalPairMapper lexicalPairMapper,
            LexicalPairSenseMapper lexicalPairSenseMapper,
            LexicalPairExampleMapper lexicalPairExampleMapper,
            LexicalPairTagRelMapper lexicalPairTagRelMapper,
            LexicalTagMapper lexicalTagMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.lexicalPairMapper = lexicalPairMapper;
        this.lexicalPairSenseMapper = lexicalPairSenseMapper;
        this.lexicalPairExampleMapper = lexicalPairExampleMapper;
        this.lexicalPairTagRelMapper = lexicalPairTagRelMapper;
        this.lexicalTagMapper = lexicalTagMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public LexicalKnowledgeExportPageResponse exportLexicalPairs(
            OffsetDateTime updatedSince,
            String cursor,
            Integer limit,
            List<Long> ids
    ) {
        CursorState cursorState = parseCursor(cursor);
        int pageSize = normalizeLimit(limit);
        LocalDateTime updatedSinceLocal = updatedSince == null ? null : updatedSince.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        var wrapper = Wrappers.<LexicalPairEntity>lambdaQuery();
        if (ids != null && !ids.isEmpty()) {
            wrapper.in(LexicalPairEntity::getId, new LinkedHashSet<>(ids));
        }

        if (cursorState != null) {
            wrapper.and(query -> query
                    .gt(LexicalPairEntity::getUpdatedAt, cursorState.updatedAt())
                    .or(inner -> inner
                            .eq(LexicalPairEntity::getUpdatedAt, cursorState.updatedAt())
                            .gt(LexicalPairEntity::getId, cursorState.id())));
        } else if (updatedSinceLocal != null) {
            wrapper.ge(LexicalPairEntity::getUpdatedAt, updatedSinceLocal);
        }

        List<LexicalPairEntity> pairs = lexicalPairMapper.selectList(wrapper
                .orderByAsc(LexicalPairEntity::getUpdatedAt)
                .orderByAsc(LexicalPairEntity::getId)
                .last("LIMIT " + pageSize));

        Map<Long, List<String>> tagMap = loadTagMap(pairs.stream().map(LexicalPairEntity::getId).toList());
        Map<Long, List<LexicalPairSenseEntity>> senseMap = loadSenseMap(pairs.stream().map(LexicalPairEntity::getId).toList());
        Map<Long, List<LexicalPairExampleEntity>> exampleMap = loadExampleMap(senseMap.values().stream()
                .flatMap(Collection::stream)
                .map(LexicalPairSenseEntity::getId)
                .toList());

        List<LexicalKnowledgeExportItem> items = pairs.stream()
                .map(pair -> toExportItem(pair, tagMap.getOrDefault(pair.getId(), List.of()), senseMap, exampleMap))
                .toList();

        String nextCursor = pairs.isEmpty() ? null : encodeCursor(pairs.getLast().getUpdatedAt(), pairs.getLast().getId());
        return new LexicalKnowledgeExportPageResponse(items, nextCursor, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Exports the latest version of every practice-bank word (the four FF4
     * sections) for the ai-gateway knowledge base. Cursor pagination matches
     * the lexical export so incremental watermarks work the same way.
     */
    public PracticeWordKnowledgeExportPageResponse exportPracticeWords(
            OffsetDateTime updatedSince,
            String cursor,
            Integer limit
    ) {
        CursorState cursorState = parseCursor(cursor);
        int pageSize = normalizeLimit(limit);
        LocalDateTime updatedSinceLocal = updatedSince == null ? null : updatedSince.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        StringBuilder sql = new StringBuilder("""
                SELECT v.question_code, v.target_word, v.question_type, v.stem_text, v.explanation_text, v.updated_at, v.id
                FROM assessment_question_version v
                JOIN assessment_question_bank b ON b.id = v.question_bank_id
                WHERE b.bank_code = 'LEXIBRIDGE_FF4_V2'
                  AND v.deleted = FALSE
                  AND v.construct_code IN ('FF4_WORD_MEANING','FF4_SENTENCE_SYNONYM','FF4_TRUE_FALSE_TRANSFER','FF4_SPELLING')
                  AND v.version_no = (
                      SELECT MAX(latest.version_no)
                      FROM assessment_question_version latest
                      WHERE latest.question_bank_id = v.question_bank_id
                        AND latest.question_code = v.question_code
                        AND latest.deleted = FALSE
                  )
                """);
        List<Object> args = new java.util.ArrayList<>();
        if (cursorState != null) {
            sql.append("AND (v.updated_at > ? OR (v.updated_at = ? AND v.id > ?)) ");
            args.add(java.sql.Timestamp.valueOf(cursorState.updatedAt()));
            args.add(java.sql.Timestamp.valueOf(cursorState.updatedAt()));
            args.add(cursorState.id());
        } else if (updatedSinceLocal != null) {
            sql.append("AND v.updated_at >= ? ");
            args.add(java.sql.Timestamp.valueOf(updatedSinceLocal));
        }
        sql.append("ORDER BY v.updated_at ASC, v.id ASC LIMIT ").append(pageSize);

        List<PracticeWordRow> rows = jdbcTemplate.query(sql.toString(),
                (resultSet, rowNumber) -> new PracticeWordRow(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getTimestamp(6).toLocalDateTime(),
                        resultSet.getLong(7)
                ),
                args.toArray());

        List<PracticeWordKnowledgeExportItem> items = rows.stream()
                .map(row -> new PracticeWordKnowledgeExportItem(
                        row.questionCode(),
                        row.targetWord(),
                        row.questionType(),
                        row.chineseMeaning(),
                        row.explanation(),
                        row.updatedAt().atZone(ZoneOffset.UTC).toOffsetDateTime()
                ))
                .toList();

        String nextCursor = rows.isEmpty() ? null : encodeCursor(rows.getLast().updatedAt(), rows.getLast().id());
        return new PracticeWordKnowledgeExportPageResponse(items, nextCursor, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public LexicalPairEmbeddingStatusSyncResponse syncLexicalPairEmbeddingStatuses(
            LexicalPairEmbeddingStatusSyncRequest request
    ) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            return new LexicalPairEmbeddingStatusSyncResponse(0);
        }

        Map<Long, LexicalPairEmbeddingStatusSyncItem> itemsById = new LinkedHashMap<>();
        for (LexicalPairEmbeddingStatusSyncItem item : request.items()) {
            if (item == null || item.lexicalPairId() == null || item.lexicalPairId() <= 0) {
                continue;
            }
            itemsById.put(item.lexicalPairId(), item);
        }

        int updatedCount = 0;
        for (LexicalPairEmbeddingStatusSyncItem item : itemsById.values()) {
            EmbeddingStatus status = parseEmbeddingStatus(item.embeddingStatus());
            updatedCount += lexicalPairMapper.updateEmbeddingState(
                    item.lexicalPairId(),
                    status.name(),
                    resolveLastEmbeddedAt(status, item.lastEmbeddedAt())
            );
        }

        return new LexicalPairEmbeddingStatusSyncResponse(updatedCount);
    }

    private LexicalKnowledgeExportItem toExportItem(
            LexicalPairEntity pair,
            List<String> tags,
            Map<Long, List<LexicalPairSenseEntity>> senseMap,
            Map<Long, List<LexicalPairExampleEntity>> exampleMap
    ) {
        List<LexicalKnowledgeSenseItem> senses = senseMap.getOrDefault(pair.getId(), List.of()).stream()
                .map(sense -> new LexicalKnowledgeSenseItem(
                        sense.getId(),
                        sense.getSortOrder(),
                        sense.getEnglishDefinition(),
                        sense.getFrenchDefinition(),
                        sense.getChineseDefinition(),
                        exampleMap.getOrDefault(sense.getId(), List.of()).stream()
                                .map(example -> new LexicalKnowledgeExampleItem(
                                        example.getId(),
                                        example.getSortOrder(),
                                        example.getEnglishExample(),
                                        example.getFrenchExample(),
                                        example.getChineseTranslation(),
                                        example.getContextSupportLevel(),
                                        example.getSource()
                                ))
                                .toList()
                ))
                .toList();

        return new LexicalKnowledgeExportItem(
                pair.getId(),
                pair.getUpdatedAt() == null ? null : pair.getUpdatedAt().atOffset(ZoneOffset.UTC),
                pair.getActive(),
                pair.getKnowledgeStatus(),
                pair.getEmbeddingStatus(),
                pair.getEnglishWord(),
                pair.getFrenchWord(),
                pair.getChineseGloss(),
                pair.getLexicalPairType(),
                decimal(pair.getSemanticOverlapScore()),
                decimal(pair.getFalseFriendRisk()),
                pair.getDefaultContextSupport(),
                pair.getDifficultyLevel(),
                pair.getNotes(),
                pair.getSource(),
                tags,
                senses
        );
    }

    private Map<Long, List<LexicalPairSenseEntity>> loadSenseMap(List<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                        .in(LexicalPairSenseEntity::getLexicalPairId, lexicalPairIds)
                        .orderByAsc(LexicalPairSenseEntity::getSortOrder)
                        .orderByAsc(LexicalPairSenseEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(LexicalPairSenseEntity::getLexicalPairId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, List<LexicalPairExampleEntity>> loadExampleMap(List<Long> senseIds) {
        if (senseIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairExampleMapper.selectList(Wrappers.<LexicalPairExampleEntity>lambdaQuery()
                        .in(LexicalPairExampleEntity::getLexicalPairSenseId, senseIds)
                        .orderByAsc(LexicalPairExampleEntity::getSortOrder)
                        .orderByAsc(LexicalPairExampleEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(LexicalPairExampleEntity::getLexicalPairSenseId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, List<String>> loadTagMap(List<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        List<LexicalPairTagRelEntity> relations = lexicalPairTagRelMapper.selectList(Wrappers.<LexicalPairTagRelEntity>lambdaQuery()
                .in(LexicalPairTagRelEntity::getLexicalPairId, lexicalPairIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> tagNameMap = lexicalTagMapper.selectBatchIds(relations.stream()
                        .map(LexicalPairTagRelEntity::getLexicalTagId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .filter(tag -> Boolean.TRUE.equals(tag.getActive()))
                .collect(Collectors.toMap(LexicalTagEntity::getId, LexicalTagEntity::getTagName));

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (LexicalPairTagRelEntity relation : relations) {
            String tagName = tagNameMap.get(relation.getLexicalTagId());
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            result.computeIfAbsent(relation.getLexicalPairId(), key -> new java.util.ArrayList<>()).add(tagName);
        }
        return result;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private CursorState parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            return new CursorState(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Invalid export cursor", 400);
        }
    }

    private String encodeCursor(LocalDateTime updatedAt, Long id) {
        String value = updatedAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Double decimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private EmbeddingStatus parseEmbeddingStatus(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "embeddingStatus must not be blank", 400);
        }
        try {
            return EmbeddingStatus.fromCode(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, exception.getMessage(), 400);
        }
    }

    private LocalDateTime resolveLastEmbeddedAt(EmbeddingStatus status, OffsetDateTime lastEmbeddedAt) {
        if (!Objects.equals(status, EmbeddingStatus.EMBEDDED)) {
            return null;
        }
        OffsetDateTime effective = lastEmbeddedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : lastEmbeddedAt;
        return effective.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private record CursorState(LocalDateTime updatedAt, Long id) {
    }

    private record PracticeWordRow(
            String questionCode,
            String targetWord,
            String questionType,
            String chineseMeaning,
            String explanation,
            LocalDateTime updatedAt,
            Long id
    ) {
    }
}
