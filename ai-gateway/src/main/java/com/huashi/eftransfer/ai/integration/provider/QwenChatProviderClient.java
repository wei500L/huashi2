package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class QwenChatProviderClient {

    private final AiRuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;
    private final ResilientAiExecutor resilientAiExecutor;
    private final AiProviderObservationService observationService;
    private final ProviderRequestContextHolder requestContextHolder;

    public QwenChatProviderClient(
            AiRuntimeConfigService runtimeConfigService,
            ObjectMapper objectMapper,
            ResilientAiExecutor resilientAiExecutor,
            AiProviderObservationService observationService,
            ProviderRequestContextHolder requestContextHolder
    ) {
        this.runtimeConfigService = runtimeConfigService;
        this.objectMapper = objectMapper;
        this.resilientAiExecutor = resilientAiExecutor;
        this.observationService = observationService;
        this.requestContextHolder = requestContextHolder;
    }

    public ChatResponse chat(ChatRequest request) {
        String provider = "qwen";
        String model = resolveModel(request.model());
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            AiRuntimeBundle bundle = runtimeConfigService.current();
            org.springframework.ai.chat.model.ChatResponse response = resilientAiExecutor.execute(
                    "chat",
                    () -> bundle.chatClient().prompt(toPrompt(request, model)).call().chatResponse()
            );
            ChatResponse chatResponse = new ChatResponse(
                    provider,
                    model,
                    response.getResult().getOutput().getText(),
                    normalizeFinishReason(response.getResult().getMetadata().getFinishReason()),
                    response.getMetadata().getId() != null ? response.getMetadata().getId() : requestContextHolder.getRequestId(),
                    toUsage(response.getMetadata().getUsage())
            );
            observationService.recordSuccess("chat", provider, model, startNanos, chatResponse.providerRequestId(), chatResponse.usage());
            return chatResponse;
        } catch (Exception ex) {
            throw observationService.recordFailure("chat", provider, model, startNanos, ex);
        }
    }

    public StructuredChatResponse structuredChat(StructuredChatRequest request) {
        String provider = "qwen";
        String model = resolveModel(request.model());
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            AiRuntimeBundle bundle = runtimeConfigService.current();
            org.springframework.ai.chat.model.ChatResponse response = resilientAiExecutor.execute(
                    "chat",
                    () -> bundle.chatModel().call(toStructuredPrompt(request, model))
            );
            String content = response.getResult().getOutput().getText();
            StructuredChatResponse structuredResponse = new StructuredChatResponse(
                    provider,
                    model,
                    content,
                    objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
                    }),
                    normalizeFinishReason(response.getResult().getMetadata().getFinishReason()),
                    response.getMetadata().getId() != null ? response.getMetadata().getId() : requestContextHolder.getRequestId(),
                    toUsage(response.getMetadata().getUsage())
            );
            observationService.recordSuccess(
                    "chat",
                    provider,
                    model,
                    startNanos,
                    structuredResponse.providerRequestId(),
                    structuredResponse.usage()
            );
            return structuredResponse;
        } catch (Exception ex) {
            throw observationService.recordFailure("chat", provider, model, startNanos, ex);
        }
    }

    private Prompt toPrompt(ChatRequest request, String model) {
        return new Prompt(toMessages(request.messages()), OpenAiChatOptions.builder()
                .model(model)
                .temperature(request.temperature() != null ? request.temperature() : defaultChat().temperature())
                .maxTokens(request.maxTokens() != null ? request.maxTokens() : defaultChat().maxTokens())
                .build());
    }

    private Prompt toStructuredPrompt(StructuredChatRequest request, String model) {
        boolean strict = request.strict() == null || request.strict();
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(ResponseFormat.JsonSchema.builder()
                        .name(request.schemaName())
                        .strict(strict)
                        .schema(writeSchema(request.schema()))
                        .build())
                .build();

        return new Prompt(toMessages(request.messages()), OpenAiChatOptions.builder()
                .model(model)
                .temperature(request.temperature() != null ? request.temperature() : defaultChat().temperature())
                .responseFormat(responseFormat)
                .build());
    }

    private List<Message> toMessages(List<ChatMessage> messages) {
        return messages.stream().map(this::toMessage).toList();
    }

    private Message toMessage(ChatMessage message) {
        return switch (message.role()) {
            case "system" -> new SystemMessage(message.content());
            case "assistant" -> new AssistantMessage(message.content());
            default -> new UserMessage(message.content());
        };
    }

    private String resolveModel(String requestModel) {
        return requestModel != null && !requestModel.isBlank() ? requestModel : defaultChat().model();
    }

    private com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig defaultChat() {
        return runtimeConfigService.current().config().provider().chat();
    }

    private TokenUsage toUsage(org.springframework.ai.chat.metadata.Usage usage) {
        if (usage == null) {
            return null;
        }
        return new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private String writeSchema(Map<String, Object> schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception ex) {
            throw new IllegalArgumentException("schema must be serializable to JSON", ex);
        }
    }

    private String normalizeFinishReason(String finishReason) {
        return finishReason == null ? null : finishReason.toLowerCase(Locale.ROOT);
    }
}
