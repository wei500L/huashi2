package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.service.AiGenerationRecordService;
import com.huashi.eftransfer.app.modules.ai.service.AiOutputSchemaFactory;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAiAnalysisVO;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class AssessmentAiAnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(AssessmentAiAnalysisProcessor.class);
    static final String PROMPT_VERSION = "assessment-analysis/v2";
    static final String SYSTEM_PROMPT = """
            你是 Lexi-Bridge 英法词汇认知迁移研究的结果解释助手。你的任务是把匿名评分证据转化为清晰、克制、可行动的中文反馈，而不是给参与者贴标签或作临床、智力、人格诊断。

            证据使用原则：
            1. 只依据输入 JSON。优先使用总分、各维度指标、计分题正误与质量标记；没有提供的维度不得补造。
            2. 将“观察到的表现”“可能的解释”“证据限制”明确分开。单次答题只能提供线索，不能证明稳定能力或因果关系。
            3. 反应时仅作辅助证据。出现 FAST_ITEM、SHORT_TOTAL_DURATION、缺失或异常计时标记时，必须降低 confidence，并明确说明不能由速度直接推断熟练度、认真程度或认知能力。
            4. 语境解释应比较孤立词义、句内语境、假朋友词、同形同义或其他实际存在的指标；若输入没有可比较数据，应直接说明证据不足。
            5. 不得输出或猜测姓名、联系方式、IP、参与码、身份、学校、健康状况等敏感信息，不得复述任何疑似个人信息。

            输出要求：
            - performanceOverview：2 至 3 句，先给总体结果，再指出最有证据支持的迁移模式，最后给出必要的不确定性说明。
            - strengths：2 至 4 条，每条写清“什么表现 + 哪类证据支持”，避免空泛表扬。
            - risks：2 至 4 条，区分知识薄弱点与数据质量限制，不使用“能力差”“不认真”等评判性措辞。
            - contextInterpretation：解释语境如何帮助或干扰判断；没有维度证据时明确写出无法比较。
            - reactionTimeInterpretation：结合质量标记校准措辞，不把快或慢直接等同于好或坏。
            - recommendations：恰好 3 条，分别说明要练什么、为什么、如何练；应能在真实学习中执行，且与本次证据对应。
            - confidence：0 到 1。证据完整且质量标记少时可提高；存在明显质量风险时必须降低。
            - qualityNotice：用一段易懂文字说明本分析的证据边界，以及参与者应该如何阅读结果。

            语言应自然、具体、尊重参与者。不要暴露内部字段名，不要声称模型看到了未提供的数据，不要把规则 fallback 描述成真实模型结论。
            """;
    private static final String SCENE = "ASSESSMENT_ANALYSIS";
    private static final String GENERATION_SOURCE_AI = "AI";
    private static final String GENERATION_SOURCE_FALLBACK = "RULE_FALLBACK";
    private static final Pattern SENSITIVE_OUTPUT = Pattern.compile(
            "(?i)([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}|\\b1[3-9]\\d{9}\\b|\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b|\\b[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}\\b)"
    );

    private final AssessmentAiAnalysisMapper aiAnalysisMapper;
    private final AssessmentMetricSnapshotMapper metricSnapshotMapper;
    private final AssessmentAttemptAnswerMapper answerMapper;
    private final AiGatewayClient aiGatewayClient;
    private final AiOutputSchemaFactory schemaFactory;
    private final AiGenerationRecordService generationRecordService;
    private final AssessmentJsonCodec jsonCodec;
    private final ObjectMapper objectMapper;
    private final AssessmentAiAnalysisProperties properties;

    public AssessmentAiAnalysisProcessor(
            AssessmentAiAnalysisMapper aiAnalysisMapper,
            AssessmentMetricSnapshotMapper metricSnapshotMapper,
            AssessmentAttemptAnswerMapper answerMapper,
            AiGatewayClient aiGatewayClient,
            AiOutputSchemaFactory schemaFactory,
            AiGenerationRecordService generationRecordService,
            AssessmentJsonCodec jsonCodec,
            ObjectMapper objectMapper,
            AssessmentAiAnalysisProperties properties
    ) {
        this.aiAnalysisMapper = aiAnalysisMapper;
        this.metricSnapshotMapper = metricSnapshotMapper;
        this.answerMapper = answerMapper;
        this.aiGatewayClient = aiGatewayClient;
        this.schemaFactory = schemaFactory;
        this.generationRecordService = generationRecordService;
        this.jsonCodec = jsonCodec;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "#{@assessmentAiAnalysisProperties.pollInterval.toMillis()}",
            fixedDelayString = "#{@assessmentAiAnalysisProperties.pollInterval.toMillis()}"
    )
    public void processPending() {
        if (!properties.isEnabled()) return;
        long staleSeconds = Math.max(1, properties.getProcessingTimeout().toSeconds());
        for (Long id : aiAnalysisMapper.selectProcessableIds(properties.getBatchSize(), staleSeconds)) {
            if (aiAnalysisMapper.claimForProcessing(id, staleSeconds) != 1) continue;
            processClaimed(id);
        }
    }

    void processClaimed(Long analysisId) {
        AssessmentAiAnalysisEntity analysis = aiAnalysisMapper.selectById(analysisId);
        if (analysis == null || !"PROCESSING".equals(analysis.getStatus())) return;
        try {
            AssessmentMetricSnapshotEntity metric = metricSnapshotMapper.selectById(analysis.getMetricSnapshotId());
            if (metric == null) {
                completeFallback(analysis, null, "METRIC_SNAPSHOT_MISSING", 0L, null);
                return;
            }
            List<AssessmentAttemptAnswerEntity> scoredAnswers = answerMapper.selectList(
                    Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                            .eq(AssessmentAttemptAnswerEntity::getAttemptId, analysis.getAttemptId())
                            .gt(AssessmentAttemptAnswerEntity::getQuestionScore, 0)
                            .orderByAsc(AssessmentAttemptAnswerEntity::getQuestionOrder)
            );
            Map<String, Object> payload = buildSafePayload(metric, scoredAnswers);
            StructuredChatRequest request = new StructuredChatRequest(
                    List.of(
                            new ChatMessage("system", SYSTEM_PROMPT),
                            new ChatMessage("user", "请分析以下匿名研究结果 JSON：\n" + jsonCodec.write(payload))
                    ),
                    null,
                    0.2d,
                    "assessment_analysis",
                    Boolean.TRUE,
                    schemaFactory.assessmentAnalysisSchema(),
                    "medium",
                    Boolean.FALSE
            );
            AiGatewayCallResult<StructuredChatResponse> result = aiGatewayClient.structuredChat(request);
            if (!result.success() || result.data() == null) {
                handleFailure(analysis, metric, result.failureReason() == null ? "UNKNOWN" : result.failureReason().name(),
                        result.latencyMs(), payload);
                return;
            }
            AssessmentAiAnalysisVO output;
            try {
                output = validateOutput(result.data().structuredData());
            } catch (RuntimeException exception) {
                handleFailure(analysis, metric, "INVALID_MODEL_OUTPUT", result.latencyMs(), payload);
                return;
            }
            completeAi(analysis, result.data(), output, payload, result.latencyMs());
        } catch (RuntimeException exception) {
            log.error("event=assessment_ai_analysis_unhandled analysisId={} attemptId={}",
                    analysisId, analysis.getAttemptId(), exception);
            AssessmentAiAnalysisEntity current = aiAnalysisMapper.selectById(analysisId);
            if (current == null || !"PROCESSING".equals(current.getStatus())) {
                return;
            }
            AssessmentMetricSnapshotEntity metric = metricSnapshotMapper.selectById(current.getMetricSnapshotId());
            handleFailure(current, metric, "UNHANDLED_" + exception.getClass().getSimpleName(), 0L, null);
        }
    }

    private Map<String, Object> buildSafePayload(
            AssessmentMetricSnapshotEntity metric,
            List<AssessmentAttemptAnswerEntity> answers
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scoringVersion", metric.getScoringVersion());
        payload.put("rawScore", metric.getRawScore());
        payload.put("maxScore", metric.getMaxScore());
        payload.put("percentageScore", metric.getPercentageScore());
        payload.put("metricSnapshot", metric.getMetricsJson());
        payload.put("qualityFlags", metric.getQualityFlagsJson());
        payload.put("scoredItems", answers.stream().map(answer -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("order", answer.getQuestionOrder());
            item.put("type", answer.getQuestionType());
            item.put("stem", answer.getStemTextSnapshot());
            item.put("prompt", answer.getPromptTextSnapshot());
            item.put("options", jsonCodec.readOptions(answer.getOptionsJsonSnapshot()));
            item.put("responseKeys", answer.getResponseJson());
            item.put("correctKeys", answer.getCorrectAnswerJson());
            item.put("correct", answer.getCorrect());
            item.put("scoreAwarded", answer.getScoreAwarded());
            item.put("maxScore", answer.getQuestionScore());
            item.put("effectiveDurationMs", answer.getEffectiveDurationMs());
            return item;
        }).toList());
        return payload;
    }

    private AssessmentAiAnalysisVO validateOutput(Map<String, Object> structuredData) {
        if (structuredData == null || structuredData.isEmpty()) throw new IllegalStateException("empty output");
        AssessmentAiAnalysisVO output = objectMapper.convertValue(structuredData, AssessmentAiAnalysisVO.class);
        if (blank(output.performanceOverview()) || blank(output.contextInterpretation())
                || blank(output.reactionTimeInterpretation()) || blank(output.qualityNotice())
                || output.confidence() == null
                || output.confidence() < 0 || output.confidence() > 1
                || output.strengths() == null || output.strengths().size() < 2 || output.strengths().size() > 4
                || output.risks() == null || output.risks().size() < 2 || output.risks().size() > 4
                || output.recommendations() == null || output.recommendations().size() != 3) {
            throw new IllegalStateException("incomplete output");
        }
        String serialized = jsonCodec.write(output);
        if (SENSITIVE_OUTPUT.matcher(serialized).find()) throw new IllegalStateException("sensitive output");
        return output;
    }

    private void completeAi(
            AssessmentAiAnalysisEntity analysis,
            StructuredChatResponse response,
            AssessmentAiAnalysisVO output,
            Map<String, Object> payload,
            long latencyMs
    ) {
        AiGenerationRecordEntity generation = generationRecord(
                analysis, response.model(), response.providerRequestId(), latencyMs, payload,
                Map.of("rawContent", response.rawContent(), "structuredData", response.structuredData()),
                output, GENERATION_SOURCE_AI, null, response.usage()
        );
        persistGenerationRecord(generation);
        analysis.setStatus("COMPLETED");
        analysis.setModelName(response.model());
        if (response.usage() != null) {
            analysis.setPromptTokens(response.usage().promptTokens());
            analysis.setCompletionTokens(response.usage().completionTokens());
        }
        analysis.setAnalysisJson(jsonCodec.write(output));
        analysis.setRawResponse(limit(response.rawContent(), 60_000));
        analysis.setGenerationRecordId(generation.getId());
        analysis.setCompletedAt(LocalDateTime.now());
        aiAnalysisMapper.updateById(analysis);
        log.info("event=assessment_ai_analysis_completed analysisId={} attemptId={} model={} latencyMs={}",
                analysis.getId(), analysis.getAttemptId(), response.model(), latencyMs);
    }

    private void handleFailure(
            AssessmentAiAnalysisEntity analysis,
            AssessmentMetricSnapshotEntity metric,
            String reason,
            long latencyMs,
            Map<String, Object> payload
    ) {
        int retryCount = analysis.getRetryCount() == null ? 0 : analysis.getRetryCount();
        if (retryCount < properties.getMaxRetries()) {
            analysis.setStatus("PENDING");
            analysis.setRetryCount(retryCount + 1);
            analysis.setFallbackReason(limit(reason, 1000));
            analysis.setNextRetryAt(LocalDateTime.now().plus(properties.getRetryDelay()));
            aiAnalysisMapper.updateById(analysis);
            log.warn("event=assessment_ai_analysis_retry analysisId={} attemptId={} retryCount={} reason={}",
                    analysis.getId(), analysis.getAttemptId(), retryCount + 1, reason);
            return;
        }
        completeFallback(analysis, metric, reason, latencyMs, payload);
    }

    private void completeFallback(
            AssessmentAiAnalysisEntity analysis,
            AssessmentMetricSnapshotEntity metric,
            String reason,
            long latencyMs,
            Map<String, Object> payload
    ) {
        AssessmentAiAnalysisVO fallback = ruleFallback(metric);
        Map<String, Object> safePayload = payload == null ? Map.of("metricSnapshotId", analysis.getMetricSnapshotId()) : payload;
        AiGenerationRecordEntity generation = generationRecord(
                analysis, null, null, latencyMs, safePayload, Map.of("failureReason", reason), fallback,
                GENERATION_SOURCE_FALLBACK, reason, null
        );
        persistGenerationRecord(generation);
        analysis.setStatus("FALLBACK");
        analysis.setRuleFallbackJson(jsonCodec.write(fallback));
        analysis.setFallbackReason(limit(reason, 1000));
        analysis.setGenerationRecordId(generation.getId());
        analysis.setCompletedAt(LocalDateTime.now());
        aiAnalysisMapper.updateById(analysis);
        log.warn("event=assessment_ai_analysis_fallback analysisId={} attemptId={} reason={} latencyMs={}",
                analysis.getId(), analysis.getAttemptId(), reason, latencyMs);
    }

    private AssessmentAiAnalysisVO ruleFallback(AssessmentMetricSnapshotEntity metric) {
        BigDecimal raw = metric == null || metric.getRawScore() == null ? BigDecimal.ZERO : metric.getRawScore();
        BigDecimal max = metric == null || metric.getMaxScore() == null ? BigDecimal.ZERO : metric.getMaxScore();
        BigDecimal percentage = metric == null || metric.getPercentageScore() == null ? BigDecimal.ZERO : metric.getPercentageScore();
        List<String> strengths = percentage.compareTo(BigDecimal.valueOf(80)) >= 0
                ? List.of("规则评分显示正式计分题整体掌握较稳定。")
                : List.of("已完成全部正式计分题，可据此定位后续练习重点。");
        List<String> risks = percentage.compareTo(BigDecimal.valueOf(60)) < 0
                ? List.of("部分英法词义对应和语境辨析仍需巩固。")
                : List.of("规则评分不能单独证明迁移策略已经稳定形成。");
        return new AssessmentAiAnalysisVO(
                "规则降级分析：本次正式计分题得分 " + raw.stripTrailingZeros().toPlainString()
                        + "/" + max.stripTrailingZeros().toPlainString() + "，百分比为 "
                        + percentage.stripTrailingZeros().toPlainString() + "% 。",
                strengths,
                risks,
                "建议结合各维度指标区分同形同义、假朋友词和语境修复表现；当前内容由确定性规则生成。",
                "反应时仅作为辅助证据；若存在过快作答或总时长偏短标记，不宜据此作能力定论。",
                List.of("复盘错误或犹豫较多的词项。", "用完整语境重新判断相近词义。", "间隔一段时间后进行同类题复测。"),
                0.55d,
                "真实模型分析暂不可用，当前展示的是规则 fallback，不代表 LLM 已成功生成。"
        );
    }

    private AiGenerationRecordEntity generationRecord(
            AssessmentAiAnalysisEntity analysis,
            String model,
            String providerRequestId,
            long latencyMs,
            Map<String, Object> input,
            Map<String, Object> raw,
            AssessmentAiAnalysisVO output,
            String source,
            String fallbackReason,
            com.huashi.eftransfer.shared.ai.TokenUsage usage
    ) {
        AiGenerationRecordEntity entity = new AiGenerationRecordEntity();
        entity.setRequestId("assessment-analysis-" + analysis.getId());
        entity.setScene(SCENE);
        entity.setPromptVersion(analysis.getPromptVersion());
        entity.setModel(model);
        entity.setProviderRequestId(providerRequestId);
        entity.setLatencyMs(latencyMs);
        entity.setTokenUsageJson(jsonCodec.write(usage == null ? Map.of() : usage));
        entity.setInputPayloadJson(jsonCodec.write(input));
        entity.setRawResponseJson(jsonCodec.write(raw));
        entity.setValidatedOutputJson(jsonCodec.write(output));
        entity.setGenerationSource(source);
        entity.setFallbackReason(limit(fallbackReason, 128));
        entity.setGeneratedAt(LocalDateTime.now());
        return entity;
    }

    private void persistGenerationRecord(AiGenerationRecordEntity generation) {
        AiGenerationRecordEntity existing = generationRecordService.findByRequestId(generation.getRequestId());
        if (existing != null) {
            generation.setId(existing.getId());
            return;
        }
        try {
            generationRecordService.save(generation);
        } catch (DuplicateKeyException duplicate) {
            existing = generationRecordService.findByRequestId(generation.getRequestId());
            if (existing == null) {
                throw duplicate;
            }
            generation.setId(existing.getId());
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
}
