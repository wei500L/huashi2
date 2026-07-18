package com.huashi.eftransfer.shared.ai.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class AiOpsConfigSemanticValidator {

    private static final Pattern PROVIDER_KEY_PATTERN = Pattern.compile("^[a-z0-9_-]+$");

    private AiOpsConfigSemanticValidator() {
    }

    public static List<AiOpsConfigIssue> validate(AiOpsConfigPayload payload) {
        List<AiOpsConfigIssue> issues = new ArrayList<>();
        if (payload == null) {
            issues.add(issue("config", "config_required", "config is required"));
            return issues;
        }
        if (payload.provider() == null) {
            issues.add(issue("provider", "provider_section_required", "provider section is required"));
        }
        if (payload.resilience() == null) {
            issues.add(issue("resilience", "resilience_section_required", "resilience section is required"));
        }
        if (payload.rag() == null) {
            issues.add(issue("rag", "rag_section_required", "rag section is required"));
        }
        if (!issues.isEmpty()) {
            return issues;
        }

        validateProvider(payload.provider(), issues);
        validateResilience(payload.resilience(), issues);
        validateRag(payload.rag(), issues);
        return issues;
    }

    public static AiOpsConfigIssue issueFromViolation(String field, String message) {
        if ("protocol is required".equals(message)) {
            return issue(field, "protocol_required", message);
        }
        if (message != null && message.endsWith("is required")) {
            return issue(field, "value_required", message);
        }
        if ("temperature must be between 0 and 2".equals(message)) {
            return issue(field, "temperature_out_of_range", message);
        }
        if (message != null && message.contains("must be greater than 0")) {
            return issue(field, "must_be_greater_than_zero", message);
        }
        return issue(field, "invalid_value", message == null ? "invalid value" : message);
    }

    private static void validateProvider(AiOpsProviderConfig provider, List<AiOpsConfigIssue> issues) {
        if (provider.providers() == null || provider.providers().isEmpty()) {
            issues.add(issue("provider.providers", "provider_definitions_required", "at least one provider definition is required"));
            return;
        }
        if (provider.providers().size() < 2) {
            issues.add(issue(
                    "provider.providers",
                    "provider_count_requires_fallback",
                    "fallbackProvider requires at least two provider definitions"
            ));
        }
        validateProviderReference("provider.activeProvider", provider.activeProvider(), provider.providers(), issues);
        validateProviderReference("provider.fallbackProvider", provider.fallbackProvider(), provider.providers(), issues);
        if (hasText(provider.activeProvider())
                && provider.activeProvider().equalsIgnoreCase(provider.fallbackProvider())) {
            issues.add(issue(
                    "provider.fallbackProvider",
                    "fallback_provider_must_differ",
                    "fallbackProvider must be different from activeProvider"
            ));
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : provider.providers().entrySet()) {
            validateProviderKey(entry.getKey(), issues);
            validateProviderDefinition(entry.getKey(), entry.getValue(), issues);
        }
        validateEmbeddingDimensions(provider.providers(), issues);
        validateEmbeddingSpaces(provider.providers(), issues);
    }

    private static void validateResilience(AiOpsResilienceConfig resilience, List<AiOpsConfigIssue> issues) {
        validateDuration("resilience.waitDuration", resilience.waitDuration(), issues);
        validateDuration("resilience.openStateDuration", resilience.openStateDuration(), issues);
    }

    private static void validateRag(AiOpsRagConfig rag, List<AiOpsConfigIssue> issues) {
        if (rag.appServer() == null) {
            issues.add(issue("rag.appServer", "app_server_section_required", "appServer section is required"));
        } else {
            validateUrl("rag.appServer.baseUrl", rag.appServer().baseUrl(), issues);
            requireText("rag.appServer.internalToken", rag.appServer().internalToken(), issues);
            validateDuration("rag.appServer.connectTimeout", rag.appServer().connectTimeout(), issues);
            validateDuration("rag.appServer.readTimeout", rag.appServer().readTimeout(), issues);
        }

        if (rag.ingestion() == null) {
            issues.add(issue("rag.ingestion", "ingestion_section_required", "ingestion section is required"));
        }

        if (rag.retrieval() == null) {
            issues.add(issue("rag.retrieval", "retrieval_section_required", "retrieval section is required"));
            return;
        }

        if (rag.retrieval().rerankTopN() != null
                && rag.retrieval().recallTopK() != null
                && rag.retrieval().rerankTopN() > rag.retrieval().recallTopK()) {
            issues.add(issue(
                    "rag.retrieval.rerankTopN",
                    "rerank_top_n_exceeds_recall_top_k",
                    "rerankTopN must be less than or equal to recallTopK"
            ));
        }
        if (rag.retrieval().finalTopK() != null
                && rag.retrieval().rerankTopN() != null
                && rag.retrieval().finalTopK() > rag.retrieval().rerankTopN()) {
            issues.add(issue(
                    "rag.retrieval.finalTopK",
                    "final_top_k_exceeds_rerank_top_n",
                    "finalTopK must be less than or equal to rerankTopN"
            ));
        }
    }

    private static void validateProviderDefinition(
            String providerName,
            AiOpsProviderDefinition definition,
            List<AiOpsConfigIssue> issues
    ) {
        String prefix = "provider.providers." + providerName;
        if (definition == null) {
            issues.add(issue(prefix, "provider_definition_required", "provider definition is required"));
            return;
        }
        if (definition.chat() == null) {
            issues.add(issue(prefix + ".chat", "chat_section_required", "chat section is required"));
        } else {
            validateProtocol(prefix + ".chat.protocol", definition.chat().protocol(), AiOpsProtocols.OPENAI_COMPAT, issues);
            validateUrl(prefix + ".chat.baseUrl", definition.chat().baseUrl(), issues);
            requireText(prefix + ".chat.apiKey", definition.chat().apiKey(), issues);
            validateDuration(prefix + ".chat.connectTimeout", definition.chat().connectTimeout(), issues);
            validateDuration(prefix + ".chat.readTimeout", definition.chat().readTimeout(), issues);
        }
        if (definition.embedding() == null) {
            issues.add(issue(prefix + ".embedding", "embedding_section_required", "embedding section is required"));
        } else {
            validateProtocol(prefix + ".embedding.protocol", definition.embedding().protocol(), AiOpsProtocols.OPENAI_COMPAT, issues);
            validateUrl(prefix + ".embedding.baseUrl", definition.embedding().baseUrl(), issues);
            requireText(prefix + ".embedding.apiKey", definition.embedding().apiKey(), issues);
            validateDuration(prefix + ".embedding.connectTimeout", definition.embedding().connectTimeout(), issues);
            validateDuration(prefix + ".embedding.readTimeout", definition.embedding().readTimeout(), issues);
        }
        if (definition.rerank() == null) {
            issues.add(issue(prefix + ".rerank", "rerank_section_required", "rerank section is required"));
        } else {
            validateRerankProtocol(prefix + ".rerank.protocol", definition.rerank().protocol(), issues);
            validateUrl(prefix + ".rerank.baseUrl", definition.rerank().baseUrl(), issues);
            requireText(prefix + ".rerank.apiKey", definition.rerank().apiKey(), issues);
            validateDuration(prefix + ".rerank.connectTimeout", definition.rerank().connectTimeout(), issues);
            validateDuration(prefix + ".rerank.readTimeout", definition.rerank().readTimeout(), issues);
        }
    }

    private static void validateEmbeddingDimensions(
            Map<String, AiOpsProviderDefinition> providers,
            List<AiOpsConfigIssue> issues
    ) {
        Integer expectedDimension = null;
        for (Map.Entry<String, AiOpsProviderDefinition> entry : providers.entrySet()) {
            AiOpsProviderDefinition definition = entry.getValue();
            Integer dimension = definition == null || definition.embedding() == null ? null : definition.embedding().dimension();
            if (dimension == null) {
                continue;
            }
            if (expectedDimension == null) {
                expectedDimension = dimension;
                continue;
            }
            if (!expectedDimension.equals(dimension)) {
                issues.add(issue(
                        "provider.providers." + entry.getKey() + ".embedding.dimension",
                        "embedding_dimension_mismatch",
                        "All provider embedding dimensions must match",
                        Map.of("expected", expectedDimension, "actual", dimension)
                ));
            }
        }
    }

    private static void validateEmbeddingSpaces(
            Map<String, AiOpsProviderDefinition> providers,
            List<AiOpsConfigIssue> issues
    ) {
        String expectedModel = null;
        String expectedMultimodalModel = null;
        for (Map.Entry<String, AiOpsProviderDefinition> entry : providers.entrySet()) {
            AiOpsProviderDefinition definition = entry.getValue();
            AiOpsEmbeddingConfig embedding = definition == null ? null : definition.embedding();
            if (embedding == null) {
                continue;
            }
            if (expectedModel == null) {
                expectedModel = embedding.model();
                expectedMultimodalModel = embedding.multimodalModel();
                continue;
            }
            if (!java.util.Objects.equals(expectedModel, embedding.model())) {
                issues.add(issue(
                        "provider.providers." + entry.getKey() + ".embedding.model",
                        "embedding_space_model_mismatch",
                        "All failover providers must use the same embedding model",
                        Map.of("expected", expectedModel, "actual", String.valueOf(embedding.model()))
                ));
            }
            if (!java.util.Objects.equals(expectedMultimodalModel, embedding.multimodalModel())) {
                issues.add(issue(
                        "provider.providers." + entry.getKey() + ".embedding.multimodalModel",
                        "embedding_space_multimodal_model_mismatch",
                        "All failover providers must use the same multimodal embedding model"
                ));
            }
        }
    }

    private static void validateProtocol(String field, String actual, String expected, List<AiOpsConfigIssue> issues) {
        if (!hasText(actual)) {
            return;
        }
        if (!expected.equals(actual)) {
            issues.add(issue(
                    field,
                    "unsupported_protocol",
                    "Unsupported protocol '" + actual + "'; expected " + expected,
                    Map.of("actual", actual, "expected", expected)
            ));
        }
    }

    private static void validateRerankProtocol(String field, String actual, List<AiOpsConfigIssue> issues) {
        if (!hasText(actual)) {
            return;
        }
        if (AiOpsProtocols.OPENAI_RERANK.equals(actual)
                || AiOpsProtocols.OPENAI_CHAT_RERANK.equals(actual)
                || AiOpsProtocols.OPENAI_COMPAT.equals(actual)) {
            return;
        }
        issues.add(issue(
                field,
                "unsupported_protocol",
                "Unsupported protocol '" + actual + "'; expected one of openai-rerank, openai-chat-rerank",
                Map.of("actual", actual, "expected", List.of(AiOpsProtocols.OPENAI_RERANK, AiOpsProtocols.OPENAI_CHAT_RERANK))
        ));
    }

    private static void validateProviderReference(
            String field,
            String providerName,
            Map<String, AiOpsProviderDefinition> providers,
            List<AiOpsConfigIssue> issues
    ) {
        if (!hasText(providerName) || providers == null) {
            return;
        }
        if (!providers.containsKey(providerName)) {
            issues.add(issue(field, "provider_reference_missing", "must reference a configured provider"));
        }
    }

    private static void validateProviderKey(String providerName, List<AiOpsConfigIssue> issues) {
        if (!hasText(providerName)) {
            issues.add(issue("provider.providers", "provider_key_required", "provider key must not be blank"));
            return;
        }
        if (!PROVIDER_KEY_PATTERN.matcher(providerName).matches()) {
            issues.add(issue(
                    "provider.providers." + providerName,
                    "provider_key_invalid",
                    "provider key must contain only lowercase letters, numbers, hyphen, or underscore"
            ));
        }
    }

    private static void validateUrl(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!hasText(value)) {
            return;
        }
        try {
            URI uri = URI.create(value);
            if (!hasText(uri.getScheme()) || !hasText(uri.getHost())) {
                issues.add(issue(field, "absolute_url_required", "must be an absolute URL"));
            }
        } catch (Exception ex) {
            issues.add(issue(field, "invalid_url", "must be a valid URL"));
        }
    }

    private static void validateDuration(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!hasText(value)) {
            return;
        }
        try {
            if (AiOpsFlexibleDurationParser.parse(value).isNegative() || AiOpsFlexibleDurationParser.parse(value).isZero()) {
                issues.add(issue(field, "positive_duration_required", "must be a positive duration"));
            }
        } catch (Exception ex) {
            issues.add(issue(field, "invalid_duration", "must be a valid duration"));
        }
    }

    private static void requireText(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!hasText(value)) {
            issues.add(issue(field, "value_required", "value is required"));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static AiOpsConfigIssue issue(String field, String code, String defaultMessage) {
        return new AiOpsConfigIssue(field, code, defaultMessage, Map.of());
    }

    private static AiOpsConfigIssue issue(String field, String code, String defaultMessage, Map<String, Object> args) {
        return new AiOpsConfigIssue(field, code, defaultMessage, args);
    }
}
