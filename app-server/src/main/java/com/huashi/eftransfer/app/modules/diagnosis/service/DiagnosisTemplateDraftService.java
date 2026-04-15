package com.huashi.eftransfer.app.modules.diagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.service.TeachingClassService;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateDraftBasicRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateDraftItemRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateDraftPageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateDraftSaveRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateItemRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateOptionRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateScoringProfileRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateStimulusRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateUpsertRequest;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateDraftEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateDraftMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisStimulusPayload;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftBasicVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftItemVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftItemValidationVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftSchemaVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftSummaryVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftValidationResponseVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateItemVO;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DiagnosisTemplateDraftService {

    private static final String DEFAULT_TEMPLATE_NAME = "未命名模板草稿";
    private static final String DEFAULT_PUBLISH_TARGET = "SELF";
    private static final String CLASS_PUBLISH_TARGET = "CLASS";
    private static final String DEFAULT_SCORING_VERSION = "RULE_V1";
    private static final String STEP_BASIC_INFO = "BASIC_INFO";
    private static final String STEP_ITEM_CONFIGURATION = "ITEM_CONFIGURATION";
    private static final String STEP_PAIR_SELECTION = "PAIR_SELECTION";
    private static final String STEP_PREVIEW_PUBLISH = "PREVIEW_PUBLISH";

    private final DiagnosisTemplateDraftMapper diagnosisTemplateDraftMapper;
    private final DiagnosisTemplateService diagnosisTemplateService;
    private final LexicalPairMapper lexicalPairMapper;
    private final TeachingClassService teachingClassService;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final ObjectMapper objectMapper;

    public DiagnosisTemplateDraftService(
            DiagnosisTemplateDraftMapper diagnosisTemplateDraftMapper,
            DiagnosisTemplateService diagnosisTemplateService,
            LexicalPairMapper lexicalPairMapper,
            TeachingClassService teachingClassService,
            DiagnosisJsonCodec diagnosisJsonCodec,
            ObjectMapper objectMapper
    ) {
        this.diagnosisTemplateDraftMapper = diagnosisTemplateDraftMapper;
        this.diagnosisTemplateService = diagnosisTemplateService;
        this.lexicalPairMapper = lexicalPairMapper;
        this.teachingClassService = teachingClassService;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.objectMapper = objectMapper;
    }

    public PageResult<DiagnosisTemplateDraftSummaryVO> pageQuery(DiagnosisTemplateDraftPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        LambdaQueryWrapper<DiagnosisTemplateDraftEntity> wrapper = Wrappers.<DiagnosisTemplateDraftEntity>lambdaQuery()
                .eq(DiagnosisTemplateDraftEntity::getOwnerUserId, currentUserId())
                .orderByDesc(DiagnosisTemplateDraftEntity::getUpdatedAt)
                .orderByDesc(DiagnosisTemplateDraftEntity::getId);
        if (query.keyword() != null && !query.keyword().isBlank()) {
            String keyword = query.keyword().trim();
            wrapper.and(condition -> condition.like(DiagnosisTemplateDraftEntity::getTemplateName, keyword)
                    .or()
                    .like(DiagnosisTemplateDraftEntity::getDescription, keyword));
        }
        long total = diagnosisTemplateDraftMapper.selectCount(wrapper);
        List<DiagnosisTemplateDraftEntity> drafts = diagnosisTemplateDraftMapper.selectList(wrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        List<DiagnosisTemplateDraftSummaryVO> records = drafts.stream()
                .map(this::toSummary)
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    @Transactional
    public DiagnosisTemplateDraftDetailVO createBlankDraft() {
        DiagnosisTemplateDraftEntity entity = new DiagnosisTemplateDraftEntity();
        entity.setOwnerUserId(currentUserId());
        entity.setTemplateName(DEFAULT_TEMPLATE_NAME);
        entity.setDescription(null);
        entity.setPublishTarget(DEFAULT_PUBLISH_TARGET);
        entity.setEstimatedDurationMinutes(10);
        entity.setScoringVersion(DEFAULT_SCORING_VERSION);
        entity.setSyncState("DIRTY");
        entity.setVersion(1L);
        entity.setSchemaJson(writeSchema(blankSchema()));
        diagnosisTemplateDraftMapper.insert(entity);
        return toDetail(requireManageableDraft(entity.getId()));
    }

    @Transactional
    public DiagnosisTemplateDraftDetailVO createFromTemplate(Long templateId) {
        DiagnosisTemplateDraftEntity existing = diagnosisTemplateDraftMapper.selectOne(Wrappers.<DiagnosisTemplateDraftEntity>lambdaQuery()
                .eq(DiagnosisTemplateDraftEntity::getOwnerUserId, currentUserId())
                .eq(DiagnosisTemplateDraftEntity::getSourceTemplateId, templateId)
                .orderByDesc(DiagnosisTemplateDraftEntity::getUpdatedAt)
                .last("LIMIT 1"));
        if (existing != null) {
            return toDetail(existing);
        }

        DiagnosisTemplateDetailVO template = diagnosisTemplateService.getDetail(templateId);
        DiagnosisTemplateDraftSchemaVO schema = schemaFromTemplate(template);
        DiagnosisTemplateDraftEntity entity = new DiagnosisTemplateDraftEntity();
        entity.setOwnerUserId(currentUserId());
        entity.setSourceTemplateId(templateId);
        entity.setPublishedTemplateId(templateId);
        entity.setTemplateName(template.templateName());
        entity.setDescription(template.description());
        entity.setPublishTarget(template.targetClassId() == null ? DEFAULT_PUBLISH_TARGET : CLASS_PUBLISH_TARGET);
        entity.setEstimatedDurationMinutes(template.estimatedDurationMinutes());
        entity.setScoringVersion(template.scoringVersion());
        entity.setSyncState("IN_SYNC");
        entity.setVersion(1L);
        entity.setSchemaJson(writeSchema(schema));
        diagnosisTemplateDraftMapper.insert(entity);
        return toDetail(requireManageableDraft(entity.getId()));
    }

    public DiagnosisTemplateDraftDetailVO getDetail(Long draftId) {
        return toDetail(requireManageableDraft(draftId));
    }

    @Transactional
    public DiagnosisTemplateDraftDetailVO save(Long draftId, DiagnosisTemplateDraftSaveRequest request) {
        DiagnosisTemplateDraftEntity entity = requireManageableDraft(draftId);
        if (!Objects.equals(entity.getVersion(), request.version())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis template draft version mismatch", 409);
        }
        DiagnosisTemplateDraftSchemaVO schema = sanitizeSchema(request);
        entity.setTemplateName(schema.basic().templateName());
        entity.setDescription(schema.basic().description());
        entity.setPublishTarget(schema.basic().publishTarget());
        entity.setEstimatedDurationMinutes(schema.basic().estimatedDurationMinutes());
        entity.setScoringVersion(schema.basic().scoringVersion());
        entity.setSyncState("DIRTY");
        entity.setVersion(entity.getVersion() + 1);
        entity.setSchemaJson(writeSchema(schema));
        diagnosisTemplateDraftMapper.updateById(entity);
        return toDetail(requireManageableDraft(draftId));
    }

    public DiagnosisTemplateDraftValidationResponseVO validate(Long draftId) {
        DiagnosisTemplateDraftEntity entity = requireManageableDraft(draftId);
        return validateSchema(readSchema(entity.getSchemaJson()), true);
    }

    @Transactional
    public DiagnosisTemplateDetailVO publish(Long draftId) {
        DiagnosisTemplateDraftEntity entity = requireManageableDraft(draftId);
        DiagnosisTemplateDraftSchemaVO schema = readSchema(entity.getSchemaJson());
        DiagnosisTemplateDraftValidationResponseVO validation = validateSchema(schema, true);
        if (!validation.valid()) {
            String message = validation.fieldErrors().values().stream().findFirst()
                    .orElseGet(() -> validation.itemErrors().stream()
                            .findFirst()
                            .flatMap(item -> item.fieldErrors().values().stream().findFirst())
                            .orElse("Diagnosis template draft has blocking issues"));
            throw new BusinessException(ResultCode.CONFLICT, message, 409);
        }

        DiagnosisTemplateUpsertRequest payload = toTemplateUpsertRequest(schema, "PUBLISHED");
        Long templateId = entity.getPublishedTemplateId();
        if (templateId != null) {
            diagnosisTemplateService.update(templateId, payload);
        } else if (entity.getSourceTemplateId() != null) {
            templateId = entity.getSourceTemplateId();
            diagnosisTemplateService.update(templateId, payload);
        } else {
            templateId = diagnosisTemplateService.create(payload);
        }

        entity.setPublishedTemplateId(templateId);
        entity.setSourceTemplateId(templateId);
        entity.setTemplateName(schema.basic().templateName());
        entity.setDescription(schema.basic().description());
        entity.setPublishTarget(schema.basic().publishTarget());
        entity.setEstimatedDurationMinutes(schema.basic().estimatedDurationMinutes());
        entity.setScoringVersion(schema.basic().scoringVersion());
        entity.setSyncState("IN_SYNC");
        entity.setVersion(entity.getVersion() + 1);
        entity.setSchemaJson(writeSchema(schema));
        diagnosisTemplateDraftMapper.updateById(entity);
        return diagnosisTemplateService.getDetail(templateId);
    }

    @Transactional
    public void delete(Long draftId) {
        requireManageableDraft(draftId);
        diagnosisTemplateDraftMapper.deleteById(draftId);
    }

    private DiagnosisTemplateDraftSummaryVO toSummary(DiagnosisTemplateDraftEntity entity) {
        return new DiagnosisTemplateDraftSummaryVO(
                entity.getId(),
                entity.getSourceTemplateId(),
                entity.getPublishedTemplateId(),
                entity.getTemplateName(),
                entity.getDescription(),
                entity.getSyncState(),
                entity.getVersion(),
                entity.getUpdatedAt()
        );
    }

    private DiagnosisTemplateDraftDetailVO toDetail(DiagnosisTemplateDraftEntity entity) {
        return new DiagnosisTemplateDraftDetailVO(
                entity.getId(),
                entity.getSourceTemplateId(),
                entity.getPublishedTemplateId(),
                entity.getSyncState(),
                entity.getVersion(),
                hydrateLexicalPairs(readSchema(entity.getSchemaJson())),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DiagnosisTemplateDraftEntity requireManageableDraft(Long draftId) {
        DiagnosisTemplateDraftEntity entity = diagnosisTemplateDraftMapper.selectById(draftId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis template draft was not found", 404);
        }
        if (!Objects.equals(entity.getOwnerUserId(), currentUserId()) && !isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to manage this diagnosis template draft", 403);
        }
        return entity;
    }

    private DiagnosisTemplateDraftSchemaVO blankSchema() {
        return new DiagnosisTemplateDraftSchemaVO(
                new DiagnosisTemplateDraftBasicVO(DEFAULT_TEMPLATE_NAME, null, DEFAULT_PUBLISH_TARGET, 10, null, DEFAULT_SCORING_VERSION),
                List.of()
        );
    }

    private DiagnosisTemplateDraftSchemaVO schemaFromTemplate(DiagnosisTemplateDetailVO template) {
        List<DiagnosisTemplateDraftItemVO> items = template.items().stream()
                .map(this::toDraftItem)
                .toList();
        return new DiagnosisTemplateDraftSchemaVO(
                new DiagnosisTemplateDraftBasicVO(
                        template.templateName(),
                        template.description(),
                        template.targetClassId() == null ? DEFAULT_PUBLISH_TARGET : CLASS_PUBLISH_TARGET,
                        template.estimatedDurationMinutes(),
                        template.targetClassId(),
                        template.scoringVersion()
                ),
                items
        );
    }

    private DiagnosisTemplateDraftItemVO toDraftItem(DiagnosisTemplateItemVO item) {
        List<DiagnosisOptionPayload> options = item.options().stream()
                .map(option -> new DiagnosisOptionPayload(option.key(), option.label(), option.semanticMatch(), option.ignoreContextTrap()))
                .toList();
        DiagnosisScoringProfilePayload scoringProfile = item.scoringProfile() == null
                ? null
                : new DiagnosisScoringProfilePayload(
                        item.scoringProfile().formulaKey(),
                        item.scoringProfile().pairWeight(),
                        item.scoringProfile().riskAmplifier(),
                        item.scoringProfile().maxReactionTimeMs()
                );
        DiagnosisStimulusPayload stimulus = new DiagnosisStimulusPayload(
                item.stimulus().instruction(),
                item.stimulus().contextSentence(),
                item.stimulus().promptText()
        );
        return new DiagnosisTemplateDraftItemVO(
                "item-" + (item.id() == null ? UUID.randomUUID() : item.id()),
                item.lexicalPairId(),
                item.englishWord(),
                item.frenchWord(),
                item.chineseGloss(),
                item.lexicalPairType(),
                item.taskType(),
                item.blockCode(),
                item.sortOrder(),
                item.contextSupportLevel(),
                item.expectedSemanticMatch(),
                stimulus,
                options,
                item.correctAnswerKey(),
                scoringProfile
        );
    }

    private DiagnosisTemplateDraftSchemaVO sanitizeSchema(DiagnosisTemplateDraftSaveRequest request) {
        DiagnosisTemplateDraftBasicRequest basicRequest = request.schema().basic();
        DiagnosisTemplateDraftBasicVO basic = new DiagnosisTemplateDraftBasicVO(
                normalizeTemplateName(basicRequest == null ? null : basicRequest.templateName()),
                trimToNull(basicRequest == null ? null : basicRequest.description()),
                normalizePublishTarget(basicRequest == null ? null : basicRequest.publishTarget()),
                normalizeEstimatedDurationMinutes(basicRequest == null ? null : basicRequest.estimatedDurationMinutes()),
                normalizeTargetClassId(
                        basicRequest == null ? null : basicRequest.publishTarget(),
                        basicRequest == null ? null : basicRequest.targetClassId()
                ),
                normalizeScoringVersion(basicRequest == null ? null : basicRequest.scoringVersion())
        );
        List<DiagnosisTemplateDraftItemVO> items = (request.schema().items() == null ? List.<DiagnosisTemplateDraftItemRequest>of() : request.schema().items())
                .stream()
                .map(this::sanitizeDraftItem)
                .toList();
        return new DiagnosisTemplateDraftSchemaVO(basic, items);
    }

    private DiagnosisTemplateDraftItemVO sanitizeDraftItem(DiagnosisTemplateDraftItemRequest item) {
        List<DiagnosisOptionPayload> options = item.options() == null
                ? List.of()
                : item.options().stream()
                .map(option -> new DiagnosisOptionPayload(
                        trimToEmpty(option.key()),
                        trimToEmpty(option.label()),
                        option.semanticMatch(),
                        option.ignoreContextTrap()
                ))
                .toList();
        DiagnosisStimulusPayload stimulus = item.stimulus() == null
                ? new DiagnosisStimulusPayload("", null, null)
                : new DiagnosisStimulusPayload(
                trimToEmpty(item.stimulus().instruction()),
                trimToNull(item.stimulus().contextSentence()),
                trimToNull(item.stimulus().promptText())
        );
        DiagnosisScoringProfilePayload scoringProfile = item.scoringProfile() == null
                ? null
                : new DiagnosisScoringProfilePayload(
                trimToNull(item.scoringProfile().formulaKey()),
                item.scoringProfile().pairWeight(),
                item.scoringProfile().riskAmplifier(),
                item.scoringProfile().maxReactionTimeMs()
        );
        return new DiagnosisTemplateDraftItemVO(
                normalizeDraftItemId(item.draftItemId()),
                item.lexicalPairId(),
                null,
                null,
                null,
                null,
                trimToNull(item.taskType()),
                trimToNull(item.blockCode()),
                item.sortOrder(),
                trimToNull(item.contextSupportLevel()),
                item.expectedSemanticMatch(),
                stimulus,
                options,
                trimToNull(item.correctAnswerKey()),
                scoringProfile
        );
    }

    private DiagnosisTemplateDraftValidationResponseVO validateSchema(DiagnosisTemplateDraftSchemaVO schema, boolean publishing) {
        LinkedHashMap<String, String> fieldErrors = new LinkedHashMap<>();
        List<DiagnosisTemplateDraftItemValidationVO> itemErrors = new ArrayList<>();
        LinkedHashSet<String> blockingSteps = new LinkedHashSet<>();

        if (schema.basic() == null || !hasText(schema.basic().templateName())) {
            fieldErrors.put("templateName", "模板名称不能为空。");
            blockingSteps.add(STEP_BASIC_INFO);
        }
        if (schema.basic() == null || schema.basic().estimatedDurationMinutes() == null || schema.basic().estimatedDurationMinutes() <= 0) {
            fieldErrors.put("estimatedDurationMinutes", "预计时长必须大于 0。");
            blockingSteps.add(STEP_BASIC_INFO);
        }
        if (schema.basic() == null || !hasText(schema.basic().scoringVersion())) {
            fieldErrors.put("scoringVersion", "计分版本不能为空。");
            blockingSteps.add(STEP_BASIC_INFO);
        }
        if (schema.basic() != null && CLASS_PUBLISH_TARGET.equalsIgnoreCase(schema.basic().publishTarget())) {
            if (schema.basic().targetClassId() == null) {
                fieldErrors.put("targetClassId", "定向发布时必须选择班级。");
                blockingSteps.add(STEP_BASIC_INFO);
            } else {
                try {
                    teachingClassService.requireAccessibleClass(schema.basic().targetClassId());
                } catch (BusinessException exception) {
                    fieldErrors.put("targetClassId", "所选班级不存在或不可访问。");
                    blockingSteps.add(STEP_BASIC_INFO);
                }
            }
        }

        List<DiagnosisTemplateDraftItemVO> items = schema.items() == null ? List.of() : schema.items();
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairMap(items.stream()
                .map(DiagnosisTemplateDraftItemVO::lexicalPairId)
                .filter(Objects::nonNull)
                .toList());
        Set<String> blockOrderKeys = new LinkedHashSet<>();
        Set<String> taskTypes = new LinkedHashSet<>();
        Set<String> contextLevels = new LinkedHashSet<>();
        boolean hasExpectedTrue = false;
        boolean hasExpectedFalse = false;

        for (int index = 0; index < items.size(); index += 1) {
            DiagnosisTemplateDraftItemVO item = items.get(index);
            LinkedHashMap<String, String> errors = new LinkedHashMap<>();
            if (item.lexicalPairId() == null) {
                errors.put("lexicalPairId", "请选择词对。");
                blockingSteps.add(STEP_PAIR_SELECTION);
            } else if (!lexicalPairMap.containsKey(item.lexicalPairId())) {
                errors.put("lexicalPairId", "所选词对不存在或已删除。");
                blockingSteps.add(STEP_PAIR_SELECTION);
            }
            if (!hasText(item.taskType())) {
                errors.put("taskType", "题型不能为空。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            } else {
                taskTypes.add(item.taskType());
            }
            if (!hasText(item.blockCode())) {
                errors.put("blockCode", "Block code 不能为空。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            }
            if (item.sortOrder() == null || item.sortOrder() <= 0) {
                errors.put("sortOrder", "排序必须大于 0。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            }
            if (!hasText(item.contextSupportLevel())) {
                errors.put("contextSupportLevel", "语境支持等级不能为空。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            } else {
                contextLevels.add(item.contextSupportLevel());
            }
            if (item.expectedSemanticMatch() == null) {
                errors.put("expectedSemanticMatch", "请标记预期语义匹配结果。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            } else if (item.expectedSemanticMatch()) {
                hasExpectedTrue = true;
            } else {
                hasExpectedFalse = true;
            }
            if (item.stimulus() == null || !hasText(item.stimulus().instruction())) {
                errors.put("stimulus.instruction", "题目指令不能为空。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            }
            List<DiagnosisOptionPayload> options = item.options() == null ? List.of() : item.options();
            if (options.size() < 2) {
                errors.put("options", "至少需要 2 个选项。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            }
            if (!hasText(item.correctAnswerKey())) {
                errors.put("correctAnswerKey", "请指定正确答案。");
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            }

            if (hasText(item.blockCode()) && item.sortOrder() != null && item.sortOrder() > 0) {
                String uniqueKey = item.blockCode() + "#" + item.sortOrder();
                if (!blockOrderKeys.add(uniqueKey)) {
                    errors.put("blockCodeSortOrder", "同一 block 内的排序不能重复。");
                    blockingSteps.add(STEP_ITEM_CONFIGURATION);
                }
            }

            List<String> duplicateOptionKeys = findDuplicateOptionKeys(options);
            if (!duplicateOptionKeys.isEmpty()) {
                errors.put("options", buildDuplicateOptionKeyMessage(duplicateOptionKeys));
                blockingSteps.add(STEP_ITEM_CONFIGURATION);
            }

            Map<String, DiagnosisOptionPayload> optionMap = new LinkedHashMap<>();
            options.stream()
                    .filter(option -> hasText(option.key()))
                    .forEach(option -> optionMap.putIfAbsent(option.key(), option));
            if (hasText(item.correctAnswerKey())) {
                DiagnosisOptionPayload correctOption = optionMap.get(item.correctAnswerKey());
                if (correctOption == null) {
                    errors.put("correctAnswerKey", "正确答案必须匹配某个选项 key。");
                    blockingSteps.add(STEP_ITEM_CONFIGURATION);
                } else if (!Objects.equals(correctOption.semanticMatch(), item.expectedSemanticMatch())) {
                    errors.put("correctAnswerKey", "正确答案的语义标签必须与 expectedSemanticMatch 一致。");
                    blockingSteps.add(STEP_ITEM_CONFIGURATION);
                }
            }

            if ("REACTION_TIME".equalsIgnoreCase(item.taskType()) || "REACTION_TIME_TASK".equalsIgnoreCase(item.taskType())) {
                long semanticTrueCount = options.stream().filter(option -> Boolean.TRUE.equals(option.semanticMatch())).count();
                long semanticFalseCount = options.stream().filter(option -> Boolean.FALSE.equals(option.semanticMatch())).count();
                if (options.size() != 2 || semanticTrueCount != 1 || semanticFalseCount != 1) {
                    errors.put("options", "反应时题必须恰好包含一个语义一致和一个语义不一致选项。");
                    blockingSteps.add(STEP_ITEM_CONFIGURATION);
                }
            }
            if ("SEMANTIC_JUDGEMENT".equalsIgnoreCase(item.taskType()) || "SEMANTIC_JUDGEMENT_TASK".equalsIgnoreCase(item.taskType())) {
                boolean missingSemanticMatch = options.stream().anyMatch(option -> option.semanticMatch() == null);
                if (missingSemanticMatch) {
                    errors.put("options", "语义判断题的每个选项都必须标记 semanticMatch。");
                    blockingSteps.add(STEP_ITEM_CONFIGURATION);
                }
            }

            if (!errors.isEmpty()) {
                itemErrors.add(new DiagnosisTemplateDraftItemValidationVO(item.draftItemId(), index, errors));
            }
        }

        if (publishing) {
            if (items.isEmpty()) {
                fieldErrors.put("items", "发布前至少需要 1 个题项。");
                blockingSteps.add(STEP_PREVIEW_PUBLISH);
            }
            if (!(taskTypes.contains("REACTION_TIME") || taskTypes.contains("REACTION_TIME_TASK"))
                    || !(taskTypes.contains("SEMANTIC_JUDGEMENT") || taskTypes.contains("SEMANTIC_JUDGEMENT_TASK"))) {
                fieldErrors.put("taskTypeCoverage", "发布模板必须同时覆盖反应时和语义判断两种题型。");
                blockingSteps.add(STEP_PREVIEW_PUBLISH);
            }
            if (!(contextLevels.contains("LOW") && contextLevels.contains("MEDIUM") && contextLevels.contains("HIGH"))) {
                fieldErrors.put("contextSupportCoverage", "发布模板必须覆盖低、中、高三档语境支持。");
                blockingSteps.add(STEP_PREVIEW_PUBLISH);
            }
            if (!hasExpectedTrue || !hasExpectedFalse) {
                fieldErrors.put("semanticCoverage", "发布模板必须同时覆盖语义一致和语义不一致。");
                blockingSteps.add(STEP_PREVIEW_PUBLISH);
            }
        }

        return new DiagnosisTemplateDraftValidationResponseVO(fieldErrors.isEmpty() && itemErrors.isEmpty(), fieldErrors, itemErrors, blockingSteps);
    }

    private List<String> findDuplicateOptionKeys(List<DiagnosisOptionPayload> options) {
        LinkedHashSet<String> seenKeys = new LinkedHashSet<>();
        LinkedHashSet<String> duplicateKeys = new LinkedHashSet<>();
        options.stream()
                .map(DiagnosisOptionPayload::key)
                .filter(this::hasText)
                .forEach(key -> {
                    if (!seenKeys.add(key)) {
                        duplicateKeys.add(key);
                    }
                });
        return List.copyOf(duplicateKeys);
    }

    private String buildDuplicateOptionKeyMessage(List<String> duplicateOptionKeys) {
        return "选项 key 不能重复：" + String.join("、", duplicateOptionKeys) + "。";
    }

    private DiagnosisTemplateUpsertRequest toTemplateUpsertRequest(DiagnosisTemplateDraftSchemaVO schema, String status) {
        List<DiagnosisTemplateItemRequest> items = schema.items().stream()
                .map(item -> new DiagnosisTemplateItemRequest(
                        item.lexicalPairId(),
                        item.taskType(),
                        item.blockCode(),
                        item.sortOrder(),
                        item.contextSupportLevel(),
                        item.expectedSemanticMatch(),
                        new DiagnosisTemplateStimulusRequest(
                                item.stimulus().instruction(),
                                item.stimulus().contextSentence(),
                                item.stimulus().promptText()
                        ),
                        item.options().stream()
                                .map(option -> new DiagnosisTemplateOptionRequest(
                                        option.key(),
                                        option.label(),
                                        option.semanticMatch(),
                                        option.ignoreContextTrap()
                                ))
                                .toList(),
                        item.correctAnswerKey(),
                        item.scoringProfile() == null
                                ? null
                                : new DiagnosisTemplateScoringProfileRequest(
                                item.scoringProfile().formulaKey(),
                                item.scoringProfile().pairWeight(),
                                item.scoringProfile().riskAmplifier(),
                                item.scoringProfile().maxReactionTimeMs()
                        )
                ))
                .toList();
        return new DiagnosisTemplateUpsertRequest(
                schema.basic().templateName(),
                schema.basic().description(),
                status,
                schema.basic().estimatedDurationMinutes(),
                schema.basic().targetClassId(),
                schema.basic().scoringVersion(),
                items
        );
    }

    private DiagnosisTemplateDraftSchemaVO hydrateLexicalPairs(DiagnosisTemplateDraftSchemaVO schema) {
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairMap(schema.items().stream()
                .map(DiagnosisTemplateDraftItemVO::lexicalPairId)
                .filter(Objects::nonNull)
                .toList());
        List<DiagnosisTemplateDraftItemVO> items = schema.items().stream()
                .map(item -> {
                    LexicalPairEntity pair = item.lexicalPairId() == null ? null : lexicalPairMap.get(item.lexicalPairId());
                    return new DiagnosisTemplateDraftItemVO(
                            item.draftItemId(),
                            item.lexicalPairId(),
                            pair == null ? item.englishWord() : pair.getEnglishWord(),
                            pair == null ? item.frenchWord() : pair.getFrenchWord(),
                            pair == null ? item.chineseGloss() : pair.getChineseGloss(),
                            pair == null ? item.lexicalPairType() : pair.getLexicalPairType(),
                            item.taskType(),
                            item.blockCode(),
                            item.sortOrder(),
                            item.contextSupportLevel(),
                            item.expectedSemanticMatch(),
                            item.stimulus(),
                            item.options(),
                            item.correctAnswerKey(),
                            item.scoringProfile()
                    );
                })
                .toList();
        return new DiagnosisTemplateDraftSchemaVO(schema.basic(), items);
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> ids) {
        Set<Long> normalizedIds = ids == null ? Set.of() : ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(normalizedIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(LexicalPairEntity::getId, pair -> pair));
    }

    private DiagnosisTemplateDraftSchemaVO readSchema(String json) {
        try {
            return objectMapper.readValue(json, DiagnosisTemplateDraftSchemaVO.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize diagnosis template draft schema", exception);
        }
    }

    private String writeSchema(DiagnosisTemplateDraftSchemaVO schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize diagnosis template draft schema", exception);
        }
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

    private String normalizeTemplateName(String value) {
        return hasText(value) ? value.trim() : DEFAULT_TEMPLATE_NAME;
    }

    private String normalizePublishTarget(String value) {
        if (!hasText(value)) {
            return DEFAULT_PUBLISH_TARGET;
        }
        return CLASS_PUBLISH_TARGET.equalsIgnoreCase(value.trim()) ? CLASS_PUBLISH_TARGET : DEFAULT_PUBLISH_TARGET;
    }

    private Integer normalizeEstimatedDurationMinutes(Integer value) {
        return value == null || value <= 0 ? 10 : value;
    }

    private Long normalizeTargetClassId(String publishTarget, Long targetClassId) {
        return CLASS_PUBLISH_TARGET.equalsIgnoreCase(normalizePublishTarget(publishTarget)) ? targetClassId : null;
    }

    private String normalizeScoringVersion(String value) {
        return hasText(value) ? value.trim() : DEFAULT_SCORING_VERSION;
    }

    private String normalizeDraftItemId(String value) {
        return hasText(value) ? value.trim() : "draft-" + UUID.randomUUID();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
