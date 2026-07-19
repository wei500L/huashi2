package com.huashi.eftransfer.ai.integration.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.exception.InvalidProviderResponseException;
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
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.LinkedHashMap;
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
            if (AiOpsProtocols.OPENAI_RESPONSES.equals(runtime.definition().chat().protocol())) {
                ResponsesResult responsesResult = callResponses(
                        runtime,
                        request.messages(),
                        model,
                        request.maxTokens() != null ? request.maxTokens() : defaultChat(runtime).maxTokens(),
                        request.temperature() != null ? request.temperature() : defaultChat(runtime).temperature(),
                        request.reasoningEffort(),
                        null,
                        null,
                        null
                );
                ChatResponse chatResponse = new ChatResponse(
                        provider,
                        responsesResult.model(),
                        responsesResult.content(),
                        responsesResult.finishReason(),
                        responsesResult.providerRequestId(),
                        responsesResult.usage()
                );
                observationService.recordSuccess("chat", provider, model, startNanos, chatResponse.providerRequestId(), chatResponse.usage());
                return chatResponse;
            }
            ProviderChatResult providerResult = resilientAiExecutor.execute(
                    runtime,
                    "chat",
                    () -> validateChatResponse(
                            runtime.chatClient().prompt(toPrompt(runtime, request, model)).call().chatResponse(),
                            model
                    )
            );
            org.springframework.ai.chat.model.ChatResponse response = providerResult.response();
            ChatResponse chatResponse = new ChatResponse(
                    provider,
                    model,
                    providerResult.content(),
                    finishReason(response),
                    providerRequestId(response),
                    response.getMetadata() == null ? null : toUsage(response.getMetadata().getUsage())
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
            if (AiOpsProtocols.OPENAI_RESPONSES.equals(runtime.definition().chat().protocol())) {
                ResponsesResult responsesResult = callResponses(
                        runtime,
                        request.messages(),
                        model,
                        defaultChat(runtime).maxTokens(),
                        request.temperature() != null ? request.temperature() : defaultChat(runtime).temperature(),
                        request.reasoningEffort(),
                        request.schemaName(),
                        request.strict(),
                        request.schema()
                );
                Map<String, Object> structuredData;
                try {
                    structuredData = objectMapper.readValue(
                            responsesResult.content(),
                            new TypeReference<Map<String, Object>>() {
                            }
                    );
                } catch (Exception exception) {
                    throw new InvalidProviderResponseException("Responses provider returned invalid structured JSON", exception);
                }
                if (structuredData == null || structuredData.isEmpty()) {
                    throw new InvalidProviderResponseException("Responses provider returned an empty JSON object");
                }
                StructuredChatResponse structuredResponse = new StructuredChatResponse(
                        provider,
                        responsesResult.model(),
                        responsesResult.content(),
                        structuredData,
                        responsesResult.finishReason(),
                        responsesResult.providerRequestId(),
                        responsesResult.usage()
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
            }
            ProviderStructuredChatResult providerResult = resilientAiExecutor.execute(
                    runtime,
                    "chat",
                    () -> {
                        ProviderChatResult validated = validateChatResponse(
                                runtime.chatModel().call(toStructuredPrompt(runtime, request, model)),
                                model
                        );
                        try {
                            Map<String, Object> structuredData = objectMapper.readValue(
                                    validated.content(),
                                    new TypeReference<Map<String, Object>>() {
                                    }
                            );
                            if (structuredData == null || structuredData.isEmpty()) {
                                throw new InvalidProviderResponseException("Structured chat provider returned an empty JSON object");
                            }
                            return new ProviderStructuredChatResult(
                                    validated.response(),
                                    validated.content(),
                                    structuredData
                            );
                        } catch (InvalidProviderResponseException exception) {
                            throw exception;
                        } catch (Exception exception) {
                            throw new InvalidProviderResponseException(
                                    "Structured chat provider returned invalid JSON",
                                    exception
                            );
                        }
                    }
            );
            org.springframework.ai.chat.model.ChatResponse response = providerResult.response();
            StructuredChatResponse structuredResponse = new StructuredChatResponse(
                    provider,
                    model,
                    providerResult.content(),
                    providerResult.structuredData(),
                    finishReason(response),
                    providerRequestId(response),
                    response.getMetadata() == null ? null : toUsage(response.getMetadata().getUsage())
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

    private ResponsesResult callResponses(
            AiProviderRuntime runtime,
            List<ChatMessage> messages,
            String model,
            Integer maxOutputTokens,
            Double temperature,
            String reasoningEffort,
            String schemaName,
            Boolean strict,
            Map<String, Object> schema
    ) {
        return resilientAiExecutor.execute(runtime, "responses", () -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", messages.stream()
                    .map(message -> Map.of("role", message.role(), "content", message.content()))
                    .toList());
            payload.put("store", false);
            if (maxOutputTokens != null) {
                payload.put("max_output_tokens", maxOutputTokens);
            }
            if (temperature != null) {
                payload.put("temperature", temperature);
            }
            Map<String, Object> reasoning = new LinkedHashMap<>();
            reasoning.put("effort", reasoningEffort == null || reasoningEffort.isBlank() ? "high" : reasoningEffort);
            payload.put("reasoning", reasoning);
            if (schema != null && !schema.isEmpty()) {
                payload.put("text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", schemaName,
                                "strict", strict == null || strict,
                                "schema", schema
                        )
                ));
            }

            JsonNode response = runtime.chatRestClient().post()
                    .uri(responsesEndpoint(runtime))
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new InvalidProviderResponseException("Responses provider returned no response");
            }
            String status = response.path("status").asText();
            if (!"completed".equals(status)) {
                String reason = response.path("error").path("message").asText();
                if (reason.isBlank()) {
                    reason = response.path("incomplete_details").path("reason").asText();
                }
                throw new InvalidProviderResponseException(
                        reason.isBlank()
                                ? "Responses provider returned status " + status
                                : "Responses provider returned status " + status + ": " + reason
                );
            }
            String responseModel = response.path("model").asText();
            String content = extractResponseText(response);
            if (content.isBlank()) {
                throw new InvalidProviderResponseException("Responses provider returned empty content");
            }
            JsonNode usageNode = response.path("usage");
            TokenUsage usage = usageNode.isMissingNode() || usageNode.isNull()
                    ? null
                    : new TokenUsage(
                            nullableInteger(usageNode.get("input_tokens")),
                            nullableInteger(usageNode.get("output_tokens")),
                            nullableInteger(usageNode.get("total_tokens"))
                    );
            String responseId = response.path("id").asText();
            return new ResponsesResult(
                    responseModel.isBlank() ? model : responseModel,
                    content,
                    status,
                    responseId.isBlank() ? requestContextHolder.getRequestId() : responseId,
                    usage
            );
        });
    }

    private URI responsesEndpoint(AiProviderRuntime runtime) {
        String baseUrl = defaultChat(runtime).baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Responses baseUrl must not be blank");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/responses")) {
            return URI.create(normalized);
        }
        return URI.create(normalized + "/responses");
    }

    private String extractResponseText(JsonNode response) {
        JsonNode directOutputText = response.get("output_text");
        if (directOutputText != null && directOutputText.isTextual()) {
            return directOutputText.asText();
        }
        StringBuilder content = new StringBuilder();
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return "";
        }
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            JsonNode contentItems = item.path("content");
            if (!contentItems.isArray()) {
                continue;
            }
            for (JsonNode contentItem : contentItems) {
                String type = contentItem.path("type").asText();
                if ("refusal".equals(type)) {
                    throw new InvalidProviderResponseException(
                            "Responses provider refused the request: " + contentItem.path("refusal").asText()
                    );
                }
                if ("output_text".equals(type) && contentItem.path("text").isTextual()) {
                    content.append(contentItem.path("text").asText());
                }
            }
        }
        return content.toString();
    }

    private Integer nullableInteger(JsonNode node) {
        return node != null && node.isNumber() ? node.intValue() : null;
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

    private ProviderChatResult validateChatResponse(
            org.springframework.ai.chat.model.ChatResponse response,
            String requestedModel
    ) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new InvalidProviderResponseException("Chat provider returned no result");
        }
        String content = response.getResult().getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new InvalidProviderResponseException("Chat provider returned empty content");
        }
        String responseModel = response.getMetadata() == null ? null : response.getMetadata().getModel();
        if (responseModel != null && !responseModel.isBlank() && !requestedModel.equals(responseModel)) {
            throw new InvalidProviderResponseException(
                    "Chat provider returned model %s but %s was requested".formatted(responseModel, requestedModel)
            );
        }
        return new ProviderChatResult(response, content);
    }

    private String finishReason(org.springframework.ai.chat.model.ChatResponse response) {
        return response.getResult().getMetadata() == null
                ? null
                : normalizeFinishReason(response.getResult().getMetadata().getFinishReason());
    }

    private String providerRequestId(org.springframework.ai.chat.model.ChatResponse response) {
        return response.getMetadata() != null && response.getMetadata().getId() != null
                ? response.getMetadata().getId()
                : requestContextHolder.getRequestId();
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

    private record ProviderChatResult(
            org.springframework.ai.chat.model.ChatResponse response,
            String content
    ) {
    }

    private record ProviderStructuredChatResult(
            org.springframework.ai.chat.model.ChatResponse response,
            String content,
            Map<String, Object> structuredData
    ) {
    }

    private record ResponsesResult(
            String model,
            String content,
            String finishReason,
            String providerRequestId,
            TokenUsage usage
    ) {
    }
}
