package com.huashi.eftransfer.app.modules.diagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateItemRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateOptionRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplatePageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateScoringProfileRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateStimulusRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateUpsertRequest;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateItemEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateItemMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateItemVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateSummaryVO;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.DiagnosisTaskType;
import com.huashi.eftransfer.shared.enums.DiagnosisTemplateStatus;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiagnosisTemplateService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisTemplateService.class);

    private final DiagnosisTemplateMapper diagnosisTemplateMapper;
    private final DiagnosisTemplateItemMapper diagnosisTemplateItemMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final AuditLogService auditLogService;

    public DiagnosisTemplateService(
            DiagnosisTemplateMapper diagnosisTemplateMapper,
            DiagnosisTemplateItemMapper diagnosisTemplateItemMapper,
            LexicalPairMapper lexicalPairMapper,
            DiagnosisJsonCodec diagnosisJsonCodec,
            AuditLogService auditLogService
    ) {
        this.diagnosisTemplateMapper = diagnosisTemplateMapper;
        this.diagnosisTemplateItemMapper = diagnosisTemplateItemMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Long create(DiagnosisTemplateUpsertRequest request) {
        Long ownerUserId = currentUserId();
        DiagnosisTemplateStatus status = parseTemplateStatus(request.status());
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairs(request.items());
        validateTemplateItems(request.items(), status, lexicalPairMap);

        DiagnosisTemplateEntity entity = new DiagnosisTemplateEntity();
        entity.setTemplateName(request.templateName().trim());
        entity.setDescription(trimToNull(request.description()));
        entity.setOwnerUserId(ownerUserId);
        entity.setStatus(status.name());
        entity.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        entity.setScoringVersion(resolveScoringVersion(request.scoringVersion()));
        entity.setItemCount(request.items().size());
        entity.setMetadataJson(buildMetadataJson(request.items(), lexicalPairMap));
        diagnosisTemplateMapper.insert(entity);

        replaceItems(entity.getId(), request.items(), lexicalPairMap);
        auditLogService.record("template_create", "diagnosis_template", String.valueOf(entity.getId()), request, ResultCode.SUCCESS.code());
        log.info("event=diagnosis_template_created templateId={} ownerUserId={} status={} itemCount={}",
                entity.getId(), ownerUserId, entity.getStatus(), entity.getItemCount());
        return entity.getId();
    }

    @Transactional
    public Long update(Long templateId, DiagnosisTemplateUpsertRequest request) {
        DiagnosisTemplateEntity entity = requireManageableTemplate(templateId);
        DiagnosisTemplateStatus status = parseTemplateStatus(request.status());
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairs(request.items());
        validateTemplateItems(request.items(), status, lexicalPairMap);

        entity.setTemplateName(request.templateName().trim());
        entity.setDescription(trimToNull(request.description()));
        entity.setStatus(status.name());
        entity.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        entity.setScoringVersion(resolveScoringVersion(request.scoringVersion()));
        entity.setItemCount(request.items().size());
        entity.setMetadataJson(buildMetadataJson(request.items(), lexicalPairMap));
        diagnosisTemplateMapper.updateById(entity);

        diagnosisTemplateItemMapper.delete(Wrappers.<DiagnosisTemplateItemEntity>lambdaQuery()
                .eq(DiagnosisTemplateItemEntity::getTemplateId, templateId));
        replaceItems(templateId, request.items(), lexicalPairMap);
        auditLogService.record("template_update", "diagnosis_template", String.valueOf(templateId), request, ResultCode.SUCCESS.code());
        log.info("event=diagnosis_template_updated templateId={} status={} itemCount={}", templateId, entity.getStatus(), entity.getItemCount());
        return templateId;
    }

    public PageResult<DiagnosisTemplateSummaryVO> pageQuery(DiagnosisTemplatePageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        LambdaQueryWrapper<DiagnosisTemplateEntity> wrapper = Wrappers.<DiagnosisTemplateEntity>lambdaQuery()
                .orderByDesc(DiagnosisTemplateEntity::getUpdatedAt)
                .orderByDesc(DiagnosisTemplateEntity::getId);

        if (query.keyword() != null && !query.keyword().isBlank()) {
            String keyword = query.keyword().trim();
            wrapper.and(condition -> condition.like(DiagnosisTemplateEntity::getTemplateName, keyword)
                    .or()
                    .like(DiagnosisTemplateEntity::getDescription, keyword));
        }
        if (query.status() != null && !query.status().isBlank()) {
            wrapper.eq(DiagnosisTemplateEntity::getStatus, parseTemplateStatus(query.status()).name());
        }
        if (!isAdmin() || Boolean.TRUE.equals(query.mineOnly())) {
            wrapper.eq(DiagnosisTemplateEntity::getOwnerUserId, currentUserId());
        }

        long total = diagnosisTemplateMapper.selectCount(wrapper);
        List<DiagnosisTemplateEntity> templates = diagnosisTemplateMapper.selectList(wrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));

        List<DiagnosisTemplateSummaryVO> records = templates.stream()
                .map(entity -> new DiagnosisTemplateSummaryVO(
                        entity.getId(),
                        entity.getTemplateName(),
                        entity.getDescription(),
                        entity.getStatus(),
                        entity.getItemCount(),
                        entity.getEstimatedDurationMinutes(),
                        entity.getScoringVersion(),
                        entity.getOwnerUserId(),
                        entity.getUpdatedAt()
                ))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    public DiagnosisTemplateDetailVO getDetail(Long templateId) {
        DiagnosisTemplateEntity template = requireManageableTemplate(templateId);
        List<DiagnosisTemplateItemEntity> itemEntities = diagnosisTemplateItemMapper.selectList(Wrappers.<DiagnosisTemplateItemEntity>lambdaQuery()
                .eq(DiagnosisTemplateItemEntity::getTemplateId, templateId)
                .orderByAsc(DiagnosisTemplateItemEntity::getSortOrder)
                .orderByAsc(DiagnosisTemplateItemEntity::getId));
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairMap(itemEntities.stream()
                .map(DiagnosisTemplateItemEntity::getLexicalPairId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        List<DiagnosisTemplateItemVO> items = itemEntities.stream()
                .map(item -> toItemVO(item, lexicalPairMap.get(item.getLexicalPairId())))
                .toList();

        return new DiagnosisTemplateDetailVO(
                template.getId(),
                template.getTemplateName(),
                template.getDescription(),
                template.getStatus(),
                template.getEstimatedDurationMinutes(),
                template.getScoringVersion(),
                template.getItemCount(),
                template.getOwnerUserId(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                items
        );
    }

    public DiagnosisTemplateEntity requirePublishedTemplate(Long templateId) {
        DiagnosisTemplateEntity template = requireTemplate(templateId);
        if (!DiagnosisTemplateStatus.PUBLISHED.name().equals(template.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis template is not published", 409);
        }
        return template;
    }

    public DiagnosisTemplateEntity requireExistingTemplate(Long templateId) {
        return requireTemplate(templateId);
    }

    public List<DiagnosisTemplateItemEntity> listTemplateItems(Long templateId) {
        return diagnosisTemplateItemMapper.selectList(Wrappers.<DiagnosisTemplateItemEntity>lambdaQuery()
                .eq(DiagnosisTemplateItemEntity::getTemplateId, templateId)
                .orderByAsc(DiagnosisTemplateItemEntity::getSortOrder)
                .orderByAsc(DiagnosisTemplateItemEntity::getId));
    }

    private DiagnosisTemplateItemVO toItemVO(DiagnosisTemplateItemEntity item, LexicalPairEntity lexicalPair) {
        return new DiagnosisTemplateItemVO(
                item.getId(),
                item.getLexicalPairId(),
                lexicalPair == null ? null : lexicalPair.getEnglishWord(),
                lexicalPair == null ? null : lexicalPair.getFrenchWord(),
                lexicalPair == null ? null : lexicalPair.getChineseGloss(),
                lexicalPair == null ? null : lexicalPair.getLexicalPairType(),
                item.getTaskType(),
                item.getBlockCode(),
                item.getSortOrder(),
                item.getContextSupportLevel(),
                item.getExpectedSemanticMatch(),
                diagnosisJsonCodec.readStimulus(item.getStimulusPayloadJson()),
                diagnosisJsonCodec.readOptions(item.getOptionsPayloadJson()),
                item.getCorrectAnswerKey(),
                diagnosisJsonCodec.readScoringProfile(item.getScoringProfileJson())
        );
    }

    private DiagnosisTemplateEntity requireManageableTemplate(Long templateId) {
        DiagnosisTemplateEntity template = requireTemplate(templateId);
        if (!isAdmin() && !Objects.equals(template.getOwnerUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to manage this diagnosis template", 403);
        }
        return template;
    }

    private DiagnosisTemplateEntity requireTemplate(Long templateId) {
        DiagnosisTemplateEntity entity = diagnosisTemplateMapper.selectById(templateId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis template was not found", 404);
        }
        return entity;
    }

    private void replaceItems(Long templateId, List<DiagnosisTemplateItemRequest> items, Map<Long, LexicalPairEntity> lexicalPairMap) {
        for (DiagnosisTemplateItemRequest request : items) {
            LexicalPairEntity lexicalPair = lexicalPairMap.get(request.lexicalPairId());
            DiagnosisTemplateItemEntity entity = new DiagnosisTemplateItemEntity();
            entity.setTemplateId(templateId);
            entity.setLexicalPairId(request.lexicalPairId());
            entity.setTaskType(parseTaskType(request.taskType()).name());
            entity.setBlockCode(request.blockCode().trim());
            entity.setSortOrder(request.sortOrder());
            entity.setContextSupportLevel(parseContextSupportLevel(request.contextSupportLevel()).name());
            entity.setExpectedSemanticMatch(request.expectedSemanticMatch());
            entity.setStimulusPayloadJson(diagnosisJsonCodec.write(toStimulusPayload(request.stimulus())));
            entity.setOptionsPayloadJson(diagnosisJsonCodec.write(toOptionsPayloads(request.options())));
            entity.setCorrectAnswerKey(request.correctAnswerKey().trim());
            entity.setScoringProfileJson(diagnosisJsonCodec.write(resolveScoringProfile(request.scoringProfile(), request, lexicalPair)));
            diagnosisTemplateItemMapper.insert(entity);
        }
    }

    private DiagnosisStimulusPayload toStimulusPayload(DiagnosisTemplateStimulusRequest stimulus) {
        return new DiagnosisStimulusPayload(
                stimulus.instruction().trim(),
                trimToNull(stimulus.contextSentence()),
                trimToNull(stimulus.promptText())
        );
    }

    private List<DiagnosisOptionPayload> toOptionsPayloads(List<DiagnosisTemplateOptionRequest> options) {
        return options.stream()
                .map(option -> new DiagnosisOptionPayload(
                        option.key().trim(),
                        option.label().trim(),
                        option.semanticMatch(),
                        option.ignoreContextTrap()
                ))
                .toList();
    }

    private DiagnosisScoringProfilePayload resolveScoringProfile(
            DiagnosisTemplateScoringProfileRequest request,
            DiagnosisTemplateItemRequest itemRequest,
            LexicalPairEntity lexicalPair
    ) {
        DiagnosisTaskType taskType = parseTaskType(itemRequest.taskType());
        double pairWeight = request == null || request.pairWeight() == null ? defaultPairWeight(lexicalPair.getLexicalPairType()) : request.pairWeight();
        double riskAmplifier = request == null || request.riskAmplifier() == null ? lexicalPair.getFalseFriendRisk().doubleValue() + 1.0 : request.riskAmplifier();
        int maxReactionTime = request == null || request.maxReactionTimeMs() == null
                ? (taskType == DiagnosisTaskType.REACTION_TIME ? 1500 : 2000)
                : request.maxReactionTimeMs();
        String formulaKey = request == null || request.formulaKey() == null || request.formulaKey().isBlank()
                ? "RULE_V1"
                : request.formulaKey().trim();
        return new DiagnosisScoringProfilePayload(formulaKey, pairWeight, riskAmplifier, maxReactionTime);
    }

    private double defaultPairWeight(String lexicalPairType) {
        return switch (LexicalPairType.fromCode(lexicalPairType)) {
            case COGNATE -> 1.0;
            case PARTIAL_COGNATE -> 0.9;
            case FALSE_FRIEND, ORTHOGRAPHIC_SIMILAR -> 0.6;
        };
    }

    private String buildMetadataJson(List<DiagnosisTemplateItemRequest> items, Map<Long, LexicalPairEntity> lexicalPairMap) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("taskTypeCounts", items.stream().collect(Collectors.groupingBy(
                item -> parseTaskType(item.taskType()).name(),
                LinkedHashMap::new,
                Collectors.counting()
        )));
        metadata.put("contextSupportCounts", items.stream().collect(Collectors.groupingBy(
                item -> parseContextSupportLevel(item.contextSupportLevel()).name(),
                LinkedHashMap::new,
                Collectors.counting()
        )));
        metadata.put("lexicalPairTypeCounts", items.stream().collect(Collectors.groupingBy(
                item -> lexicalPairMap.get(item.lexicalPairId()).getLexicalPairType(),
                LinkedHashMap::new,
                Collectors.counting()
        )));
        metadata.put("semanticMatchCounts", items.stream().collect(Collectors.groupingBy(
                item -> item.expectedSemanticMatch() ? "EXPECTED_TRUE" : "EXPECTED_FALSE",
                LinkedHashMap::new,
                Collectors.counting()
        )));
        return diagnosisJsonCodec.write(metadata);
    }

    private void validateTemplateItems(
            List<DiagnosisTemplateItemRequest> items,
            DiagnosisTemplateStatus status,
            Map<Long, LexicalPairEntity> lexicalPairMap
    ) {
        Set<String> blockOrderKeys = new LinkedHashSet<>();
        List<String> taskTypes = new ArrayList<>();
        List<String> contextLevels = new ArrayList<>();
        boolean hasExpectedTrue = false;
        boolean hasExpectedFalse = false;

        for (DiagnosisTemplateItemRequest item : items) {
            DiagnosisTaskType taskType = parseTaskType(item.taskType());
            ContextSupportLevel contextSupportLevel = parseContextSupportLevel(item.contextSupportLevel());
            lexicalPairMap.computeIfAbsent(item.lexicalPairId(), ignored -> {
                throw new BusinessException(ResultCode.NOT_FOUND, "Lexical pair was not found: " + item.lexicalPairId(), 404);
            });

            validateOptions(item, taskType);
            boolean added = blockOrderKeys.add(item.blockCode().trim() + "#" + item.sortOrder());
            if (!added) {
                throw new BusinessException(ResultCode.CONFLICT, "Duplicate blockCode and sortOrder combination in template items", 409);
            }

            taskTypes.add(taskType.name());
            contextLevels.add(contextSupportLevel.name());
            if (item.expectedSemanticMatch()) {
                hasExpectedTrue = true;
            } else {
                hasExpectedFalse = true;
            }
        }

        if (status == DiagnosisTemplateStatus.PUBLISHED) {
            if (items.isEmpty()) {
                throw new BusinessException(ResultCode.CONFLICT, "Published template must contain at least one item", 409);
            }
            if (!taskTypes.contains(DiagnosisTaskType.REACTION_TIME.name())
                    || !taskTypes.contains(DiagnosisTaskType.SEMANTIC_JUDGEMENT.name())) {
                throw new BusinessException(ResultCode.CONFLICT, "Published template must contain both reaction time and semantic judgement tasks", 409);
            }
            if (!(contextLevels.contains(ContextSupportLevel.LOW.name())
                    && contextLevels.contains(ContextSupportLevel.MEDIUM.name())
                    && contextLevels.contains(ContextSupportLevel.HIGH.name()))) {
                throw new BusinessException(ResultCode.CONFLICT, "Published template must cover low, medium, and high context support levels", 409);
            }
            if (!hasExpectedTrue || !hasExpectedFalse) {
                throw new BusinessException(ResultCode.CONFLICT, "Published template must cover both semantic match and mismatch items", 409);
            }
        }
    }

    private void validateOptions(DiagnosisTemplateItemRequest item, DiagnosisTaskType taskType) {
        Map<String, DiagnosisTemplateOptionRequest> optionMap = item.options().stream()
                .collect(Collectors.toMap(
                        option -> option.key().trim(),
                        option -> option,
                        (left, right) -> {
                            throw new BusinessException(ResultCode.CONFLICT, "Duplicate option key in template item: " + left.key(), 409);
                        },
                        LinkedHashMap::new
                ));
        DiagnosisTemplateOptionRequest correctOption = optionMap.get(item.correctAnswerKey().trim());
        if (correctOption == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "correctAnswerKey must match one of the option keys");
        }
        if (correctOption.semanticMatch() == null || !Objects.equals(correctOption.semanticMatch(), item.expectedSemanticMatch())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "correct option semanticMatch must align with expectedSemanticMatch");
        }
        if (taskType == DiagnosisTaskType.REACTION_TIME) {
            if (item.options().size() != 2) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Reaction time task must contain exactly 2 options");
            }
            long semanticTrueCount = item.options().stream().filter(option -> Boolean.TRUE.equals(option.semanticMatch())).count();
            long semanticFalseCount = item.options().stream().filter(option -> Boolean.FALSE.equals(option.semanticMatch())).count();
            if (semanticTrueCount != 1 || semanticFalseCount != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Reaction time task options must include exactly one semantic true and one semantic false option");
            }
        } else {
            if (item.options().size() < 2) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Semantic judgement task must contain at least 2 options");
            }
            boolean missingSemanticMatch = item.options().stream().anyMatch(option -> option.semanticMatch() == null);
            if (missingSemanticMatch) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Semantic judgement task options must provide semanticMatch");
            }
        }
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairs(Collection<DiagnosisTemplateItemRequest> items) {
        Set<Long> ids = items.stream().map(DiagnosisTemplateItemRequest::lexicalPairId).collect(Collectors.toCollection(LinkedHashSet::new));
        return loadLexicalPairMap(ids);
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<LexicalPairEntity> pairs = lexicalPairMapper.selectBatchIds(ids);
        if (pairs.size() != ids.size()) {
            Set<Long> existingIds = pairs.stream().map(LexicalPairEntity::getId).collect(Collectors.toSet());
            Long missingId = ids.stream().filter(id -> !existingIds.contains(id)).findFirst().orElse(null);
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical pair was not found: " + missingId, 404);
        }
        return pairs.stream().collect(Collectors.toMap(LexicalPairEntity::getId, pair -> pair));
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveScoringVersion(String value) {
        if (value == null || value.isBlank()) {
            return "RULE_V1";
        }
        return value.trim();
    }

    private DiagnosisTemplateStatus parseTemplateStatus(String value) {
        try {
            return DiagnosisTemplateStatus.fromCode(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }

    private DiagnosisTaskType parseTaskType(String value) {
        try {
            return DiagnosisTaskType.fromCode(value.trim());
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
}
