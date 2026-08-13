# 数据库结构执行手册

项目当前以两个模块各自的 `schema.sql` 作为结构快照：

- MySQL：`app-server/src/main/resources/schema.sql`
- PostgreSQL/pgvector：`ai-gateway/src/main/resources/schema.sql`

## 全新环境

`deploy/docker-compose.yml` 已把两个 schema 以只读方式挂载到数据库镜像的初始化目录。仅在数据卷第一次创建时自动执行：

```bash
cd deploy
docker compose --env-file .env up -d mysql postgres
```

执行后至少核对：

```sql
-- MySQL
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE();

-- PostgreSQL
SELECT extversion FROM pg_extension WHERE extname = 'vector';
SELECT embedding_dimension, hnsw_m, hnsw_ef_construction FROM rag_schema_metadata WHERE id = 1;
SELECT pg_get_expr(indexprs, indrelid), pg_get_expr(indpred, indrelid)
FROM pg_index
WHERE indexrelid = 'idx_chunk_embedding_vector_hnsw'::regclass;
```

HNSW 索引的 predicate 必须包含 `is_current = true`，避免历史向量参与 ANN 候选；`chunk_embedding` 的维度约束也必须与 `rag_schema_metadata.embedding_dimension` 一致。

## 已有环境

数据库初始化目录不会在已有数据卷上重复执行。上线前应先备份，再在维护窗口中显式执行经过评审的 DDL；不要通过删除生产数据卷来套用新快照。

1. 使用 `deploy/scripts/backup-all.sh` 生成备份并验证可读取。
2. 比较目标环境结构与对应 `schema.sql`。
3. 将差异整理为 `docs/ddl/<module>/YYYY-MM-DD-*.sql` 一次性、可回滚的向前脚本，在预生产数据副本上验证。见 [`docs/ddl/README.md`](ddl/README.md)。
4. 停止写入或进入维护模式后执行 DDL。
5. 启动服务并检查 app-server、ai-gateway、pgvector 元数据和 RAG 探针。
6. embedding 模型、维度或指令模板有变化时，执行全量强制 reindex；完成前不得切换检索流量。

本次 current-only HNSW 调整在已有库中至少需要重建索引。维护窗口内可按目标库实际结构评审并执行等价 DDL：

```sql
DROP INDEX IF EXISTS public.idx_chunk_embedding_vector_hnsw;
CREATE INDEX idx_chunk_embedding_vector_hnsw
    ON public.chunk_embedding
    USING hnsw (embedding public.vector_cosine_ops)
    WITH (m = 16, ef_construction = 128)
    WHERE is_current = TRUE;
```

服务启动守卫会核对索引类型、距离算子、`m`、`ef_construction` 和 `is_current = TRUE` predicate；已有环境未完成该 DDL 时，ai-gateway 会拒绝以不一致索引启动。

## 回滚原则

- 若需要还原，使用 `CONFIRM_RESTORE=YES BACKUP_FILE=... ./deploy/scripts/restore-mysql.sh` 或 `restore-postgres.sh`，先在副本上验证。
- 应用回滚不等于数据库回滚；破坏性 DDL 必须准备反向脚本或从备份恢复。
- embedding 索引切换应使用新旧空间并存后原子切换，不能把不同模型的向量混在同一空间。
- 若 reindex 出现任何 `embeddingFailures`，任务应视为失败，修复上游后重试。
