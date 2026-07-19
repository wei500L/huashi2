# AI 质量优先架构

本项目采用“业务规则控制事实与评分、混合检索提供证据、大模型负责结构化解释、独立模型调用复核证据”的质量优先架构。

## 模型边界

- Embedding 固定使用 `Qwen/Qwen3-Embedding-8B`，维度固定为 1024。
- 不使用 `text-embedding-3-large`，也不维护第二套向量空间。
- Rerank 默认使用独立重排模型。
- Chat 支持 `openai-compat` 和 `openai-responses` 两种协议。
- `openai-responses` 请求默认关闭服务端存储，普通生成使用 high reasoning，教师干预场景启用 pro mode。

Embedding 的 active/fallback provider 必须使用相同模型和相同维度。更换 Embedding 模型后必须执行完整 RAG reindex。

生产环境把该空间标识为 `qwen3-embedding-8b@1024/v1`，管理端不能在线修改模型或维度。跨供应商 fallback 只有在双 provider 探针成功且对应向量余弦兼容度不低于 0.995 时才视为就绪。未来迁移空间必须通过维护版本修改策略与 schema，并在恢复流量前完成全量 reindex。

## 检索链路

单次查询执行以下流程：

1. 查询规划器生成不超过 4 条语义检索表达和不超过 8 个精确词汇检索词。
2. 所有语义表达通过 Qwen3 Embedding 批量向量化。
3. PostgreSQL 并行执行 pgvector 相似度召回和 `pg_trgm` 精确/模糊词汇召回。
4. 多路结果通过 Reciprocal Rank Fusion 合并。
5. exact title match 获得强提升，随后进入独立 reranker。
6. 最终上下文限制同一词对最多 3 个 chunk，防止单一词对挤占全部证据位置。

默认质量参数：

```env
RAG_RECALL_TOP_K=50
RAG_RECALL_THRESHOLD=0.45
RAG_RERANK_TOP_N=16
RAG_RERANK_THRESHOLD=0.2
RAG_FINAL_TOP_K=8
AI_HNSW_EF_SEARCH=128
```

## 生成与证据审查

训练推荐、诊断解释、教师干预不再调用 `ragAnswer` 生成中间文本，而是直接消费 `ragRetrieve` 返回的原始 citations 和 context chunks。

模型输出必须：

- 遵守 JSON Schema；
- 只能引用当前请求实际检索到的 citation ID；
- 在 explanation 或 teacherNote 中写出对应的 `[C1]` 行内引用；
- 只返回后端允许的词对 ID 和训练模式；
- 在证据不足时返回 uncertaintyNote。

生成完成后会发起独立证据审查请求。审查不通过、引用越界、结构非法或返回未经后端批准的词对/训练模式时，结果不会进入 AI 正常输出，而是走现有规则降级链路。

## 数据库初始化

混合检索增加了 PostgreSQL `pg_trgm` 扩展和两个 trigram GIN 索引。仓库不使用版本化迁移，结构变更后需要按既有开发流程清空并重建 AI PostgreSQL 数据库，再执行完整 RAG reindex。

## Responses API 配置

需要使用 Responses API 时，为每个 provider 独立配置协议、URL、Key 与模型。URL 可以是任意兼容服务地址；填写 `https://xxxx.com/v1` 时，网关请求 `https://xxxx.com/v1/responses`：

请求体与 `/responses` 路径遵循 [OpenAI Responses Create API](https://developers.openai.com/api/reference/resources/responses/methods/create/)，兼容服务需要实现同一契约。

```env
AI_CHAT_PROTOCOL=openai-responses
AI_CHAT_BASE_URL=https://xxxx.com/v1
AI_CHAT_API_KEY=replace-with-provider-api-key
AI_CHAT_MODEL=replace-with-responses-compatible-model
```

Embedding 与 Chat 使用独立的 URL 和密钥，因此 Chat 切换为 OpenAI Responses 不会改变 Qwen3 Embedding：

```env
AI_EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1
AI_EMBEDDING_API_KEY=replace-with-qwen3-embedding-api-key
AI_EMBEDDING_MODEL=Qwen/Qwen3-Embedding-8B
AI_EMBEDDING_DIMENSION=1024
AI_FALLBACK_EMBEDDING_MODEL=Qwen/Qwen3-Embedding-8B
```
