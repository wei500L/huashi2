# 已有环境增量 DDL

`schema.sql` 仍是空库和测试的目标快照：

- MySQL：`app-server/src/main/resources/schema.sql`
- PostgreSQL：`ai-gateway/src/main/resources/schema.sql`

已有环境（含任何有数据的卷）不要靠删除数据卷或 `DROP DATABASE` 套用新快照。每次结构变更都要同时：

1. 更新对应模块的 `schema.sql`（以及 app-server 的 `schema-h2.sql`）。
2. 在本目录新增一次性向前脚本：`docs/ddl/<module>/YYYY-MM-DD-short-name.sql`。
3. 按 [`docs/db-migration-runbook.md`](../db-migration-runbook.md) 备份、在副本上验证、再在维护窗口执行。

`<module>` 为 `app-server` 或 `ai-gateway`。脚本必须可回滚或附带反向说明。

仅 **local 空库** 可以用 `docker compose down -v` 重建。生产与任何已有数据环境禁止该流程。
