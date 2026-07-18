package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
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

    public ChatResponse chat(String providerName, ChatRequest request) {
        return chat(providerRuntime(providerName), providerName, request);
    }

    public ChatResponse chat(AiProviderRuntime runtime, String providerName, ChatRequest request) {
        String provider = providerName;
        String model = resolveModel(runtime, request.model());
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            org.springframework.ai.chat.model.ChatResponse response = resilientAiExecutor.execute(
                    runtime,
                    "chat",
                    () -> runtime.chatClient().prompt(toPrompt(runtime, request, model)).call().chatResponse()
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

    public StructuredChatResponse structuredChat(String providerName, StructuredChatRequest request) {
        return structuredChat(providerRuntime(providerName), providerName, request);
    }

    public StructuredChatResponse structuredChat(
            AiProviderRuntime runtime,
            String providerName,
            StructuredChatRequest request
    ) {
        String provider = providerName;
        String model = resolveModel(runtime, request.model());
        long startNanos = System.nanoTime();
        requestContextHolder.clear();

        try {
            org.springframework.ai.chat.model.ChatResponse response = resilientAiExecutor.execute(
                    runtime,
                    "chat",
                    () -> runtime.chatModel().call(toStructuredPrompt(runtime, request, model))
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

    private Prompt toPrompt(AiProviderRuntime runtime, ChatRequest request, String model) {
        return new Prompt(toMessages(request.messages()), OpenAiChatOptions.builder()
                .model(model)
                .temperature(request.temperature() != null ? request.temperature() : defaultChat(runtime).temperature())
                .maxTokens(request.maxTokens() != null ? request.maxTokens() : defaultChat(runtime).maxTokens())
                .build());
    }

    private Prompt toStructuredPrompt(AiProviderRuntime runtime, StructuredChatRequest request, String model) {
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
                .temperature(request.temperature() != null ? request.temperature() : defaultChat(runtime).temperature())
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

    private String resolveModel(AiProviderRuntime runtime, String requestModel) {
        if (requestModel != null && !requestModel.isBlank()) {
            return requestModel;
        }
        return defaultChat(runtime).model();
    }

    private com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig defaultChat(AiProviderRuntime runtime) {
        return runtime.definition().chat();
    }

    private AiProviderRuntime providerRuntime(String providerName) {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        AiProviderRuntime runtime = bundle.providerRuntime(providerName);
        if (runtime == null) {
            throw new IllegalStateException("No configured AI provider runtime for " + providerName);
        }
        return runtime;
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
