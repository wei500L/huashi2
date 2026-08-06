package com.huashi.eftransfer.app.modules.ai.vo;

import com.huashi.eftransfer.shared.ai.RagCitation;
import com.huashi.eftransfer.shared.ai.RagContextChunk;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LexicalRagAnswerVO(
        @NotBlank(message = "requestId must not be blank")
        String requestId,
        @NotBlank(message = "conversationId must not be blank")
        String conversationId,
        @NotBlank(message = "generationSource must not be blank")
        String generationSource,
        String model,
        @NotNull(message = "latencyMs must not be null")
        Long latencyMs,
        boolean grounded,
        @NotBlank(message = "answer must not be blank")
        String answer,
        @NotBlank(message = "explanation must not be blank")
        String explanation,
        @NotEmpty(message = "recommendedActions must not be empty")
        List<@NotBlank(message = "recommendedActions item must not be blank") String> recommendedActions,
        @DecimalMin(value = "0.0", message = "confidence must be >= 0")
        @DecimalMax(value = "1.0", message = "confidence must be <= 1")
        double confidence,
        List<@NotBlank(message = "citationIds item must not be blank") String> citationIds,
        List<@Valid RagCitation> citations,
        List<@Valid RagContextChunk> contextChunks,
        String fallbackReason,
        String fallbackDetail
) {
    public LexicalRagAnswerVO(
            String requestId,
            String conversationId,
            String generationSource,
            String model,
            Long latencyMs,
            boolean grounded,
            String answer,
            String explanation,
            List<String> recommendedActions,
            double confidence,
            List<String> citationIds,
            List<RagCitation> citations,
            List<RagContextChunk> contextChunks,
            String fallbackReason
    ) {
        this(
                requestId,
                conversationId,
                generationSource,
                model,
                latencyMs,
                grounded,
                answer,
                explanation,
                recommendedActions,
                confidence,
                citationIds,
                citations,
                contextChunks,
                fallbackReason,
                null
        );
    }

    public LexicalRagAnswerVO withFallbackMeta(String reason, String detail) {
        return new LexicalRagAnswerVO(
                requestId,
                conversationId,
                generationSource,
                model,
                latencyMs,
                grounded,
                answer,
                explanation,
                recommendedActions,
                confidence,
                citationIds,
                citations,
                contextChunks,
                reason,
                detail
        );
    }
}
