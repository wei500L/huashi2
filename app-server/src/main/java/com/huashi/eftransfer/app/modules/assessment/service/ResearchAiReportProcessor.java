package com.huashi.eftransfer.app.modules.assessment.service;

import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.service.AiGenerationRecordService;
import com.huashi.eftransfer.app.modules.ai.service.AiOutputSchemaFactory;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAggregateSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAiReportEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchAggregateSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchAiReportMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiReportContentVO;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ResearchAiReportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ResearchAiReportProcessor.class);
    static final String SYSTEM_PROMPT = """
            你是 Lexi-Bridge 英法词汇迁移研究的群体报告助手。你只解读已经汇总的匿名统计事实，不得编造未提供的数据，不得输出因果结论。
            使用“观察到相关模式”“不能据此推断”这类克制措辞。
            禁止在输出中包含姓名、邮箱、手机号、IP、参与码、附件原文或任何可识别个人身份的信息。
            """;
    private static final Pattern SENSITIVE_OUTPUT = Pattern.compile(
            "(?i)([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}|\\b1[3-9]\\d{9}\\b|\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b|\\b[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}\\b)"
    );

    private final ResearchAiReportMapper reportMapper;
    private final ResearchAggregateSnapshotMapper snapshotMapper;
    private final AiGatewayClient aiGatewayClient;
    private final AiOutputSchemaFactory schemaFactory;
    private final AiGenerationRecordService generationRecordService;
    private final AssessmentJsonCodec jsonCodec;
    private final ObjectMapper objectMapper;
    private final ResearchAnalyticsProperties properties;

    public ResearchAiReportProcessor(
            ResearchAiReportMapper reportMapper,
            ResearchAggregateSnapshotMapper snapshotMapper,
            AiGatewayClient aiGatewayClient,
            AiOutputSchemaFactory schemaFactory,
            AiGenerationRecordService generationRecordService,
            AssessmentJsonCodec jsonCodec,
            ObjectMapper objectMapper,
            ResearchAnalyticsProperties properties
    ) {
        this.reportMapper = reportMapper;
        this.snapshotMapper = snapshotMapper;
        this.aiGatewayClient = aiGatewayClient;
        this.schemaFactory = schemaFactory;
        this.generationRecordService = generationRecordService;
        this.jsonCodec = jsonCodec;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "#{@researchAnalyticsProperties.pollInterval.toMillis()}",
            fixedDelayString = "#{@researchAnalyticsProperties.pollInterval.toMillis()}"
    )
    public void processPending() {
        if (!properties.isAiEnabled()) {
            return;
        }
        var staleBefore = LocalDateTime.now().minus(properties.getProcessingTimeout());
        for (Long id : reportMapper.selectProcessableIds(properties.getBatchSize(), staleBefore)) {
            if (reportMapper.claimForProcessing(id, staleBefore) != 1) {
                continue;
            }
            processClaimed(id);
        }
    }

    public void processClaimed(Long reportId) {
        ResearchAiReportEntity report = reportMapper.selectById(reportId);
        if (report == null || !"PROCESSING".equals(report.getStatus())) {
            return;
        }
        ResearchAggregateSnapshotEntity snapshot = snapshotMapper.selectById(report.getAggregateSnapshotId());
        if (snapshot == null) {
            completeFallback(report, "SNAPSHOT_MISSING", Map.of());
            return;
        }
        Map<String, Object> payload = buildSafePayload(snapshot);
        StructuredChatRequest request = new StructuredChatRequest(
                List.of(
                        new ChatMessage("system", SYSTEM_PROMPT),
                        new ChatMessage("user", "请基于以下不可变统计快照撰写群体研究报告 JSON：\n" + jsonCodec.write(payload))
                ),
                null,
                0.2d,
                "research_aggregate_report",
                Boolean.TRUE,
                schemaFactory.researchAggregateReportSchema(),
                "medium",
                Boolean.FALSE
        );
        AiGatewayCallResult<StructuredChatResponse> result = aiGatewayClient.structuredChat(request);
        if (!result.success() || result.data() == null) {
            handleFailure(report, result.failureReason() == null ? "UNKNOWN" : result.failureReason().name(), payload);
            return;
        }
        try {
            ResearchAiReportContentVO output = validate(result.data().structuredData());
            completeAi(report, result.data(), output, payload);
        } catch (RuntimeException exception) {
            handleFailure(report, "INVALID_MODEL_OUTPUT", payload);
        }
    }

    Map<String, Object> buildSafePayload(ResearchAggregateSnapshotEntity snapshot) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("snapshotVersion", snapshot.getSnapshotVersion());
        payload.put("sampleCount", snapshot.getSampleCount());
        payload.put("submittedCount", snapshot.getSubmittedCount());
        payload.put("statistics", jsonCodec.read(snapshot.getStatisticsJson(), Map.class));
        payload.put("qualitySummary", jsonCodec.read(snapshot.getQualitySummaryJson(), Map.class));
        payload.put("forbidden", List.of("name", "email", "phone", "ip", "participationCode", "attachmentContent"));
        return payload;
    }

    private ResearchAiReportContentVO validate(Map<String, Object> structured) {
        ResearchAiReportContentVO output = objectMapper.convertValue(structured, ResearchAiReportContentVO.class);
        if (output == null || blank(output.executiveSummary()) || output.observedPatterns() == null
                || output.researchCautions() == null || output.researchCautions().isEmpty()
                || output.confidence() == null || output.confidence() < 0 || output.confidence() > 1) {
            throw new IllegalStateException("incomplete output");
        }
        if (SENSITIVE_OUTPUT.matcher(jsonCodec.write(output)).find()) {
            throw new IllegalStateException("sensitive output");
        }
        return output;
    }

    private void completeAi(
            ResearchAiReportEntity report,
            StructuredChatResponse response,
            ResearchAiReportContentVO output,
            Map<String, Object> payload
    ) {
        AiGenerationRecordEntity generation = newGeneration(report, response.model(), payload, output, "AI", null);
        generationRecordService.save(generation);
        report.setStatus("COMPLETED");
        report.setModelName(response.model());
        if (response.usage() != null) {
            report.setPromptTokens(response.usage().promptTokens());
            report.setCompletionTokens(response.usage().completionTokens());
        }
        report.setReportJson(jsonCodec.write(output));
        report.setRawResponse(limit(response.rawContent(), 60_000));
        report.setGenerationRecordId(generation.getId());
        report.setCompletedAt(LocalDateTime.now());
        reportMapper.updateById(report);
        log.info("event=research_ai_report_completed reportId={} model={}", report.getId(), response.model());
    }

    private void handleFailure(ResearchAiReportEntity report, String reason, Map<String, Object> payload) {
        int retryCount = report.getRetryCount() == null ? 0 : report.getRetryCount();
        if (retryCount < properties.getMaxRetries()) {
            report.setStatus("PENDING");
            report.setRetryCount(retryCount + 1);
            report.setFallbackReason(limit(reason, 1000));
            report.setNextRetryAt(LocalDateTime.now().plus(properties.getRetryDelay()));
            reportMapper.updateById(report);
            return;
        }
        completeFallback(report, reason, payload);
    }

    private void completeFallback(ResearchAiReportEntity report, String reason, Map<String, Object> payload) {
        ResearchAiReportContentVO fallback = ruleFallback(payload);
        AiGenerationRecordEntity generation = newGeneration(report, null, payload, fallback, "RULE_FALLBACK", reason);
        generationRecordService.save(generation);
        report.setStatus("FALLBACK");
        report.setRuleFallbackJson(jsonCodec.write(fallback));
        report.setFallbackReason(limit(reason, 1000));
        report.setGenerationRecordId(generation.getId());
        report.setCompletedAt(LocalDateTime.now());
        reportMapper.updateById(report);
        log.warn("event=research_ai_report_fallback reportId={} reason={}", report.getId(), reason);
    }

    ResearchAiReportContentVO ruleFallback(Map<String, Object> payload) {
        Object sample = payload.get("sampleCount");
        return new ResearchAiReportContentVO(
                "当前报告依据规则汇总生成，不是模型结论。样本量：" + (sample == null ? "未知" : sample) + "。",
                List.of("已按题目、维度和反应时完成描述性统计。"),
                List.of("维度正确率仅反映本次过滤条件下的观察结果。"),
                List.of("正确率较低的题目应回到题干与选项分布继续核对。"),
                List.of("干扰项选择率需要结合题目顺序阅读，不能单独推断能力。"),
                List.of("反应时只统计有效计时样本，异常值已在质量标记中提示。"),
                List.of("质量标记会限制解释强度，不能把过快作答直接视为不认真。"),
                List.of("不能据此推断因果关系、稳定能力或个体特征。"),
                List.of("建议先核对低正确率题目与质量标记，再决定是否扩大样本。"),
                0.35d
        );
    }

    private AiGenerationRecordEntity newGeneration(
            ResearchAiReportEntity report,
            String model,
            Map<String, Object> input,
            ResearchAiReportContentVO output,
            String source,
            String reason
    ) {
        AiGenerationRecordEntity entity = new AiGenerationRecordEntity();
        entity.setRequestId("research-report-" + report.getId());
        entity.setScene("RESEARCH_AGGREGATE_REPORT");
        entity.setTeacherUserId(report.getRequestedBy());
        entity.setPromptVersion(report.getPromptVersion());
        entity.setModel(model);
        entity.setInputPayloadJson(jsonCodec.write(input));
        entity.setValidatedOutputJson(jsonCodec.write(output));
        entity.setGenerationSource(source);
        entity.setFallbackReason(limit(reason, 128));
        entity.setGeneratedAt(LocalDateTime.now());
        return entity;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
