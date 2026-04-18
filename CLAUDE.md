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
