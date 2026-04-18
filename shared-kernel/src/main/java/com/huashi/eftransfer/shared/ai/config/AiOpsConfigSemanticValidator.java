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
            issues.add(new AiOpsConfigIssue("config", "config is required"));
            return issues;
        }
        if (payload.provider() == null) {
            issues.add(new AiOpsConfigIssue("provider", "provider section is required"));
        }
        if (payload.resilience() == null) {
            issues.add(new AiOpsConfigIssue("resilience", "resilience section is required"));
        }
        if (payload.rag() == null) {
            issues.add(new AiOpsConfigIssue("rag", "rag section is required"));
        }
        if (!issues.isEmpty()) {
            return issues;
        }

        validateProvider(payload.provider(), issues);
        validateResilience(payload.resilience(), issues);
        validateRag(payload.rag(), issues);
        return issues;
    }

    private static void validateProvider(AiOpsProviderConfig provider, List<AiOpsConfigIssue> issues) {
        if (provider.providers() == null || provider.providers().isEmpty()) {
            issues.add(new AiOpsConfigIssue("provider.providers", "at least one provider definition is required"));
            return;
        }
        validateProviderReference("provider.activeProvider", provider.activeProvider(), provider.providers(), issues);
        validateProviderReference("provider.fallbackProvider", provider.fallbackProvider(), provider.providers(), issues);
        if (hasText(provider.activeProvider())
                && provider.activeProvider().equalsIgnoreCase(provider.fallbackProvider())) {
            issues.add(new AiOpsConfigIssue(
                    "provider.fallbackProvider",
                    "fallbackProvider must be different from activeProvider"
            ));
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : provider.providers().entrySet()) {
            validateProviderKey(entry.getKey(), issues);
            validateProviderDefinition(entry.getKey(), entry.getValue(), issues);
        }
    }

    private static void validateResilience(AiOpsResilienceConfig resilience, List<AiOpsConfigIssue> issues) {
        validateDuration("resilience.waitDuration", resilience.waitDuration(), issues);
        validateDuration("resilience.openStateDuration", resilience.openStateDuration(), issues);
    }

    private static void validateRag(AiOpsRagConfig rag, List<AiOpsConfigIssue> issues) {
        if (rag.appServer() == null) {
            issues.add(new AiOpsConfigIssue("rag.appServer", "appServer section is required"));
        } else {
            validateUrl("rag.appServer.baseUrl", rag.appServer().baseUrl(), issues);
            requireText("rag.appServer.internalToken", rag.appServer().internalToken(), issues);
            validateDuration("rag.appServer.connectTimeout", rag.appServer().connectTimeout(), issues);
            validateDuration("rag.appServer.readTimeout", rag.appServer().readTimeout(), issues);
        }

        if (rag.ingestion() == null) {
            issues.add(new AiOpsConfigIssue("rag.ingestion", "ingestion section is required"));
        }

        if (rag.retrieval() == null) {
            issues.add(new AiOpsConfigIssue("rag.retrieval", "retrieval section is required"));
            return;
        }

        if (rag.retrieval().rerankTopN() != null
                && rag.retrieval().recallTopK() != null
                && rag.retrieval().rerankTopN() > rag.retrieval().recallTopK()) {
            issues.add(new AiOpsConfigIssue(
                    "rag.retrieval.rerankTopN",
                    "rerankTopN must be less than or equal to recallTopK"
            ));
        }
        if (rag.retrieval().finalTopK() != null
                && rag.retrieval().rerankTopN() != null
                && rag.retrieval().finalTopK() > rag.retrieval().rerankTopN()) {
            issues.add(new AiOpsConfigIssue(
                    "rag.retrieval.finalTopK",
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
            issues.add(new AiOpsConfigIssue(prefix, "provider definition is required"));
            return;
        }
        if (definition.chat() == null) {
            issues.add(new AiOpsConfigIssue(prefix + ".chat", "chat section is required"));
        } else {
            validateUrl(prefix + ".chat.baseUrl", definition.chat().baseUrl(), issues);
            requireText(prefix + ".chat.apiKey", definition.chat().apiKey(), issues);
            validateDuration(prefix + ".chat.timeout", definition.chat().timeout(), issues);
        }
        if (definition.embedding() == null) {
            issues.add(new AiOpsConfigIssue(prefix + ".embedding", "embedding section is required"));
        } else {
            validateUrl(prefix + ".embedding.baseUrl", definition.embedding().baseUrl(), issues);
            requireText(prefix + ".embedding.apiKey", definition.embedding().apiKey(), issues);
            validateDuration(prefix + ".embedding.timeout", definition.embedding().timeout(), issues);
            if (definition.embedding().dimension() != null && definition.embedding().dimension() != 1024) {
                issues.add(new AiOpsConfigIssue(
                        prefix + ".embedding.dimension",
                        "Current pgvector schema is fixed at 1024 dimensions"
                ));
            }
        }
        if (definition.rerank() == null) {
            issues.add(new AiOpsConfigIssue(prefix + ".rerank", "rerank section is required"));
        } else {
            validateUrl(prefix + ".rerank.baseUrl", definition.rerank().baseUrl(), issues);
            requireText(prefix + ".rerank.apiKey", definition.rerank().apiKey(), issues);
            validateDuration(prefix + ".rerank.timeout", definition.rerank().timeout(), issues);
        }
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
            issues.add(new AiOpsConfigIssue(field, "must reference a configured provider"));
        }
    }

    private static void validateProviderKey(String providerName, List<AiOpsConfigIssue> issues) {
        if (!hasText(providerName)) {
            issues.add(new AiOpsConfigIssue("provider.providers", "provider key must not be blank"));
            return;
        }
        if (!PROVIDER_KEY_PATTERN.matcher(providerName).matches()) {
            issues.add(new AiOpsConfigIssue(
                    "provider.providers." + providerName,
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
                issues.add(new AiOpsConfigIssue(field, "must be an absolute URL"));
            }
        } catch (Exception ex) {
            issues.add(new AiOpsConfigIssue(field, "must be a valid URL"));
        }
    }

    private static void validateDuration(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!hasText(value)) {
            return;
        }
        try {
            if (AiOpsFlexibleDurationParser.parse(value).isNegative() || AiOpsFlexibleDurationParser.parse(value).isZero()) {
                issues.add(new AiOpsConfigIssue(field, "must be a positive duration"));
            }
        } catch (Exception ex) {
            issues.add(new AiOpsConfigIssue(field, "must be a valid duration"));
        }
    }

    private static void requireText(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!hasText(value)) {
            issues.add(new AiOpsConfigIssue(field, "value is required"));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
