package com.huashi.eftransfer.ai.modules.rag.integration;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class AppServerKnowledgeClient {

    private static final ParameterizedTypeReference<ApiResponse<LexicalKnowledgeExportPageResponse>> EXPORT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final AiRuntimeConfigService runtimeConfigService;

    public AppServerKnowledgeClient(AiRuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    public LexicalKnowledgeExportPageResponse exportLexicalPairs(
            OffsetDateTime updatedSince,
            String cursor,
            int limit,
            List<String> sourceIds
    ) {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        ApiResponse<LexicalKnowledgeExportPageResponse> response = bundle.appServerRestClient().get()
                .uri(uriBuilder -> buildExportUri(uriBuilder, updatedSince, cursor, limit, sourceIds))
                .headers(headers -> {
                    if (StringUtils.hasText(bundle.config().rag().appServer().internalToken())) {
                        headers.set("X-Internal-Token", bundle.config().rag().appServer().internalToken());
                    }
                })
                .retrieve()
                .body(EXPORT_TYPE);

        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("Unexpected app-server lexical knowledge export response");
        }
        return response.data();
    }

    private URI buildExportUri(
            UriBuilder uriBuilder,
            OffsetDateTime updatedSince,
            String cursor,
            int limit,
            List<String> sourceIds
    ) {
        UriBuilder builder = uriBuilder.path("/internal/knowledge/lexical-pairs/export")
                .queryParam("limit", limit);
        if (updatedSince != null) {
            builder.queryParam("updatedSince", updatedSince);
        }
        if (StringUtils.hasText(cursor)) {
            builder.queryParam("cursor", cursor);
        }
        if (sourceIds != null && !sourceIds.isEmpty()) {
            builder.queryParam("ids", sourceIds.toArray());
        }
        return builder.build();
    }
}
