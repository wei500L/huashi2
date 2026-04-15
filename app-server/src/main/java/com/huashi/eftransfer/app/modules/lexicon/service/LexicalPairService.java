package com.huashi.eftransfer.app.modules.lexicon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairExampleRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairSenseRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairUpsertRequest;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalListItemEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairTagRelEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalTagEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportTemplateSupport;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalListItemMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairExampleMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairSenseMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairTagRelMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalTagMapper;
import com.huashi.eftransfer.app.modules.lexicon.event.LexicalKnowledgeChangedEventPublisher;
import com.huashi.eftransfer.app.modules.lexicon.support.LexicalRiskSupport;
import com.huashi.eftransfer.app.modules.lexicon.support.PinyinSearchIndexSupport;
import com.huashi.eftransfer.app.modules.lexicon.support.SearchableTextBuilder;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportFailureVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportResultVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportTemplateFieldVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportTemplateVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairDetailVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairExampleVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairOverviewVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairSenseVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairSuggestionVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairSummaryVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.EmbeddingStatus;
import com.huashi.eftransfer.shared.enums.KnowledgeStatus;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LexicalPairService {

    private record SuggestionMatch(String matchedBy, int priority) {
    }

    public record CsvExportFile(
            String filename,
            byte[] content
    ) {
    }

    private static final Logger log = LoggerFactory.getLogger(LexicalPairService.class);

    private static final List<CsvImportTemplateFieldVO> TEMPLATE_FIELDS = List.of(
            new CsvImportTemplateFieldVO("english_word", true, "English lexical item", "coin"),
            new CsvImportTemplateFieldVO("french_word", true, "French lexical item", "coin"),
            new CsvImportTemplateFieldVO("chinese_gloss", true, "Chinese gloss", "硬币；角落"),
            new CsvImportTemplateFieldVO("lexical_pair_type", true, "cognate / false_friend / partial_cognate / orthographic_similar", "false_friend"),
            new CsvImportTemplateFieldVO("semantic_overlap_score", true, "0.0 - 1.0", "0.10"),
            new CsvImportTemplateFieldVO("false_friend_risk", true, "0.0 - 1.0", "0.92"),
            new CsvImportTemplateFieldVO("default_context_support", true, "low / medium / high", "high"),
            new CsvImportTemplateFieldVO("difficulty_level", true, "1 - 5", "4"),
            new CsvImportTemplateFieldVO("notes", false, "Teacher notes", "High confusion for beginners"),
            new CsvImportTemplateFieldVO("source", false, "Source or textbook", "Teacher Curated"),
            new CsvImportTemplateFieldVO("active", false, "true / false", "true"),
            new CsvImportTemplateFieldVO("tags", false, "Pipe separated tags", "false-friend|high-frequency"),
            new CsvImportTemplateFieldVO("knowledge_status", false, "draft / ready / disabled", "ready"),
            new CsvImportTemplateFieldVO("embedding_status", false, "pending / embedded / failed", "pending"),
            new CsvImportTemplateFieldVO("sense_english_definition", false, "Primary English definition", "a piece of money"),
            new CsvImportTemplateFieldVO("sense_french_definition", false, "Primary French definition", "pièce de monnaie"),
            new CsvImportTemplateFieldVO("sense_chinese_definition", false, "Primary Chinese definition", "硬币"),
            new CsvImportTemplateFieldVO("example_english", false, "Primary English example", "I found a coin on the floor."),
            new CsvImportTemplateFieldVO("example_french", false, "Primary French example", "J'ai trouvé une pièce dans la rue."),
            new CsvImportTemplateFieldVO("example_chinese", false, "Primary Chinese translation", "我在地上捡到一枚硬币。"),
            new CsvImportTemplateFieldVO("example_context_support", false, "low / medium / high", "high")
    );

    private static final Set<String> REQUIRED_HEADERS = TEMPLATE_FIELDS.stream()
            .filter(CsvImportTemplateFieldVO::required)
            .map(CsvImportTemplateFieldVO::fieldName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final LexicalPairMapper lexicalPairMapper;
    private final LexicalPairSenseMapper lexicalPairSenseMapper;
    private final LexicalPairExampleMapper lexicalPairExampleMapper;
    private final LexicalTagMapper lexicalTagMapper;
    private final LexicalPairTagRelMapper lexicalPairTagRelMapper;
    private final LexicalListItemMapper lexicalListItemMapper;
    private final TransactionTemplate transactionTemplate;
    private final LexicalKnowledgeChangedEventPublisher lexicalKnowledgeChangedEventPublisher;
    private final LexicalImportTemplateSupport lexicalImportTemplateSupport;

    public LexicalPairService(
            LexicalPairMapper lexicalPairMapper,
            LexicalPairSenseMapper lexicalPairSenseMapper,
            LexicalPairExampleMapper lexicalPairExampleMapper,
            LexicalTagMapper lexicalTagMapper,
            LexicalPairTagRelMapper lexicalPairTagRelMapper,
            LexicalListItemMapper lexicalListItemMapper,
            PlatformTransactionManager transactionManager,
            LexicalKnowledgeChangedEventPublisher lexicalKnowledgeChangedEventPublisher,
            LexicalImportTemplateSupport lexicalImportTemplateSupport
    ) {
        this.lexicalPairMapper = lexicalPairMapper;
        this.lexicalPairSenseMapper = lexicalPairSenseMapper;
        this.lexicalPairExampleMapper = lexicalPairExampleMapper;
        this.lexicalTagMapper = lexicalTagMapper;
        this.lexicalPairTagRelMapper = lexicalPairTagRelMapper;
        this.lexicalListItemMapper = lexicalListItemMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.lexicalKnowledgeChangedEventPublisher = lexicalKnowledgeChangedEventPublisher;
        this.lexicalImportTemplateSupport = lexicalImportTemplateSupport;
    }

    public PageResult<LexicalPairSummaryVO> pageQuery(LexicalPairPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        LambdaQueryWrapper<LexicalPairEntity> countWrapper = buildPageWrapper(query);
        long total = lexicalPairMapper.selectCount(countWrapper);

        LambdaQueryWrapper<LexicalPairEntity> pageWrapper = buildPageWrapper(query)
                .orderByDesc(LexicalPairEntity::getUpdatedAt)
                .orderByDesc(LexicalPairEntity::getId)
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset());

        List<LexicalPairEntity> pairs = lexicalPairMapper.selectList(pageWrapper);
        Map<Long, List<String>> tagMap = loadTagMap(pairs.stream().map(LexicalPairEntity::getId).toList());

        List<LexicalPairSummaryVO> records = pairs.stream()
                .map(pair -> toSummaryVO(pair, tagMap.getOrDefault(pair.getId(), List.of())))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    public LexicalPairOverviewVO getOverview() {
        long totalCount = lexicalPairMapper.selectCount(Wrappers.lambdaQuery());
        long activeCount = lexicalPairMapper.selectCount(Wrappers.<LexicalPairEntity>lambdaQuery()
                .eq(LexicalPairEntity::getActive, true));
        long pendingEmbeddingCount = lexicalPairMapper.selectCount(Wrappers.<LexicalPairEntity>lambdaQuery()
                .eq(LexicalPairEntity::getEmbeddingStatus, EmbeddingStatus.PENDING.name()));
        long embeddedCount = lexicalPairMapper.selectCount(Wrappers.<LexicalPairEntity>lambdaQuery()
                .eq(LexicalPairEntity::getEmbeddingStatus, EmbeddingStatus.EMBEDDED.name()));
        long failedEmbeddingCount = lexicalPairMapper.selectCount(Wrappers.<LexicalPairEntity>lambdaQuery()
                .eq(LexicalPairEntity::getEmbeddingStatus, EmbeddingStatus.FAILED.name()));

        LocalDateTime latestCreatedAt = lexicalPairMapper.selectList(Wrappers.<LexicalPairEntity>lambdaQuery()
                        .select(LexicalPairEntity::getCreatedAt)
                        .orderByDesc(LexicalPairEntity::getCreatedAt)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(LexicalPairEntity::getCreatedAt)
                .orElse(null);

        LocalDateTime latestUpdatedAt = lexicalPairMapper.selectList(Wrappers.<LexicalPairEntity>lambdaQuery()
                        .select(LexicalPairEntity::getUpdatedAt)
                        .orderByDesc(LexicalPairEntity::getUpdatedAt)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(LexicalPairEntity::getUpdatedAt)
                .orElse(null);

        LocalDateTime latestEmbeddedAt = lexicalPairMapper.selectList(Wrappers.<LexicalPairEntity>lambdaQuery()
                        .select(LexicalPairEntity::getLastEmbeddedAt)
                        .isNotNull(LexicalPairEntity::getLastEmbeddedAt)
                        .orderByDesc(LexicalPairEntity::getLastEmbeddedAt)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(LexicalPairEntity::getLastEmbeddedAt)
                .orElse(null);

        return new LexicalPairOverviewVO(
                totalCount,
                activeCount,
                pendingEmbeddingCount,
                embeddedCount,
                failedEmbeddingCount,
                latestCreatedAt,
                latestUpdatedAt,
                latestEmbeddedAt
        );
    }

    public LexicalPairDetailVO getDetail(Long lexicalPairId) {
        LexicalPairEntity pair = requirePair(lexicalPairId);
        List<LexicalPairSenseEntity> senses = lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                .eq(LexicalPairSenseEntity::getLexicalPairId, lexicalPairId)
                .orderByAsc(LexicalPairSenseEntity::getSortOrder)
                .orderByAsc(LexicalPairSenseEntity::getId));
        Map<Long, List<LexicalPairExampleEntity>> exampleMap = loadExampleMap(senses.stream().map(LexicalPairSenseEntity::getId).toList());
        List<String> tags = loadTagMap(List.of(lexicalPairId)).getOrDefault(lexicalPairId, List.of());
        List<LexicalPairSenseVO> senseVOs = senses.stream()
                .map(sense -> new LexicalPairSenseVO(
                        sense.getId(),
                        sense.getSortOrder(),
                        sense.getEnglishDefinition(),
                        sense.getFrenchDefinition(),
                        sense.getChineseDefinition(),
                        exampleMap.getOrDefault(sense.getId(), List.of()).stream()
                                .map(this::toExampleVO)
                                .toList()
                ))
                .toList();
        return new LexicalPairDetailVO(
                pair.getId(),
                pair.getEnglishWord(),
                pair.getFrenchWord(),
                pair.getChineseGloss(),
                pair.getLexicalPairType(),
                pair.getSemanticOverlapScore(),
                pair.getFalseFriendRisk(),
                LexicalRiskSupport.resolve(pair.getFalseFriendRisk()).name(),
                pair.getDefaultContextSupport(),
                pair.getDifficultyLevel(),
                pair.getNotes(),
                pair.getSource(),
                pair.getActive(),
                pair.getSearchableText(),
                pair.getKnowledgeStatus(),
                pair.getEmbeddingStatus(),
                pair.getLastEmbeddedAt(),
                tags,
                senseVOs
        );
    }

    public List<LexicalPairSuggestionVO> suggest(String keyword, Integer limit, Boolean active) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        int resolvedLimit = limit == null ? 8 : Math.max(1, Math.min(limit, 20));
        LambdaQueryWrapper<LexicalPairEntity> wrapper = Wrappers.<LexicalPairEntity>lambdaQuery()
                .orderByDesc(LexicalPairEntity::getUpdatedAt)
                .orderByDesc(LexicalPairEntity::getId)
                .last("LIMIT 30");
        if (active != null) {
            wrapper.eq(LexicalPairEntity::getActive, active);
        }
        applyKeywordFilter(wrapper, normalizedKeyword);

        return lexicalPairMapper.selectList(wrapper).stream()
                .map(pair -> Map.entry(pair, resolveSuggestionMatch(pair, normalizedKeyword)))
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator
                        .comparingInt((Map.Entry<LexicalPairEntity, SuggestionMatch> entry) -> entry.getValue().priority())
                        .thenComparing((Map.Entry<LexicalPairEntity, SuggestionMatch> entry) -> safeLower(entry.getKey().getEnglishWord()))
                        .thenComparing((Map.Entry<LexicalPairEntity, SuggestionMatch> entry) -> safeLower(entry.getKey().getFrenchWord())))
                .limit(resolvedLimit)
                .map(entry -> new LexicalPairSuggestionVO(
                        entry.getKey().getId(),
                        entry.getKey().getEnglishWord(),
                        entry.getKey().getFrenchWord(),
                        entry.getKey().getChineseGloss(),
                        entry.getKey().getLexicalPairType(),
                        entry.getValue().matchedBy()
                ))
                .toList();
    }

    @Transactional
    public Long create(LexicalPairUpsertRequest request) {
        Long lexicalPairId = createInternal(request);
        registerKnowledgeChangedEvent(List.of(lexicalPairId));
        log.info("event=lexical_pair_created pairId={} englishWord={} frenchWord={}",
                lexicalPairId, request.englishWord(), request.frenchWord());
        return lexicalPairId;
    }

    @Transactional
    public Long update(Long lexicalPairId, LexicalPairUpsertRequest request) {
        LexicalPairEntity existing = requirePair(lexicalPairId);
        validatePairUniqueness(request.englishWord(), request.frenchWord(), lexicalPairId);
        validateSenses(request.senses());

        List<String> normalizedTags = normalizeTags(request.tags());
        existing.setEnglishWord(request.englishWord().trim());
        existing.setFrenchWord(request.frenchWord().trim());
        existing.setChineseGloss(request.chineseGloss().trim());
        existing.setLexicalPairType(parseLexicalPairType(request.lexicalPairType()).name());
        existing.setSemanticOverlapScore(request.semanticOverlapScore());
        existing.setFalseFriendRisk(request.falseFriendRisk());
        existing.setDefaultContextSupport(parseContextSupportLevel(request.defaultContextSupport()).name());
        existing.setDifficultyLevel(request.difficultyLevel());
        existing.setNotes(trimToNull(request.notes()));
        existing.setSource(trimToNull(request.source()));
        existing.setActive(request.active() == null || request.active());
        existing.setKnowledgeStatus(parseKnowledgeStatus(request.knowledgeStatus()).name());
        EmbeddingStatus embeddingStatus = parseEmbeddingStatus(request.embeddingStatus());
        existing.setEmbeddingStatus(embeddingStatus.name());
        existing.setLastEmbeddedAt(embeddingStatus == EmbeddingStatus.EMBEDDED ? LocalDateTime.now() : null);
        existing.setSearchableText(buildPairSearchableText(request, normalizedTags));
        applySearchIndex(existing, request, normalizedTags);
        lexicalPairMapper.updateById(existing);

        replaceSenses(existing.getId(), request.senses());
        replaceTags(existing.getId(), normalizedTags);
        registerKnowledgeChangedEvent(List.of(existing.getId()));
        log.info("event=lexical_pair_updated pairId={} englishWord={} frenchWord={}",
                existing.getId(), existing.getEnglishWord(), existing.getFrenchWord());
        return existing.getId();
    }

    @Transactional
    public void delete(Long lexicalPairId) {
        requirePair(lexicalPairId);
        List<Long> senseIds = lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                        .eq(LexicalPairSenseEntity::getLexicalPairId, lexicalPairId))
                .stream()
                .map(LexicalPairSenseEntity::getId)
                .toList();
        if (!senseIds.isEmpty()) {
            lexicalPairExampleMapper.delete(Wrappers.<LexicalPairExampleEntity>lambdaQuery()
                    .in(LexicalPairExampleEntity::getLexicalPairSenseId, senseIds));
        }
        lexicalPairSenseMapper.delete(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                .eq(LexicalPairSenseEntity::getLexicalPairId, lexicalPairId));
        lexicalPairTagRelMapper.delete(Wrappers.<LexicalPairTagRelEntity>lambdaQuery()
                .eq(LexicalPairTagRelEntity::getLexicalPairId, lexicalPairId));
        lexicalListItemMapper.delete(Wrappers.<LexicalListItemEntity>lambdaQuery()
                .eq(LexicalListItemEntity::getLexicalPairId, lexicalPairId));
        lexicalPairMapper.deleteById(lexicalPairId);
        registerKnowledgeChangedEvent(List.of(lexicalPairId));
        log.info("event=lexical_pair_deleted pairId={}", lexicalPairId);
    }

    public CsvImportTemplateVO getImportTemplate() {
        return lexicalImportTemplateSupport.template();
    }

    public CsvExportFile exportCsv(LexicalPairPageQuery query) {
        List<LexicalPairEntity> pairs = lexicalPairMapper.selectList(buildPageWrapper(query)
                .orderByDesc(LexicalPairEntity::getUpdatedAt)
                .orderByDesc(LexicalPairEntity::getId));
        Map<Long, List<String>> tagMap = loadTagMap(pairs.stream().map(LexicalPairEntity::getId).toList());

        List<LexicalPairSenseEntity> senses = pairs.isEmpty()
                ? List.of()
                : lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                        .in(LexicalPairSenseEntity::getLexicalPairId, pairs.stream().map(LexicalPairEntity::getId).toList())
                        .orderByAsc(LexicalPairSenseEntity::getSortOrder)
                        .orderByAsc(LexicalPairSenseEntity::getId));
        Map<Long, List<LexicalPairSenseEntity>> senseMap = senses.stream()
                .collect(Collectors.groupingBy(LexicalPairSenseEntity::getLexicalPairId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<LexicalPairExampleEntity>> exampleMap = loadExampleMap(senses.stream().map(LexicalPairSenseEntity::getId).toList());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(TEMPLATE_FIELDS.stream().map(CsvImportTemplateFieldVO::fieldName).toArray(String[]::new))
                     .build())) {
            for (LexicalPairEntity pair : pairs) {
                LexicalPairSenseEntity primarySense = senseMap.getOrDefault(pair.getId(), List.of()).stream().findFirst().orElse(null);
                LexicalPairExampleEntity primaryExample = primarySense == null
                        ? null
                        : exampleMap.getOrDefault(primarySense.getId(), List.of()).stream().findFirst().orElse(null);
                printer.printRecord(
                        pair.getEnglishWord(),
                        pair.getFrenchWord(),
                        pair.getChineseGloss(),
                        LexicalPairType.fromCode(pair.getLexicalPairType()).code(),
                        optionalDecimal(pair.getSemanticOverlapScore()),
                        optionalDecimal(pair.getFalseFriendRisk()),
                        pair.getDefaultContextSupport() == null ? null : ContextSupportLevel.fromCode(pair.getDefaultContextSupport()).code(),
                        pair.getDifficultyLevel(),
                        trimToNull(pair.getNotes()),
                        trimToNull(pair.getSource()),
                        pair.getActive(),
                        String.join("|", tagMap.getOrDefault(pair.getId(), List.of())),
                        pair.getKnowledgeStatus() == null ? null : KnowledgeStatus.fromCode(pair.getKnowledgeStatus()).code(),
                        pair.getEmbeddingStatus() == null ? null : EmbeddingStatus.fromCode(pair.getEmbeddingStatus()).code(),
                        primarySense == null ? null : trimToNull(primarySense.getEnglishDefinition()),
                        primarySense == null ? null : trimToNull(primarySense.getFrenchDefinition()),
                        primarySense == null ? null : trimToNull(primarySense.getChineseDefinition()),
                        primaryExample == null ? null : trimToNull(primaryExample.getEnglishExample()),
                        primaryExample == null ? null : trimToNull(primaryExample.getFrenchExample()),
                        primaryExample == null ? null : trimToNull(primaryExample.getChineseTranslation()),
                        primaryExample == null || primaryExample.getContextSupportLevel() == null
                                ? null
                                : ContextSupportLevel.fromCode(primaryExample.getContextSupportLevel()).code()
                );
            }
            printer.flush();
            String filename = "lexical-pairs-" + LocalDateTime.now().toLocalDate() + ".csv";
            return new CsvExportFile(filename, outputStream.toByteArray());
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to export lexical pairs", 500);
        }
    }

    public void validateImportCandidate(LexicalPairUpsertRequest request) {
        validatePairUniqueness(request.englishWord(), request.frenchWord(), null);
        validateSenses(request.senses());
        parseLexicalPairType(request.lexicalPairType());
        parseContextSupportLevel(request.defaultContextSupport());
        parseKnowledgeStatus(request.knowledgeStatus());
        parseEmbeddingStatus(request.embeddingStatus());
    }

    public CsvImportResultVO importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "CSV file must not be empty", 400);
        }

        List<CsvImportFailureVO> failures = new ArrayList<>();
        int successCount = 0;
        Set<String> seenPairKeys = new LinkedHashSet<>();
        Set<Long> changedPairIds = new LinkedHashSet<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            validateHeaders(parser.getHeaderMap().keySet());
            for (CSVRecord record : parser) {
                long rowNumber = record.getRecordNumber() + 1;
                try {
                    LexicalPairUpsertRequest request = parseImportRecord(record);
                    String pairKey = normalizePairKey(request.englishWord(), request.frenchWord());
                    if (!seenPairKeys.add(pairKey)) {
                        throw new BusinessException(ResultCode.CONFLICT, "Duplicate lexical pair in CSV file", 409);
                    }
                    Long lexicalPairId = transactionTemplate.execute(status -> createInternal(request));
                    if (lexicalPairId != null) {
                        changedPairIds.add(lexicalPairId);
                    }
                    successCount++;
                } catch (Exception exception) {
                    failures.add(new CsvImportFailureVO(
                            rowNumber,
                            getField(record, "english_word"),
                            getField(record, "french_word"),
                            resolveFailureMessage(exception)
                    ));
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Failed to read CSV file", 400);
        }

        if (!changedPairIds.isEmpty()) {
            publishKnowledgeChangedEvent(changedPairIds);
        }
        log.info("event=lexical_pair_csv_import successCount={} failedCount={}", successCount, failures.size());
        return new CsvImportResultVO(successCount, failures.size(), failures);
    }

    @Transactional
    public int backfillMissingSearchIndexes() {
        List<LexicalPairEntity> pairs = lexicalPairMapper.selectList(Wrappers.<LexicalPairEntity>lambdaQuery()
                .and(wrapper -> wrapper
                        .isNull(LexicalPairEntity::getSearchPinyin)
                        .or()
                        .eq(LexicalPairEntity::getSearchPinyin, "")
                        .or()
                        .isNull(LexicalPairEntity::getSearchInitials)
                        .or()
                        .eq(LexicalPairEntity::getSearchInitials, "")));
        for (LexicalPairEntity pair : pairs) {
            List<String> searchParts = new ArrayList<>();
            searchParts.add(pair.getEnglishWord());
            searchParts.add(pair.getFrenchWord());
            searchParts.add(pair.getChineseGloss());
            searchParts.add(pair.getSource());
            searchParts.add(pair.getNotes());
            searchParts.add(pair.getSearchableText());
            PinyinSearchIndexSupport.SearchIndex searchIndex = PinyinSearchIndexSupport.build(searchParts);
            pair.setSearchPinyin(searchIndex.pinyin());
            pair.setSearchInitials(searchIndex.initials());
            lexicalPairMapper.updateById(pair);
        }
        return pairs.size();
    }

    private Long createInternal(LexicalPairUpsertRequest request) {
        validatePairUniqueness(request.englishWord(), request.frenchWord(), null);
        validateSenses(request.senses());

        List<String> normalizedTags = normalizeTags(request.tags());
        LexicalPairEntity entity = new LexicalPairEntity();
        entity.setEnglishWord(request.englishWord().trim());
        entity.setFrenchWord(request.frenchWord().trim());
        entity.setChineseGloss(request.chineseGloss().trim());
        entity.setLexicalPairType(parseLexicalPairType(request.lexicalPairType()).name());
        entity.setSemanticOverlapScore(request.semanticOverlapScore());
        entity.setFalseFriendRisk(request.falseFriendRisk());
        entity.setDefaultContextSupport(parseContextSupportLevel(request.defaultContextSupport()).name());
        entity.setDifficultyLevel(request.difficultyLevel());
        entity.setNotes(trimToNull(request.notes()));
        entity.setSource(trimToNull(request.source()));
        entity.setActive(request.active() == null || request.active());
        entity.setKnowledgeStatus(parseKnowledgeStatus(request.knowledgeStatus()).name());
        EmbeddingStatus embeddingStatus = parseEmbeddingStatus(request.embeddingStatus());
        entity.setEmbeddingStatus(embeddingStatus.name());
        entity.setLastEmbeddedAt(embeddingStatus == EmbeddingStatus.EMBEDDED ? LocalDateTime.now() : null);
        entity.setSearchableText(buildPairSearchableText(request, normalizedTags));
        applySearchIndex(entity, request, normalizedTags);
        lexicalPairMapper.insert(entity);

        replaceSenses(entity.getId(), request.senses());
        replaceTags(entity.getId(), normalizedTags);
        return entity.getId();
    }

    private LambdaQueryWrapper<LexicalPairEntity> buildPageWrapper(LexicalPairPageQuery query) {
        LambdaQueryWrapper<LexicalPairEntity> wrapper = Wrappers.lambdaQuery();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            applyKeywordFilter(wrapper, query.keyword().trim().toLowerCase(Locale.ROOT));
        }
        if (query.lexicalPairType() != null && !query.lexicalPairType().isBlank()) {
            wrapper.eq(LexicalPairEntity::getLexicalPairType, parseLexicalPairType(query.lexicalPairType()).name());
        }
        if (query.contextSupportLevel() != null && !query.contextSupportLevel().isBlank()) {
            wrapper.eq(LexicalPairEntity::getDefaultContextSupport, parseContextSupportLevel(query.contextSupportLevel()).name());
        }
        if (query.riskLevel() != null && !query.riskLevel().isBlank()) {
            applyRiskFilter(wrapper, RiskLevel.fromCode(query.riskLevel()));
        }
        if (query.active() != null) {
            wrapper.eq(LexicalPairEntity::getActive, query.active());
        }
        if (query.embeddingStatus() != null && !query.embeddingStatus().isBlank()) {
            wrapper.eq(LexicalPairEntity::getEmbeddingStatus, parseEmbeddingStatus(query.embeddingStatus()).name());
        }
        return wrapper;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<LexicalPairEntity> wrapper, String normalizedKeyword) {
        String fuzzyKeyword = "%" + normalizedKeyword + "%";
        wrapper.and(condition -> condition
                .apply("LOWER(english_word) LIKE {0}", fuzzyKeyword)
                .or()
                .apply("LOWER(french_word) LIKE {0}", fuzzyKeyword)
                .or()
                .apply("LOWER(chinese_gloss) LIKE {0}", fuzzyKeyword)
                .or()
                .apply("LOWER(searchable_text) LIKE {0}", fuzzyKeyword)
                .or()
                .apply("LOWER(search_pinyin) LIKE {0}", fuzzyKeyword)
                .or()
                .apply("LOWER(search_initials) LIKE {0}", fuzzyKeyword));
    }

    private void applyRiskFilter(LambdaQueryWrapper<LexicalPairEntity> wrapper, RiskLevel riskLevel) {
        switch (riskLevel) {
            case LOW -> wrapper.lt(LexicalPairEntity::getFalseFriendRisk, new BigDecimal("0.25"));
            case MEDIUM -> wrapper.ge(LexicalPairEntity::getFalseFriendRisk, new BigDecimal("0.25"))
                    .lt(LexicalPairEntity::getFalseFriendRisk, new BigDecimal("0.50"));
            case HIGH -> wrapper.ge(LexicalPairEntity::getFalseFriendRisk, new BigDecimal("0.50"))
                    .lt(LexicalPairEntity::getFalseFriendRisk, new BigDecimal("0.75"));
            case CRITICAL -> wrapper.ge(LexicalPairEntity::getFalseFriendRisk, new BigDecimal("0.75"));
        }
    }

    private LexicalPairSummaryVO toSummaryVO(LexicalPairEntity pair, List<String> tags) {
        return new LexicalPairSummaryVO(
                pair.getId(),
                pair.getEnglishWord(),
                pair.getFrenchWord(),
                pair.getChineseGloss(),
                pair.getLexicalPairType(),
                pair.getSemanticOverlapScore(),
                pair.getFalseFriendRisk(),
                LexicalRiskSupport.resolve(pair.getFalseFriendRisk()).name(),
                pair.getDefaultContextSupport(),
                pair.getDifficultyLevel(),
                pair.getSource(),
                pair.getActive(),
                pair.getKnowledgeStatus(),
                pair.getEmbeddingStatus(),
                pair.getLastEmbeddedAt(),
                tags
        );
    }

    private String optionalDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private LexicalPairEntity requirePair(Long lexicalPairId) {
        LexicalPairEntity entity = lexicalPairMapper.selectById(lexicalPairId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical pair was not found", 404);
        }
        return entity;
    }

    private void validatePairUniqueness(String englishWord, String frenchWord, Long lexicalPairId) {
        LexicalPairEntity existing = lexicalPairMapper.selectByWords(englishWord.trim(), frenchWord.trim());
        if (existing != null && !Objects.equals(existing.getId(), lexicalPairId)) {
            throw new BusinessException(ResultCode.CONFLICT, "Lexical pair already exists", 409);
        }
    }

    private void validateSenses(List<LexicalPairSenseRequest> senses) {
        if (senses == null) {
            return;
        }
        for (LexicalPairSenseRequest sense : senses) {
            boolean hasDefinition = hasText(sense.englishDefinition())
                    || hasText(sense.frenchDefinition())
                    || hasText(sense.chineseDefinition());
            if (!hasDefinition) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "Each sense must have at least one definition", 400);
            }
            if (sense.examples() == null) {
                continue;
            }
            for (LexicalPairExampleRequest example : sense.examples()) {
                boolean hasExample = hasText(example.englishExample())
                        || hasText(example.frenchExample())
                        || hasText(example.chineseTranslation());
                if (!hasExample) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "Each example must contain at least one sentence or translation", 400);
                }
                parseContextSupportLevel(example.contextSupportLevel());
            }
        }
    }

    private void replaceSenses(Long lexicalPairId, List<LexicalPairSenseRequest> senses) {
        List<LexicalPairSenseEntity> existingSenses = lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                .eq(LexicalPairSenseEntity::getLexicalPairId, lexicalPairId));
        if (!existingSenses.isEmpty()) {
            List<Long> senseIds = existingSenses.stream().map(LexicalPairSenseEntity::getId).toList();
            lexicalPairExampleMapper.delete(Wrappers.<LexicalPairExampleEntity>lambdaQuery()
                    .in(LexicalPairExampleEntity::getLexicalPairSenseId, senseIds));
            lexicalPairSenseMapper.delete(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                    .eq(LexicalPairSenseEntity::getLexicalPairId, lexicalPairId));
        }

        if (senses == null || senses.isEmpty()) {
            return;
        }
        List<LexicalPairSenseRequest> sortedSenses = senses.stream()
                .sorted(Comparator.comparing(request -> request.sortOrder() == null ? Integer.MAX_VALUE : request.sortOrder()))
                .toList();
        for (LexicalPairSenseRequest request : sortedSenses) {
            LexicalPairSenseEntity senseEntity = new LexicalPairSenseEntity();
            senseEntity.setLexicalPairId(lexicalPairId);
            senseEntity.setSortOrder(request.sortOrder());
            senseEntity.setEnglishDefinition(trimToNull(request.englishDefinition()));
            senseEntity.setFrenchDefinition(trimToNull(request.frenchDefinition()));
            senseEntity.setChineseDefinition(trimToNull(request.chineseDefinition()));
            senseEntity.setSearchableText(SearchableTextBuilder.concat(
                    request.englishDefinition(),
                    request.frenchDefinition(),
                    request.chineseDefinition()
            ));
            lexicalPairSenseMapper.insert(senseEntity);

            if (request.examples() == null || request.examples().isEmpty()) {
                continue;
            }
            List<LexicalPairExampleRequest> sortedExamples = request.examples().stream()
                    .sorted(Comparator.comparing(example -> example.sortOrder() == null ? Integer.MAX_VALUE : example.sortOrder()))
                    .toList();
            for (LexicalPairExampleRequest example : sortedExamples) {
                LexicalPairExampleEntity exampleEntity = new LexicalPairExampleEntity();
                exampleEntity.setLexicalPairSenseId(senseEntity.getId());
                exampleEntity.setSortOrder(example.sortOrder());
                exampleEntity.setEnglishExample(trimToNull(example.englishExample()));
                exampleEntity.setFrenchExample(trimToNull(example.frenchExample()));
                exampleEntity.setChineseTranslation(trimToNull(example.chineseTranslation()));
                exampleEntity.setContextSupportLevel(parseContextSupportLevel(example.contextSupportLevel()).name());
                exampleEntity.setSource(trimToNull(example.source()));
                exampleEntity.setSearchableText(SearchableTextBuilder.concat(
                        example.englishExample(),
                        example.frenchExample(),
                        example.chineseTranslation()
                ));
                lexicalPairExampleMapper.insert(exampleEntity);
            }
        }
    }

    private void replaceTags(Long lexicalPairId, List<String> tags) {
        lexicalPairTagRelMapper.delete(Wrappers.<LexicalPairTagRelEntity>lambdaQuery()
                .eq(LexicalPairTagRelEntity::getLexicalPairId, lexicalPairId));
        if (tags.isEmpty()) {
            return;
        }

        List<Long> tagIds = new ArrayList<>();
        for (String tag : tags) {
            LexicalTagEntity existingTag = lexicalTagMapper.selectByTagName(tag);
            if (existingTag == null) {
                LexicalTagEntity newTag = new LexicalTagEntity();
                newTag.setTagName(tag);
                newTag.setActive(Boolean.TRUE);
                lexicalTagMapper.insert(newTag);
                tagIds.add(newTag.getId());
            } else {
                tagIds.add(existingTag.getId());
            }
        }
        for (Long tagId : tagIds) {
            LexicalPairTagRelEntity relation = new LexicalPairTagRelEntity();
            relation.setLexicalPairId(lexicalPairId);
            relation.setLexicalTagId(tagId);
            lexicalPairTagRelMapper.insert(relation);
        }
    }

    private void registerKnowledgeChangedEvent(Collection<Long> lexicalPairIds) {
        publishKnowledgeChangedEvent(lexicalPairIds);
    }

    private void publishKnowledgeChangedEvent(Collection<Long> lexicalPairIds) {
        List<String> sourceIds = lexicalPairIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
        if (sourceIds.isEmpty()) {
            return;
        }
        lexicalKnowledgeChangedEventPublisher.publish(new com.huashi.eftransfer.shared.event.LexicalKnowledgeChangedEvent(
                UUID.randomUUID().toString(),
                1,
                "LEXICAL_PAIR",
                sourceIds,
                OffsetDateTime.now(ZoneOffset.UTC),
                MDC.get("traceId")
        ));
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
                .collect(Collectors.groupingBy(
                        LexicalPairExampleEntity::getLexicalPairSenseId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, List<String>> loadTagMap(List<Long> lexicalPairIds) {
        if (lexicalPairIds == null || lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        List<LexicalPairTagRelEntity> relations = lexicalPairTagRelMapper.selectList(Wrappers.<LexicalPairTagRelEntity>lambdaQuery()
                .in(LexicalPairTagRelEntity::getLexicalPairId, lexicalPairIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Map<Long, LexicalTagEntity> tagMap = lexicalTagMapper.selectBatchIds(relations.stream()
                        .map(LexicalPairTagRelEntity::getLexicalTagId)
                        .distinct()
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(LexicalTagEntity::getId, entity -> entity));

        Map<Long, List<String>> pairTagMap = new LinkedHashMap<>();
        for (LexicalPairTagRelEntity relation : relations) {
            LexicalTagEntity tag = tagMap.get(relation.getLexicalTagId());
            if (tag == null) {
                continue;
            }
            pairTagMap.computeIfAbsent(relation.getLexicalPairId(), key -> new ArrayList<>()).add(tag.getTagName());
        }
        pairTagMap.replaceAll((key, value) -> value.stream().distinct().toList());
        return pairTagMap;
    }

    private LexicalPairExampleVO toExampleVO(LexicalPairExampleEntity example) {
        return new LexicalPairExampleVO(
                example.getId(),
                example.getSortOrder(),
                example.getEnglishExample(),
                example.getFrenchExample(),
                example.getChineseTranslation(),
                example.getContextSupportLevel(),
                example.getSource()
        );
    }

    private void applySearchIndex(LexicalPairEntity entity, LexicalPairUpsertRequest request, List<String> tags) {
        PinyinSearchIndexSupport.SearchIndex searchIndex = buildSearchIndex(request, tags);
        entity.setSearchPinyin(searchIndex.pinyin());
        entity.setSearchInitials(searchIndex.initials());
    }

    private PinyinSearchIndexSupport.SearchIndex buildSearchIndex(LexicalPairUpsertRequest request, List<String> tags) {
        List<String> parts = buildSearchParts(request, tags);
        return PinyinSearchIndexSupport.build(parts);
    }

    private List<String> buildSearchParts(LexicalPairUpsertRequest request, List<String> tags) {
        List<String> parts = new ArrayList<>();
        parts.add(request.englishWord());
        parts.add(request.frenchWord());
        parts.add(request.chineseGloss());
        parts.add(request.notes());
        parts.add(request.source());
        parts.addAll(tags);
        if (request.senses() != null) {
            for (LexicalPairSenseRequest sense : request.senses()) {
                parts.add(sense.englishDefinition());
                parts.add(sense.frenchDefinition());
                parts.add(sense.chineseDefinition());
                if (sense.examples() == null) {
                    continue;
                }
                for (LexicalPairExampleRequest example : sense.examples()) {
                    parts.add(example.englishExample());
                    parts.add(example.frenchExample());
                    parts.add(example.chineseTranslation());
                }
            }
        }
        return parts;
    }

    private String buildPairSearchableText(LexicalPairUpsertRequest request, List<String> tags) {
        return SearchableTextBuilder.concat(buildSearchParts(request, tags));
    }

    private SuggestionMatch resolveSuggestionMatch(LexicalPairEntity pair, String keyword) {
        String englishWord = safeLower(pair.getEnglishWord());
        String frenchWord = safeLower(pair.getFrenchWord());
        String chineseGloss = safeLower(pair.getChineseGloss());
        String searchPinyin = safeLower(pair.getSearchPinyin());
        String searchInitials = safeLower(pair.getSearchInitials());
        String searchableText = safeLower(pair.getSearchableText());

        if (englishWord.equals(keyword)) {
            return new SuggestionMatch("ENGLISH_WORD", 0);
        }
        if (frenchWord.equals(keyword)) {
            return new SuggestionMatch("FRENCH_WORD", 1);
        }
        if (englishWord.startsWith(keyword)) {
            return new SuggestionMatch("ENGLISH_PREFIX", 2);
        }
        if (frenchWord.startsWith(keyword)) {
            return new SuggestionMatch("FRENCH_PREFIX", 3);
        }
        if (chineseGloss.contains(keyword)) {
            return new SuggestionMatch("CHINESE_GLOSS", 4);
        }
        if (searchPinyin.contains(keyword)) {
            return new SuggestionMatch("PINYIN", 5);
        }
        if (searchInitials.contains(keyword)) {
            return new SuggestionMatch("INITIALS", 6);
        }
        if (searchableText.contains(keyword)) {
            return new SuggestionMatch("SEARCHABLE_TEXT", 7);
        }
        return null;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String rawTag : tags) {
            if (rawTag == null || rawTag.isBlank()) {
                continue;
            }
            String trimmed = rawTag.trim();
            normalized.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }
        return List.copyOf(normalized.values());
    }

    private LexicalPairType parseLexicalPairType(String value) {
        try {
            return LexicalPairType.fromCode(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }

    private ContextSupportLevel parseContextSupportLevel(String value) {
        try {
            return ContextSupportLevel.fromCode(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }

    private KnowledgeStatus parseKnowledgeStatus(String value) {
        if (value == null || value.isBlank()) {
            return KnowledgeStatus.DRAFT;
        }
        try {
            return KnowledgeStatus.fromCode(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }

    private EmbeddingStatus parseEmbeddingStatus(String value) {
        if (value == null || value.isBlank()) {
            return EmbeddingStatus.PENDING;
        }
        try {
            return EmbeddingStatus.fromCode(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }

    private void validateHeaders(Collection<String> headers) {
        Set<String> headerSet = headers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(requiredHeader -> !headerSet.contains(requiredHeader))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "Missing required CSV headers: " + String.join(", ", missingHeaders), 400);
        }
    }

    private LexicalPairUpsertRequest parseImportRecord(CSVRecord record) {
        String englishWord = requiredField(record, "english_word");
        String frenchWord = requiredField(record, "french_word");
        String chineseGloss = requiredField(record, "chinese_gloss");
        String lexicalPairType = requiredField(record, "lexical_pair_type");
        String semanticOverlapScore = requiredField(record, "semantic_overlap_score");
        String falseFriendRisk = requiredField(record, "false_friend_risk");
        String defaultContextSupport = requiredField(record, "default_context_support");
        String difficultyLevel = requiredField(record, "difficulty_level");

        List<String> tags = splitPipeSeparatedValues(getField(record, "tags"));
        List<LexicalPairSenseRequest> senses = buildImportSenses(record);

        return new LexicalPairUpsertRequest(
                englishWord,
                frenchWord,
                chineseGloss,
                lexicalPairType,
                parseDecimal(semanticOverlapScore, "semantic_overlap_score"),
                parseDecimal(falseFriendRisk, "false_friend_risk"),
                defaultContextSupport,
                parseInteger(difficultyLevel, "difficulty_level"),
                trimToNull(getField(record, "notes")),
                trimToNull(getField(record, "source")),
                parseBoolean(getField(record, "active")),
                trimToNull(getField(record, "knowledge_status")),
                trimToNull(getField(record, "embedding_status")),
                tags,
                senses
        );
    }

    private List<LexicalPairSenseRequest> buildImportSenses(CSVRecord record) {
        String senseEnglishDefinition = trimToNull(getField(record, "sense_english_definition"));
        String senseFrenchDefinition = trimToNull(getField(record, "sense_french_definition"));
        String senseChineseDefinition = trimToNull(getField(record, "sense_chinese_definition"));

        boolean hasSense = hasText(senseEnglishDefinition) || hasText(senseFrenchDefinition) || hasText(senseChineseDefinition);
        String exampleEnglish = trimToNull(getField(record, "example_english"));
        String exampleFrench = trimToNull(getField(record, "example_french"));
        String exampleChinese = trimToNull(getField(record, "example_chinese"));
        String exampleContextSupport = trimToNull(getField(record, "example_context_support"));
        boolean hasExample = hasText(exampleEnglish) || hasText(exampleFrench) || hasText(exampleChinese);
        if (!hasSense && !hasExample) {
            return List.of();
        }
        if (!hasSense && hasExample) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "Sense definition is required when example columns are provided", 400);
        }
        List<LexicalPairExampleRequest> examples = hasExample
                ? List.of(new LexicalPairExampleRequest(
                1,
                exampleEnglish,
                exampleFrench,
                exampleChinese,
                exampleContextSupport == null ? ContextSupportLevel.MEDIUM.code() : exampleContextSupport,
                null
        ))
                : List.of();
        return List.of(new LexicalPairSenseRequest(
                1,
                senseEnglishDefinition,
                senseFrenchDefinition,
                senseChineseDefinition,
                examples
        ));
    }

    private String requiredField(CSVRecord record, String header) {
        String value = getField(record, header);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, header + " is required", 400);
        }
        return value.trim();
    }

    private String getField(CSVRecord record, String header) {
        if (!record.isMapped(header)) {
            return null;
        }
        String value = record.get(header);
        return value == null ? null : value.trim();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.compareTo(BigDecimal.ZERO) < 0 || decimal.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be between 0 and 1", 400);
            }
            return decimal;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be a decimal number", 400);
        }
    }

    private Integer parseInteger(String value, String fieldName) {
        try {
            int integer = Integer.parseInt(value);
            if (integer < 1 || integer > 5) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be between 1 and 5", 400);
            }
            return integer;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be an integer", 400);
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return Boolean.TRUE;
        }
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        throw new BusinessException(ResultCode.VALIDATION_ERROR, "active must be true or false", 400);
    }

    private List<String> splitPipeSeparatedValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return normalizeTags(List.of(value.split("\\|")));
    }

    private String resolveFailureMessage(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException.getMessage();
            }
            current = current.getCause();
        }
        return exception.getMessage() == null ? "Unexpected import error" : exception.getMessage();
    }

    private String normalizePairKey(String englishWord, String frenchWord) {
        return englishWord.trim().toLowerCase(Locale.ROOT) + "::" + frenchWord.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
