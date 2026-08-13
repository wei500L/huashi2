# EF.Transfer Platform

> 英法词汇迁移学习平台 Monorepo

## 架构概览

- `src/`: React 19 + TypeScript + Vite 前端
- `app-server/`: Spring Boot 4 业务后端，MySQL + Redis + RabbitMQ
- `ai-gateway/`: Spring Boot 3 AI / RAG 网关，PostgreSQL + pgvector
- `shared-kernel/`: 跨服务共享 DTO、枚举、事件契约
- `deploy/`: Docker Compose 与本地联调脚本
- `docs/`: 开发与使用文档

## 快速启动

```bash
cd deploy
cp .env.example .env
docker compose --env-file .env up -d mysql redis rabbitmq postgres

./mvnw -pl ai-gateway -am spring-boot:run
./mvnw -pl app-server -am spring-boot:run

npm install
npm run dev
```

## 关键约束

- 两个后端都使用 `schema.sql` 初始化数据库结构，不保留版本化迁移体系。
- `app-server` 的数据库结构来自 `app-server/src/main/resources/schema.sql`。
- `ai-gateway` 的数据库结构来自 `ai-gateway/src/main/resources/schema.sql`。
- 开发期 schema 变更流程是：改 DDL，清空数据库，重启服务重新建表。
- `ai-gateway` 的 pgvector 维度固定为 `1024`，并保留 HNSW 索引参数校验。

## 验证命令

```bash
npm run lint
npm run typecheck
npm run build
./mvnw test
```

> 注意：宿主机 Docker daemon 若为 ≥26（最低 API 1.40），跑 `ai-gateway` Testcontainers 测试需给测试 JVM 传 `-Dapi.version=1.44`（testcontainers 内置 docker-java 默认协商 API 1.32 会被拒绝），例如 `./mvnw -pl ai-gateway -am -DargLine=-Dapi.version=1.44 test`。

## 测试策略

- `app-server`: 以 Spring Boot 集成测试为主，H2 在测试启动时执行 schema 快照建表；关键 MySQL 约束用 Testcontainers 补充校验。
- `ai-gateway`: 以 PostgreSQL + pgvector Testcontainers 集成测试为主，直接执行 schema 快照建表，并配合 WireMock / mock provider 验证 RAG 与配置链路。
- 前端：Vitest + Testing Library。

## 协作注意事项

- 修改前后端接口时，同步检查 `shared-kernel/` 与 `src/lib/contracts.ts`。
- 修改 schema 时，不新增历史脚本，直接更新对应模块的 `schema.sql`。
- 诊断与训练 session 仍要求单用户最多一个 `IN_PROGRESS` 记录。
- 词汇知识变更仍通过 RabbitMQ 事件驱动 `ai-gateway` 定向重建索引。

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-22 | 初始创建 | 全仓扫描生成 |
| 2026-04-18 | 文档收敛 | 根文档改为以 `schema.sql` 启动建表为准 |
| 2026-04-18 | 数据库初始化调整 | 移除版本化迁移体系，后端改为单文件 `schema.sql` 建表 |
| 2026-08-12 | 公开研究问卷恢复个人信息 + 时限调整 | V1 问卷恢复姓名/联系方式（资料非英专 6 项 / 英专 8 项，仍为 60 道正式题），答题时限 40→60 分钟；作答前言保留"约 40 分钟"；初始器支持软删除字段恢复并同步时长快照；新增 `scripts/research-viewport-qa.mjs` 多视口视觉验收 |
| 2026-08-12 | 链路静态审查修复（15 项）+ 完善（3 项） | 问卷链：答题页题数/版本动态化（V1=60/V3=239）、导入模板 40→60、V3 初始器回退值与 publish 快照同步、软删题答案不渲染为必填、`basicInfo` 死代码移除并在参与者创建时记录 `consented_at`、section 级软删恢复；事件链：增量 watermark 只取综合同步任务、消费端校验 `eventVersion`/`sourceIds` 非空、`RagReindexRequest.sourceIds` 上限对齐 200、批量导入失败路径补发事件、`importCsv` 发布事务化；其余：session 唯一键冲突判定收紧、`api.ts` 刷新失败保留原始错误、超时结果轮询 15s→5s、拼写题单题错误次数上限 30、事件发布开关关闭时告警日志；新增测试 5 个（watermark 2、事件校验 2、ghost 字段 1） |
| 2026-08-12 | V3 题库转为学生自测练习模块 + LLM 个性化辅导 | V3 题库（`LEXIBRIDGE_FF4_V2`，239 道正式题）不再作为研究问卷（V1 保持唯一研究问卷），种子初始器改为只种题库（含"移除题软删同步"）；新增学生自测练习模块（`/practice`，`modules/practice/`）：无计时、整卷作答后评分、拼写首字母提示、独立练习历史；新增 LLM 个性化辅导链路：练习完成后生成辅导报告（异步 job，复用 guidance 场景 + RAG + grounding 校验 + 规则降级）+ 每题"AI 讲解"（结构化单题讲解 + 题库解析降级）；新增表 `practice_session`/`practice_session_answer`、`ai_generation_record.practice_session_id` |
| 2026-08-12 | 题库词条接入 RAG 知识库 + 生产部署 | 新增 `PRACTICE_WORD` 知识源：app-server 提供 `/internal/knowledge/practice-words/export` 导出接口，ai-gateway 启动时同步并向量化 239 个题库词条（FULL 模式、hash 幂等、失败重试 5 次后降级）；辅导/单题讲解场景的 RAG 检索纳入 `PRACTICE_WORD`；修复 guidance 归一化不覆盖词对标签的问题；为自测辅导场景提供独立的 grounding 校验提示词（服务端练习统计/模式目录视为可信，词义事实仍须证据支撑），并将"词义只引用证据原文"写入辅导 system prompt；生产环境已部署验证：练习全流程、AI 辅导（AI source）、单题讲解均通过 |
| 2026-08-12 | 辅导链路闭环优化（缓存/历史/拼写/复练） | 辅导报告快照写回 `practice_session.tutoring_status/tutoring_json`，结果页刷新直接读缓存不再重复触发 job；辅导上下文聚合近 10 次练习的错词统计（`listRecentWrongWordStats`，派生表规避 MySQL IN+LIMIT 限制），报告与规则降级均能指出"反复出错词"；新增拼写错误模式分析（`PracticeSpellingAnalyzer`：重音/字母替换/缺/多/形近/差异大），注入单题讲解与结果页标签；`StartPracticeSessionRequest.targetWords` 支持"针对错词再练一轮"（结果页入口，按词过滤组卷） |
| 2026-08-13 | 审计前五项 High 修复 | 问卷 HMAC/PII 密钥非 local/test fail-fast 并写入 compose/`.env.example`；内部 token 走共享 `SecretPolicy` 且 prod 禁止关闭；ai-gateway 8090 绑 loopback；通知 WS 改为首条 AUTH 消息、拒绝 query token；actuator 除 health 外限 ADMIN；辅导 grounding 空 citation/RAG 失败降级 `RULE_FALLBACK` |
