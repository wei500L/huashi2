[根目录](../CLAUDE.md) > **ai-gateway**

# ai-gateway

## 模块职责

AI / RAG 网关，负责 Provider 调用、运行时配置装载、知识摄取、向量检索、重排与内部 AI 接口。

## 启动与配置

- Main class: `com.huashi.eftransfer.ai.AiGatewayApplication`
- 默认端口: `8090`
- 启动命令: `./mvnw -pl ai-gateway -am spring-boot:run`
- Profile: `local` / `dev` / `prod`
- 配置文件:
  - `src/main/resources/application.yml`
  - `src/main/resources/application-local.yml`
  - `src/main/resources/application-dev.yml`
  - `src/main/resources/application-prod.yml`

## 关键依赖

- Spring Boot 3.5.3
- Spring AI 1.1.3
- PostgreSQL Driver
- pgvector
- Resilience4j
- RabbitMQ
- `shared-kernel`

## 数据库说明

- 数据库: PostgreSQL + pgvector
- 建表快照: `src/main/resources/schema.sql`
- 最终态要求仍保留：
  - `CREATE EXTENSION vector`
  - `chunk_embedding.embedding` 维度 `1024`
  - HNSW 索引参数
  - `rag_schema_metadata` 与维度守卫校验

## 测试

- PostgreSQL + pgvector Testcontainers 集成测试
- schema 快照由测试启动时直接执行
- `RagSchemaDimensionGuardTest` 继续校验维度与 HNSW 参数

## FAQ

- Q: 如何切换 AI Provider？
  A: 通过环境变量或 app-server 运维配置中心更新运行时配置。

- Q: 开发期如何修改数据库结构？
  A:
  1. 改 `src/main/resources/schema.sql`
  2. `docker compose down -v` 或手动 `DROP DATABASE`
  3. 重启服务，Spring 自动执行 schema 建表

- Q: 如何触发知识重建？
  A: 继续通过内部 RAG 接口或 RabbitMQ 事件驱动。

## 目录提示

```text
ai-gateway/
  pom.xml
  src/main/java/com/huashi/eftransfer/ai/
  src/main/resources/
    application.yml
    application-{local,dev,prod}.yml
    schema.sql
  src/test/
```

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-22 | 初始创建 | 全量扫描生成 |
| 2026-04-18 | 数据库初始化调整 | 移除版本化迁移体系，改为单文件 `schema.sql` |
