package com.huashi.eftransfer.app.integration.ai.client;

import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiGatewayClientTest {

    private static HttpServer server;
    private static String baseUrl;
    private static final AtomicReference<StubResponse> NEXT_RESPONSE = new AtomicReference<>();
    private static final AtomicReference<CapturedRequest> LAST_REQUEST = new AtomicReference<>();

    private AiGatewayClient aiGatewayClient;

    @BeforeAll
    static void beforeAll() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", AiGatewayClientTest::handle);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        NEXT_RESPONSE.set(null);
        LAST_REQUEST.set(null);
        aiGatewayClient = new AiGatewayClient(RestClient.builder().baseUrl(baseUrl).build());
    }

    @Test
    void shouldFetchHealthPayload() {
        NEXT_RESPONSE.set(StubResponse.ok("""
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Request succeeded",
                  "data": {
                    "service": "ai-gateway",
                    "status": "UP",
                    "provider": "qwen",
                    "fallbackProvider": "deepseek",
                    "chatModel": "qwen-max",
                    "embeddingModel": "text-embedding-v4",
                    "rerankModel": "gte-rerank-v2",
                    "databaseReady": true,
                    "vectorStoreReady": true,
                    "providerReady": true,
                    "rerankReady": true,
                    "vectorExtensionVersion": "0.8.2",
                    "activeProfiles": ["local"],
                    "checkedAt": "2026-03-20T00:00:00Z"
                  },
                  "timestamp": "2026-03-20T00:00:00Z",
                  "traceId": "trace-health-client"
                }
                """));

        Optional<AiGatewayHealthResponse> response = aiGatewayClient.fetchHealth();

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().provider()).isEqualTo("qwen");
        assertThat(LAST_REQUEST.get().path()).isEqualTo("/internal/ai/health");
        assertThat(LAST_REQUEST.get().method()).isEqualTo("GET");
    }

    @Test
    void shouldPostChatRequestAndDeserializeResponse() {
        NEXT_RESPONSE.set(StubResponse.ok("""
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Request succeeded",
                  "data": {
                    "provider": "qwen",
                    "model": "qwen-max",
                    "content": "hello",
                    "finishReason": "stop",
                    "providerRequestId": "chat-1",
                    "usage": {
                      "promptTokens": 4,
                      "completionTokens": 2,
                      "totalTokens": 6
                    }
                  },
                  "timestamp": "2026-03-20T00:00:00Z",
                  "traceId": "trace-chat-client"
                }
                """));

        ChatResponse response = aiGatewayClient.chat(new ChatRequest(
                List.of(new ChatMessage("user", "Say hello")),
                null,
                0.2D,
                256
        ));

        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.usage().totalTokens()).isEqualTo(6);
        assertThat(LAST_REQUEST.get().path()).isEqualTo("/internal/ai/chat");
        assertThat(LAST_REQUEST.get().body()).contains("\"messages\"");
        assertThat(LAST_REQUEST.get().body()).contains("\"Say hello\"");
    }

    @Test
    void shouldPostStructuredChatRequestAndDeserializeStructuredMap() {
        NEXT_RESPONSE.set(StubResponse.ok("""
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Request succeeded",
                  "data": {
                    "provider": "qwen",
                    "model": "qwen-max",
                    "rawContent": "{\\"summary\\":\\"ok\\",\\"score\\":95}",
                    "structuredData": {
                      "summary": "ok",
                      "score": 95
                    },
                    "finishReason": "stop",
                    "providerRequestId": "structured-1",
                    "usage": {
                      "promptTokens": 5,
                      "completionTokens": 3,
                      "totalTokens": 8
                    }
                  },
                  "timestamp": "2026-03-20T00:00:00Z",
                  "traceId": "trace-structured-client"
                }
                """));

        StructuredChatResponse response = aiGatewayClient.structuredChat(new StructuredChatRequest(
                List.of(new ChatMessage("user", "Return JSON")),
                null,
                0.1D,
                "ResultSchema",
                true,
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "summary", Map.of("type", "string"),
                                "score", Map.of("type", "integer")
                        ),
                        "required", List.of("summary", "score")
                )
        ));

        assertThat(response.structuredData()).containsEntry("summary", "ok");
        assertThat(response.structuredData()).containsEntry("score", 95);
        assertThat(LAST_REQUEST.get().path()).isEqualTo("/internal/ai/chat/structured");
        assertThat(LAST_REQUEST.get().body()).contains("\"schemaName\":\"ResultSchema\"");
    }

    @Test
    void shouldPostEmbeddingRequests() {
        NEXT_RESPONSE.set(StubResponse.ok("""
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Request succeeded",
                  "data": {
                    "provider": "qwen",
                    "model": "text-embedding-v4",
                    "dimension": 3,
                    "providerRequestId": "embed-1",
                    "usage": {
                      "promptTokens": 2,
                      "completionTokens": 0,
                      "totalTokens": 2
                    },
                    "items": [
                      {"index": 0, "text": "alpha", "embedding": [0.1, 0.2, 0.3]}
                    ]
                  },
                  "timestamp": "2026-03-20T00:00:00Z",
                  "traceId": "trace-embed-client"
                }
                """));

        EmbeddingResponse single = aiGatewayClient.embed(new EmbeddingRequest("alpha", null, 3));

        assertThat(single.items()).hasSize(1);
        assertThat(LAST_REQUEST.get().path()).isEqualTo("/internal/ai/embed");
        assertThat(LAST_REQUEST.get().body()).contains("\"text\":\"alpha\"");

        NEXT_RESPONSE.set(StubResponse.ok("""
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Request succeeded",
                  "data": {
                    "provider": "qwen",
                    "model": "text-embedding-v4",
                    "dimension": 3,
                    "usage": {
                      "promptTokens": 4,
                      "completionTokens": 0,
                      "totalTokens": 4
                    },
                    "items": [
                      {"index": 0, "text": "alpha", "embedding": [0.1, 0.2, 0.3]},
                      {"index": 1, "text": "beta", "embedding": [0.4, 0.5, 0.6]}
                    ]
                  },
                  "timestamp": "2026-03-20T00:00:00Z",
                  "traceId": "trace-embed-batch-client"
                }
                """));

        EmbeddingResponse batch = aiGatewayClient.embedBatch(new EmbeddingBatchRequest(List.of("alpha", "beta"), null, 3));

        assertThat(batch.items()).hasSize(2);
        assertThat(batch.items().get(1).text()).isEqualTo("beta");
        assertThat(LAST_REQUEST.get().path()).isEqualTo("/internal/ai/embed/batch");
        assertThat(LAST_REQUEST.get().body()).contains("\"texts\":[\"alpha\",\"beta\"]");
    }

    @Test
    void shouldPostRerankRequestAndDeserializeResponse() {
        NEXT_RESPONSE.set(StubResponse.ok("""
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Request succeeded",
                  "data": {
                    "provider": "qwen",
                    "model": "gte-rerank-v2",
                    "providerRequestId": "rerank-1",
                    "totalTokens": 9,
                    "items": [
                      {"index": 1, "relevanceScore": 0.88, "document": "doc-b"},
                      {"index": 0, "relevanceScore": 0.21, "document": "doc-a"}
                    ]
                  },
                  "timestamp": "2026-03-20T00:00:00Z",
                  "traceId": "trace-rerank-client"
                }
                """));

        RerankResponse response = aiGatewayClient.rerank(new RerankRequest(
                null,
                "hello",
                List.of("doc-a", "doc-b"),
                2,
                true,
                null
        ));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).index()).isEqualTo(1);
        assertThat(LAST_REQUEST.get().path()).isEqualTo("/internal/ai/rerank");
        assertThat(LAST_REQUEST.get().body()).contains("\"query\":\"hello\"");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        LAST_REQUEST.set(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                new String(requestBytes, StandardCharsets.UTF_8)
        ));

        StubResponse response = Objects.requireNonNullElseGet(
                NEXT_RESPONSE.get(),
                () -> new StubResponse(500, "{\"success\":false}")
        );
        byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(response.status(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private record StubResponse(int status, String body) {

        private static StubResponse ok(String body) {
            return new StubResponse(200, body);
        }
    }

    private record CapturedRequest(String method, String path, String body) {
    }
}
