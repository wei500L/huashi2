[根目录](../CLAUDE.md) > **app-server**

# app-server

## 模块职责

业务主后端，负责认证授权、词汇管理、诊断、训练、学情分析、AI 洞察编排、运维配置与内部知识导出。

## 启动与配置

- Main class: `com.huashi.eftransfer.app.AppServerApplication`
- 默认端口: `8080`
- 启动命令: `./mvnw -pl app-server -am spring-boot:run`
- Profile: `local` / `dev` / `prod`
- 配置文件:
  - `src/main/resources/application.yml`
  - `src/main/resources/application-local.yml`
  - `src/main/resources/application-dev.yml`
  - `src/main/resources/application-prod.yml`

## 关键依赖

- Spring Boot 4.0.3
- MyBatis-Plus 3.5.16
- MySQL Connector/J
- Redisson
- JJWT
- RabbitMQ
- `shared-kernel`

## 数据库说明

- 数据库: MySQL
- 建表快照: `src/main/resources/schema.sql`
- 测试快照: `src/test/resources/schema-h2.sql`
- 最终态约束仍保留：
  - 软删除唯一键约束
  - 诊断 / 训练 session 单用户并发唯一键
  - outbox DLQ 与相关索引

## 测试

- Spring Boot Test + JUnit 5
- 默认集成测试使用 H2，并在启动时执行 schema 快照
- 关键 MySQL 约束使用 Testcontainers MySQL 校验

## FAQ

- Q: 如何新增业务模块？
  A: 在 `modules/` 下补齐 `entity / mapper / service / controller / dto / vo`，并同步补充接口与测试。

- Q: 开发期如何修改数据库结构？
  A:
  1. 改 `src/main/resources/schema.sql`（及测试用 `schema-h2.sql`）
  2. 在 `docs/ddl/app-server/` 增加一次性向前脚本，已有环境按 `docs/db-migration-runbook.md` 执行
  3. **仅 local 空库** 可用 `docker compose down -v` 或 `DROP DATABASE` 后重启套用快照；禁止对有数据的环境删卷

- Q: 事件机制如何工作？
  A: 应用内事件继续走 Spring 事件，总线事件继续走 RabbitMQ。

## 目录提示

```text
app-server/
  pom.xml
  src/main/java/com/huashi/eftransfer/app/
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
| 2026-08-13 | 审计下一批 High | AI 限流与 180s 超时、问卷 Redis 限流、导出脱敏、schema ddl 约定 |
