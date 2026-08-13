package com.huashi.eftransfer.app.modules.assessment.service;

import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayFailureReason;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.service.AiGenerationRecordService;
import com.huashi.eftransfer.app.modules.ai.service.AiOutputSchemaFactory;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAiAnalysisVO;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssessmentAiAnalysisProcessorTest {

    private AssessmentAiAnalysisMapper analysisMapper;
    private AssessmentMetricSnapshotMapper metricMapper;
    private AssessmentAttemptAnswerMapper answerMapper;
    private AiGatewayClient aiGatewayClient;
    private AiGenerationRecordService generationRecordService;
    private AssessmentJsonCodec jsonCodec;
    private ObjectMapper objectMapper;
    private AssessmentAiAnalysisProperties properties;
    private AssessmentAiAnalysisProcessor processor;

    @BeforeEach
    void setUp() {
        analysisMapper = mock(AssessmentAiAnalysisMapper.class);
        metricMapper = mock(AssessmentMetricSnapshotMapper.class);
        answerMapper = mock(AssessmentAttemptAnswerMapper.class);
        aiGatewayClient = mock(AiGatewayClient.class);
        generationRecordService = mock(AiGenerationRecordService.class);
        jsonCodec = mock(AssessmentJsonCodec.class);
        objectMapper = mock(ObjectMapper.class);
        properties = new AssessmentAiAnalysisProperties();
        when(jsonCodec.write(any())).thenReturn("{}");
        when(answerMapper.selectList(any())).thenReturn(List.of());
        processor = new AssessmentAiAnalysisProcessor(
                analysisMapper, metricMapper, answerMapper, aiGatewayClient,
                new AiOutputSchemaFactory(), generationRecordService, jsonCodec, objectMapper, properties
        );
    }

    @Test
    void retriesProviderFailureBeforeUsingFallback() {
        AssessmentAiAnalysisEntity analysis = analysis(0);
        when(analysisMapper.selectById(13L)).thenReturn(analysis);
        when(metricMapper.selectById(28L)).thenReturn(metric());
        when(aiGatewayClient.structuredChat(any())).thenReturn(AiGatewayCallResult.failure(
                AiGatewayFailureReason.PROVIDER_UNAVAILABLE, "unavailable", true, 1, 42, "/internal/ai/chat/structured"
        ));

        processor.processClaimed(13L);

        assertThat(analysis.getStatus()).isEqualTo("PENDING");
        assertThat(analysis.getRetryCount()).isEqualTo(1);
        assertThat(analysis.getNextRetryAt()).isNotNull();
        assertThat(analysis.getRuleFallbackJson()).isNull();
        verify(analysisMapper).updateById(analysis);
    }

    @Test
    void marksRuleFallbackExplicitlyAfterRetryBudgetIsExhausted() {
        AssessmentAiAnalysisEntity analysis = analysis(properties.getMaxRetries());
        when(analysisMapper.selectById(13L)).thenReturn(analysis);
        when(metricMapper.selectById(28L)).thenReturn(metric());
        when(aiGatewayClient.structuredChat(any())).thenReturn(AiGatewayCallResult.failure(
                AiGatewayFailureReason.TIMEOUT, "timeout", true, 1, 2500, "/internal/ai/chat/structured"
        ));

        processor.processClaimed(13L);

        assertThat(analysis.getStatus()).isEqualTo("FALLBACK");
        assertThat(analysis.getRuleFallbackJson()).isEqualTo("{}");
        assertThat(analysis.getFallbackReason()).isEqualTo("TIMEOUT");
        assertThat(analysis.getCompletedAt()).isNotNull();
        verify(generationRecordService).save(any());
        verify(analysisMapper).updateById(analysis);
    }

    @Test
    void persistsCompletedRealModelAnalysis() {
        AssessmentAiAnalysisEntity analysis = analysis(0);
        AssessmentAiAnalysisVO output = new AssessmentAiAnalysisVO(
                "overview", List.of("strength one", "strength two"), List.of("risk one", "risk two"), "context", "reaction",
                List.of("one", "two", "three"), 0.8, "notice"
        );
        when(analysisMapper.selectById(13L)).thenReturn(analysis);
        when(metricMapper.selectById(28L)).thenReturn(metric());
        when(objectMapper.convertValue(any(Map.class), eq(AssessmentAiAnalysisVO.class))).thenReturn(output);
        when(aiGatewayClient.structuredChat(any())).thenReturn(AiGatewayCallResult.success(
                new StructuredChatResponse("deepseek", "deepseek-v4-flash", "raw", Map.of("ok", true),
                        "stop", "provider-request", new TokenUsage(10, 20, 30)),
                1, 321, "/internal/ai/chat/structured"
        ));

        processor.processClaimed(13L);

        assertThat(analysis.getStatus()).isEqualTo("COMPLETED");
        assertThat(analysis.getModelName()).isEqualTo("deepseek-v4-flash");
        assertThat(analysis.getPromptTokens()).isEqualTo(10);
        assertThat(analysis.getCompletionTokens()).isEqualTo(20);
        assertThat(analysis.getAnalysisJson()).isEqualTo("{}");
        verify(generationRecordService).save(any());
        verify(analysisMapper).updateById(analysis);
    }

    @Test
    void retriesUnexpectedPersistenceFailureInsteadOfLeavingProcessingForever() {
        AssessmentAiAnalysisEntity analysis = analysis(0);
        AssessmentAiAnalysisVO output = validOutput();
        when(analysisMapper.selectById(13L)).thenReturn(analysis);
        when(metricMapper.selectById(28L)).thenReturn(metric());
        when(objectMapper.convertValue(any(Map.class), eq(AssessmentAiAnalysisVO.class))).thenReturn(output);
        when(aiGatewayClient.structuredChat(any())).thenReturn(successfulResponse());
        org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
                .when(generationRecordService).save(any());

        processor.processClaimed(13L);

        assertThat(analysis.getStatus()).isEqualTo("PENDING");
        assertThat(analysis.getRetryCount()).isEqualTo(1);
        assertThat(analysis.getFallbackReason()).isEqualTo("UNHANDLED_IllegalStateException");
    }

    @Test
    void reusesGenerationRecordWhenAStaleClaimRepeatsCompletion() {
        AssessmentAiAnalysisEntity analysis = analysis(0);
        AiGenerationRecordEntity existing = new AiGenerationRecordEntity();
        existing.setId(99L);
        existing.setRequestId("assessment-analysis-13");
        when(analysisMapper.selectById(13L)).thenReturn(analysis);
        when(metricMapper.selectById(28L)).thenReturn(metric());
        when(objectMapper.convertValue(any(Map.class), eq(AssessmentAiAnalysisVO.class))).thenReturn(validOutput());
        when(aiGatewayClient.structuredChat(any())).thenReturn(successfulResponse());
        when(generationRecordService.findByRequestId("assessment-analysis-13")).thenReturn(null, existing);
        org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate request id"))
                .when(generationRecordService).save(any());

        processor.processClaimed(13L);

        assertThat(analysis.getStatus()).isEqualTo("COMPLETED");
        assertThat(analysis.getGenerationRecordId()).isEqualTo(99L);
    }

    @Test
    void sendsEvidenceCalibratedPrivacySafeSystemPrompt() {
        AssessmentAiAnalysisEntity analysis = analysis(0);
        when(analysisMapper.selectById(13L)).thenReturn(analysis);
        when(metricMapper.selectById(28L)).thenReturn(metric());
        when(aiGatewayClient.structuredChat(any())).thenReturn(AiGatewayCallResult.failure(
                AiGatewayFailureReason.PROVIDER_UNAVAILABLE, "unavailable", true, 1, 42,
                "/internal/ai/chat/structured"
        ));

        processor.processClaimed(13L);

        ArgumentCaptor<StructuredChatRequest> requestCaptor = ArgumentCaptor.forClass(StructuredChatRequest.class);
        verify(aiGatewayClient, atLeastOnce()).structuredChat(requestCaptor.capture());
        String systemPrompt = requestCaptor.getValue().messages().getFirst().content();
        assertThat(systemPrompt)
                .contains("观察到的表现", "证据限制", "恰好 3 条", "confidence")
                .contains("不得输出或猜测姓名、联系方式、IP、参与码")
                .contains("不能由速度直接推断熟练度、认真程度或认知能力");
    }

    private AssessmentAiAnalysisEntity analysis(int retryCount) {
        AssessmentAiAnalysisEntity analysis = new AssessmentAiAnalysisEntity();
        analysis.setId(13L);
        analysis.setAttemptId(16L);
        analysis.setMetricSnapshotId(28L);
        analysis.setPromptVersion(AssessmentAiAnalysisProcessor.PROMPT_VERSION);
        analysis.setStatus("PROCESSING");
        analysis.setRetryCount(retryCount);
        return analysis;
    }

    private AssessmentAiAnalysisVO validOutput() {
        return new AssessmentAiAnalysisVO(
                "overview", List.of("strength one", "strength two"), List.of("risk one", "risk two"), "context", "reaction",
                List.of("one", "two", "three"), 0.8, "notice"
        );
    }

    private AiGatewayCallResult<StructuredChatResponse> successfulResponse() {
        return AiGatewayCallResult.success(
                new StructuredChatResponse("deepseek", "deepseek-v4-flash", "raw", Map.of("ok", true),
                        "stop", "provider-request", new TokenUsage(10, 20, 30)),
                1, 321, "/internal/ai/chat/structured"
        );
    }

    private AssessmentMetricSnapshotEntity metric() {
        AssessmentMetricSnapshotEntity metric = new AssessmentMetricSnapshotEntity();
        metric.setId(28L);
        metric.setScoringVersion("SCORING_V1");
        metric.setRawScore(BigDecimal.valueOf(60));
        metric.setMaxScore(BigDecimal.valueOf(60));
        metric.setPercentageScore(BigDecimal.valueOf(100));
        metric.setMetricsJson("{}");
        metric.setQualityFlagsJson("[]");
        return metric;
    }
}
