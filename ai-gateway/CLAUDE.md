[根目录](../CLAUDE.md) > **ai-gateway (AI 网关)**

# ai-gateway -- AI / RAG 网关

## 模块职责

AI 能力中枢，封装 LLM Provider（Qwen/DeepSeek）集成、向量嵌入、RAG 检索与 Rerank、知识摄取（Ingestion）与同步、运行时配置管理。独立运行于 PostgreSQL + pgvector 之上。

## 入口与启动

- **Main class**: `com.huashi.eftransfer.ai.AiGatewayApplication`
- **端口**: 8090 (默认)
- **启动命令**: `./mvnw -pl ai-gateway -am spring-boot:run`
- **Profile**: `local` / `dev` / `prod`

## 对外接口

所有接口均为内部接口，需要 `X-Internal-Token` 请求头。

| Controller | 路径前缀 | 说明 |
|-----------|---------|------|
| `AiHealthController` | `/internal/ai/health` | AI 服务健康检查（Provider / DB / Vector Store 状态） |
| `InternalAiController` | `/internal/ai` | Chat / Embedding / Structured Chat / Rerank 统一调用 |
| `InternalAiConfigController` | `/internal/ai/config` | 运行时配置查询、验证、热更新 |
| `InternalRagController` | `/internal/ai/rag` | RAG 检索、重建索引、Ingestion 任务管理 |

## 关键依赖与配置

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.3 | Web + Validation + Actuator + JDBC + AMQP + Flyway |
| Spring AI | 1.1.3 | OpenAI-compatible model starter + PgVector store + Vector advisor |
| Resilience4j | 2.3.0 | 熔断器 + 重试（AI Provider 调用保护） |
| PostgreSQL | runtime | 关系存储 + pgvector 向量存储 |
| Spring Retry | - | 重试机制 |
| Micrometer Prometheus | - | 监控指标 |
| shared-kernel | 内部 | 共享枚举、DTO、事件 |

测试依赖：WireMock 3.13 (外部 API 模拟), Testcontainers 1.21 (PostgreSQL)

配置文件：
- `src/main/resources/application.yml` -- 主配置（含 AI Provider、Resilience、RAG 参数）
- `src/main/resources/application-{local,dev,prod}.yml` -- 环境覆盖
- `src/main/resources/logback-spring.xml` -- 日志（含 traceId）

### AI Provider 配置结构

```
ai.provider:
  active-provider: qwen          # 主 Provider
  fallback-provider: deepseek    # 降级 Provider
  providers.{name}:
    chat: {base-url, api-key, model, timeout, temperature, max-tokens}
    embedding: {base-url, api-key, model, timeout, dimension}
    rerank: {base-url, api-key, model, timeout}
ai.resilience:
  max-attempts, wait-duration, failure-rate-threshold, sliding-window-size, open-state-duration
rag:
  app-server: {base-url, internal-token, connect-timeout, read-timeout}
  ingestion: {export-page-size, embedding-batch-size}
  retrieval: {recall-top-k, recall-threshold, rerank-top-n, rerank-threshold, final-top-k}
```

## 数据模型

数据库：PostgreSQL + pgvector 扩展。Flyway 迁移脚本 V1-V5。

### 核心存储

| 组件 | 说明 |
|------|------|
| `KnowledgeStoreRepository` | 知识文档 + 向量存储（pgvector） |
| `IngestionJobRepository` | Ingestion 任务记录（状态追踪） |
| `IntegrationConsumeRecordRepository` | 消息消费去重记录 |
| `RagAdvisorVectorStore` | Spring AI VectorStore 适配 |

### 数据库迁移

| 版本 | 脚本 | 说明 |
|------|------|------|
| V1 | `V1__init_pgvector.sql` | pgvector 扩展初始化 |
| V2 | `V2__knowledge_rag_schema.sql` | 知识 RAG 模式 |
| V3 | `V3__optimize_lexical_rag_retrieval.sql` | 检索优化 |
| V4 | `V4__integration_consume_record.sql` | 消息消费记录 |
| V5 | `V5__drop_legacy_rag_knowledge_document.sql` | 删除旧版 RAG 文档表 |

## 核心架构

### Provider 集成层

```
AiProviderFacade (接口)
  └── QwenAiProviderFacade (实现)
        ├── QwenChatProviderClient
        ├── QwenEmbeddingProviderClient
        └── QwenRerankClient

AiProviderRegistry -- Provider 注册与查找
ResilientAiExecutor -- 封装 Resilience4j 熔断 + 重试
AiRuntimeBundle / AiRuntimeBundleFactory -- 运行时配置热加载
```

### RAG 流水线

```
Knowledge Ingestion:
  LexicalKnowledgeChangedEvent (RabbitMQ)
    -> LexicalKnowledgeChangedEventListener
    -> KnowledgeIngestionService (分页拉取 + 批量嵌入 + 向量存储)

Knowledge Retrieval:
  InternalRagController
    -> RagService
    -> KnowledgeSearchService (向量召回 + Rerank + TopK 截取)
    -> RagRetrievalCapture (结果记录)
```

### 知识同步流程

1. app-server 词汇变更 -> RabbitMQ `LexicalKnowledgeChangedEvent`
2. ai-gateway `LexicalKnowledgeChangedEventListener` 消费事件
3. `KnowledgeIngestionService` 从 app-server `/internal/knowledge` 拉取词汇数据
4. 批量嵌入 + 存储到 pgvector
5. `IntegrationConsumeRecordRepository` 去重确保幂等

## 测试与质量

| 测试类 | 类别 |
|--------|------|
| `AiHealthControllerTest` | 健康检查 |
| `InternalAiControllerIntegrationTest` | AI 统一调用 |
| `InternalAiConfigControllerTest` | 配置管理 |
| `InternalRagControllerTest` | RAG 接口 |
| `QwenProviderClientTest` | Qwen Chat/Embedding |
| `QwenRerankClientTest` | Qwen Rerank |
| `AiRuntimeConfigServiceTest` | 运行时配置 |
| `KnowledgeStoreRepositoryTest` | 向量存储 |
| `RagSchemaDimensionGuardTest` | 启动期 schema / 维度校验 |
| `LexicalRagFlowIntegrationTest` | RAG 端到端流程 |
| `LexicalKnowledgeChangedEventListenerTest` | 知识同步事件 |
| `AppServerKnowledgeClientTest` | app-server 客户端 |

共 17 个测试类。使用 WireMock 模拟外部 AI API，Testcontainers 启动 PostgreSQL。

## 常见问题 (FAQ)

- **Q: 如何切换 AI Provider？**
  A: 修改 `AI_PROVIDER` 环境变量，或通过 app-server 运维配置中心 API 热更新。

- **Q: RAG 检索参数如何调优？**
  A: 调整 `rag.retrieval` 下的 `recall-top-k`, `recall-threshold`, `rerank-top-n`, `rerank-threshold`, `final-top-k`。

- **Q: 知识重建索引（Reindex）如何触发？**
  A: 通过 app-server `/api/admin/ai-config/reindex` 端点或自动监听 RabbitMQ 事件。

## 相关文件清单

```
ai-gateway/
  pom.xml                                                     # Maven 配置
  src/main/java/com/huashi/eftransfer/ai/
    AiGatewayApplication.java                                 # Spring Boot 入口
    common/
      config/       AiProviderProperties, AiResilienceProperties, AiProviderConfiguration, InternalApiProperties
      exception/    GlobalExceptionHandler, ProviderCallException, ProviderErrorSupport
      filter/       TraceFilter, InternalApiAuthenticationFilter
      observability/ AiProviderObservationService, ResilientAiExecutor, ProviderRequestCaptureInterceptor
      runtime/      AiRuntimeBundle, AiRuntimeBundleFactory, AiRuntimeConfigService
    integration/
      provider/     AiProviderFacade, AiProviderRegistry, QwenAiProviderFacade, QwenChatProviderClient,
                    QwenEmbeddingProviderClient, QwenRerankClient, RerankClient
    modules/
      health/       AiHealthController, AiHealthService, AiHealthPayload
      internal/     InternalAiController, InternalAiConfigController, InternalAiService
      rag/
        config/     RagConfiguration, RagProperties, KnowledgeSyncRabbitConfig
        controller/ InternalRagController
        integration/ LexicalKnowledgeChangedEventListener, AppServerKnowledgeClient
        repository/ KnowledgeStoreRepository, IngestionJobRepository, IntegrationConsumeRecordRepository
        service/    RagService, KnowledgeSearchService, KnowledgeIngestionService, RagRetrievalCapture
        support/    KnowledgeDocumentPayload, KnowledgeChunkPayload, RagSearchFilter, RagRetrievedChunk, ...
        vector/     RagAdvisorVectorStore
  src/main/resources/
    application.yml                                            # 主配置
    application-{local,dev,prod}.yml                           # 环境配置
    logback-spring.xml                                         # 日志
    db/migration/V1-V5                                         # Flyway 迁移脚本
  src/test/                                                    # 17 个测试类
```

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-22 00:35:46 | 初始创建 | 全量扫描生成 |
| 2026-04-18 | 迁移安全加固 | 显式 HNSW 参数、固定 1024 维度并新增启动期 schema guard |
