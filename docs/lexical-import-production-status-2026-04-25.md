# 词库导入生产状态（2026-04-25）

本文档记录 `docs/` 目录中语义词库 CSV 在当前线上环境 `https://huashi.qsfw.eu.cc` 的实际导入状态，避免后续重复导入重叠包。

## 当前结论

- 截至 `2026-04-25`，线上数据库**已经导入**：
  - `docs/semantic-lexicon-v2-import-ready-80.csv`
- 当前线上导入结果：
  - `lexical_import_batch`：`batch 2 = COMPLETED`
  - `lexical_pair`：`80`
  - `invalid_rows`：`0`
  - `imported_rows`：`80`

换句话说，`docs/` 里的 CSV 之前**没有**进库；本次已将一份基准全量包导入完成。

## 为什么只导入这一份

`docs/` 目录里的多个 CSV 不是互斥增量包，而是高度重叠的工作文件：

| 文件 | 数据行数 | 说明 |
| --- | ---: | --- |
| `semantic-lexicon-v2-import-ready.csv` | 15 | 初始小批次 |
| `semantic-lexicon-v2-import-ready-full.csv` | 40 | 初始完整批次 |
| `semantic-lexicon-v2-import-ready-pack-2.csv` | 40 | 第二批 |
| `semantic-lexicon-v2-import-ready-high-risk-false-friends.csv` | 23 | 高风险子集 |
| `semantic-lexicon-v2-import-ready-high-risk-false-friends-pack-2.csv` | 30 | 高风险子集补包 |
| `semantic-lexicon-v2-import-ready-pack-3.csv` | 67 | 后续工作包 |
| `semantic-lexicon-v2-import-ready-pack-3.fixed.csv` | 67 | pack-3 修复版 |
| `semantic-lexicon-v2-import-ready-pack-4.csv` | 80 | 后续工作包 |
| `semantic-lexicon-v2-import-ready-80.csv` | 80 | 当前基准全量包 |

本次只选择 `import-ready-80.csv`，原因是：

- 它是 `docs/` 中当前最稳定的基准全量包。
- 其他文件与它高度重叠，直接重复导入会制造重复治理成本。
- 线上词库此前是空库，先落一份完整基线最合理。

## 本次修复的站点问题

本次补数据和后续完善时，一共修复了两个真实站点问题：

- 问题：词库导入批次会卡在 `PARSING`，后台线程偶发读不到刚创建的批次。
- 根因：`LexicalImportBatchService.createBatch()` 和 `commitBatch()` 在事务提交前就调度异步任务，存在 after-commit 竞态。
- 修复：将异步解析和导入调度改为 **事务提交后** 执行。

- 问题：批量导入 80 条词对时会逐条发 `LexicalKnowledgeChangedEvent`，RabbitMQ 和 `ai-gateway` 会收到大量细粒度重建任务。
- 根因：`LexicalImportBatchService` 复用了 `lexicalPairService.create()`，而单条创建会立刻发知识变更事件。
- 修复：导入链路改成“逐条入库、整批聚合后按 chunk 发事件”，不再为同一批次制造 80 条独立知识同步消息。

对应代码：

- [LexicalImportBatchService.java](/mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/service/LexicalImportBatchService.java)
- [LexicalPairService.java](/mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/service/LexicalPairService.java)

数据库中的测试残留批次也已经清理为可解释状态：

- `batch 1 = FAILED`
- 原因：`Created before after-commit fix; parser task never started`

## RAG / 检索链路状态

本次除了导入词库，还补齐了“ai-gateway 重建完成后，把嵌入状态回写到 app-server 主库”的内部同步链路。

对应代码：

- [InternalKnowledgeController.java](/mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/internal/controller/InternalKnowledgeController.java)
- [InternalKnowledgeService.java](/mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/internal/service/InternalKnowledgeService.java)
- [LexicalPairMapper.java](/mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/mapper/LexicalPairMapper.java)
- [AppServerKnowledgeClient.java](/mnt/d/huashi2/ai-gateway/src/main/java/com/huashi/eftransfer/ai/modules/rag/integration/AppServerKnowledgeClient.java)
- [KnowledgeStoreRepository.java](/mnt/d/huashi2/ai-gateway/src/main/java/com/huashi/eftransfer/ai/modules/rag/repository/KnowledgeStoreRepository.java)
- [KnowledgeIngestionService.java](/mnt/d/huashi2/ai-gateway/src/main/java/com/huashi/eftransfer/ai/modules/rag/service/KnowledgeIngestionService.java)

线上验证结果：

- `job 82 = SUCCEEDED`：增量 reindex 验证了状态回写接口已经生效
- `job 84 = SUCCEEDED`：full reindex 回归验证了分页和状态回写不会再互相放大
- 当前 `lexical_pair.embedding_status`：
  - `EMBEDDED = 80`
  - `PENDING = 0`
  - `FAILED = 0`

补充说明：

- 状态回写现在**不会再修改** `lexical_pair.updated_at`
- 这样可以避免 reindex 按 `updated_at + cursor` 翻页时产生自反馈循环
- `last_embedded_at` 会被正常回写，用于后台和运维确认最近一次嵌入完成时间

## 网站中从哪里继续维护

词库导入中心现在可直接使用：

- 教师入口：`/teacher/lexical-pairs/imports`
- 管理员入口：`/admin/lexical-pairs/imports`

现在批次详情里除了导入统计，还会显示这批数据的知识同步摘要：

- `待嵌入`
- `已嵌入`
- `嵌入失败`
- `最近成功嵌入`

这三个数字来自当前 `lexical_pair.embedding_status` 的实时聚合，用来区分“CSV 已进主库”和“词条已进入知识库检索”这两个不同阶段。

管理员视图还可以直接对某个批次触发一次定向 RAG 重建：

- 入口：批次详情右上角 `重建本批索引`
- 作用范围：该批次已导入的 `LEXICAL_PAIR / LEXICAL_SENSE / LEXICAL_EXAMPLE`
- 页面会直接显示最近一次定向任务的 `jobId`、状态和基础统计

如果后续要继续扩词，建议顺序：

1. 先以 `semantic-lexicon-v2-import-ready-80.csv` 作为线上已导基线。
2. 新增包先与这 80 条做去重。
3. 优先从 `pack-3.fixed.csv`、`pack-4.csv` 中抽取**未重复**的新增词对。
4. 导入后在词对列表里抽样检查，再进入诊断模板、词表和 RAG 链路。

## 后续建议

- 不要把 `docs/` 下所有 CSV 一次性全部导入线上。
- 下一轮导入前，先做“与当前线上 80 条基线的去重清单”。
- 导入完成后，补一份新的状态文档，记录：
  - 导入的源文件
  - batch id
  - 新增词对数
  - 是否存在无效行
  - 是否已完成模板 / 词表接入
