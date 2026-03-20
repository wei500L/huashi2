package com.huashi.eftransfer.app.modules.internal.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.config.InternalKnowledgeProperties;
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
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExampleItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportItem;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeSenseItem;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
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
    private final InternalKnowledgeProperties properties;

    public InternalKnowledgeService(
            LexicalPairMapper lexicalPairMapper,
            LexicalPairSenseMapper lexicalPairSenseMapper,
            LexicalPairExampleMapper lexicalPairExampleMapper,
            LexicalPairTagRelMapper lexicalPairTagRelMapper,
            LexicalTagMapper lexicalTagMapper,
            InternalKnowledgeProperties properties
    ) {
        this.lexicalPairMapper = lexicalPairMapper;
        this.lexicalPairSenseMapper = lexicalPairSenseMapper;
        this.lexicalPairExampleMapper = lexicalPairExampleMapper;
        this.lexicalPairTagRelMapper = lexicalPairTagRelMapper;
        this.lexicalTagMapper = lexicalTagMapper;
        this.properties = properties;
    }

    public void validateToken(String token) {
        String configuredToken = properties.getToken();
        if (!StringUtils.hasText(configuredToken)) {
            return;
        }
        if (!Objects.equals(configuredToken, token)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Invalid internal knowledge token", 403);
        }
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

    private record CursorState(LocalDateTime updatedAt, Long id) {
    }
}
