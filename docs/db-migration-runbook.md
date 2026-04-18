# 数据库迁移执行手册

本手册覆盖本仓库当前的 Flyway 迁移安全约束，重点是：

- `baseline-on-migrate` 只允许 `local` / `dev` 开启
- `app-server` 的 `V27` 迁移前必须先清理重复进行中的 session
- `app-server` 的 `V30-V34` 迁移前必须先检查活动数据是否存在新唯一键冲突
- `ai-gateway` 的向量维度固定为 `1024`
- `ai-gateway` 的 `V5` 会删除旧版 `rag_knowledge_document` 表

## 1. 迁移前检查

### 1.1 Profile 与 Flyway 基线

- `application-prod.yml` 中两端都必须保持 `spring.flyway.baseline-on-migrate: false`
- `application-local.yml` / `application-dev.yml` 才允许 `baseline-on-migrate: true`
- 生产执行前确认 `SPRING_PROFILES_ACTIVE=prod`

### 1.2 app-server: V27 重复 session 预清理

迁移 `V27__session_concurrency_and_idempotency.sql` 前，必须保证每个用户最多只有一个 `IN_PROGRESS` 的诊断/训练 session。

诊断预检：

```sql
SELECT owner_user_id, COUNT(*) AS active_count
FROM diagnosis_session
WHERE deleted = FALSE
  AND status = 'IN_PROGRESS'
GROUP BY owner_user_id
HAVING COUNT(*) > 1;
```

训练预检：

```sql
SELECT owner_user_id, COUNT(*) AS active_count
FROM training_session
WHERE deleted = FALSE
  AND status = 'IN_PROGRESS'
GROUP BY owner_user_id
HAVING COUNT(*) > 1;
```

如果存在结果，按每批 `500` 行清理，保留同一用户 `started_at` 最新、`id` 最大的那条记录，其余记录更新为 `ABANDONED` 并清空 `current_item_order`。迁移脚本本身不再执行这类大表更新。

### 1.3 app-server: V30-V34 新唯一键冲突预检

在执行 `V30-V34` 前，先检查活动数据是否已经违反新唯一键。下面查询任意一条返回结果都必须先人工修复，再执行迁移。

```sql
SELECT english_word, french_word, COUNT(*)
FROM lexical_pair
WHERE deleted = FALSE
GROUP BY english_word, french_word
HAVING COUNT(*) > 1;

SELECT tag_name, COUNT(*)
FROM lexical_tag
WHERE deleted = FALSE
GROUP BY tag_name
HAVING COUNT(*) > 1;

SELECT lexical_pair_id, lexical_tag_id, COUNT(*)
FROM lexical_pair_tag_rel
WHERE deleted = FALSE
GROUP BY lexical_pair_id, lexical_tag_id
HAVING COUNT(*) > 1;

SELECT lexical_list_id, lexical_pair_id, COUNT(*)
FROM lexical_list_item
WHERE deleted = FALSE
GROUP BY lexical_list_id, lexical_pair_id
HAVING COUNT(*) > 1;

SELECT owner_user_id, source_diagnosis_summary_id, COUNT(*)
FROM training_plan
WHERE deleted = FALSE
GROUP BY owner_user_id, source_diagnosis_summary_id
HAVING COUNT(*) > 1;

SELECT owner_user_id, lexical_pair_id, COUNT(*)
FROM wrong_book
WHERE deleted = FALSE
GROUP BY owner_user_id, lexical_pair_id
HAVING COUNT(*) > 1;

SELECT publish_id, student_user_id, COUNT(*)
FROM assessment_attempt
WHERE deleted = FALSE
GROUP BY publish_id, student_user_id
HAVING COUNT(*) > 1;

SELECT attempt_id, question_order, COUNT(*)
FROM assessment_attempt_answer
WHERE deleted = FALSE
GROUP BY attempt_id, question_order
HAVING COUNT(*) > 1;

SELECT publish_id, student_user_id, COUNT(*)
FROM assessment_publish_recipient
WHERE deleted = FALSE
GROUP BY publish_id, student_user_id
HAVING COUNT(*) > 1;

SELECT owner_user_id, achievement_code, COUNT(*)
FROM achievement
WHERE deleted = FALSE
GROUP BY owner_user_id, achievement_code
HAVING COUNT(*) > 1;

SELECT teaching_class_id, student_user_id, COUNT(*)
FROM teaching_class_student
WHERE deleted = FALSE
  AND active = TRUE
GROUP BY teaching_class_id, student_user_id
HAVING COUNT(*) > 1;

SELECT scope, student_user_id, COUNT(*)
FROM learning_profile_snapshot
WHERE deleted = FALSE
  AND student_user_id IS NOT NULL
GROUP BY scope, student_user_id
HAVING COUNT(*) > 1;

SELECT scope, teaching_class_id, COUNT(*)
FROM learning_profile_snapshot
WHERE deleted = FALSE
  AND teaching_class_id IS NOT NULL
GROUP BY scope, teaching_class_id
HAVING COUNT(*) > 1;

SELECT owner_user_id, stat_date, source_type, aggregation_level, lexical_pair_id, lexical_pair_type, training_mode, context_support_level, COUNT(*)
FROM analytics_daily_aggregate
WHERE deleted = FALSE
GROUP BY owner_user_id, stat_date, source_type, aggregation_level, lexical_pair_id, lexical_pair_type, training_mode, context_support_level
HAVING COUNT(*) > 1;

SELECT teaching_class_id, stat_date, source_type, aggregation_level, lexical_pair_id, lexical_pair_type, training_mode, context_support_level, COUNT(*)
FROM class_analytics_daily_aggregate
WHERE deleted = FALSE
GROUP BY teaching_class_id, stat_date, source_type, aggregation_level, lexical_pair_id, lexical_pair_type, training_mode, context_support_level
HAVING COUNT(*) > 1;

SELECT batch_id, COUNT(*)
FROM lexical_import_file
WHERE deleted = FALSE
GROUP BY batch_id
HAVING COUNT(*) > 1;

SELECT batch_id, import_row_number, COUNT(*)
FROM lexical_import_row
WHERE deleted = FALSE
GROUP BY batch_id, import_row_number
HAVING COUNT(*) > 1;
```

### 1.4 ai-gateway: 向量维度与旧表删除

- `AI_EMBEDDING_DIMENSION` 必须为 `1024`
- `spring.ai.vectorstore.pgvector.dimensions` 必须为 `1024`
- 所有 provider 的 embedding `dimension` 都必须为 `1024`
- `V5__drop_legacy_rag_knowledge_document.sql` 会删除旧版 `rag_knowledge_document` 及其索引；如本地仍保留手工旧数据，先自行导出

## 2. 备份步骤

迁移前必须先完成双库备份：

```bash
cd deploy
./scripts/backup-mysql.sh
./scripts/backup-postgres.sh
```

如需统一执行：

```bash
cd deploy
./scripts/backup-all.sh
```

## 3. 推荐执行顺序

1. 部署配置改动，确认 `prod` profile 下 `baseline-on-migrate=false`
2. 执行 `app-server` 迁移前预检，先处理 `V27` 重复 session 和 `V30-V34` 唯一键冲突
3. 启动 `app-server`，执行 `V7`、`V17`、`V22`、`V27`、`V30-V36`
4. 启动 `ai-gateway`，执行 `V1-V5`
5. 观察启动日志，确认 ai-gateway 未触发向量维度 fail-fast

## 4. 失败处理

### 4.1 app-server

- 如果失败发生在 `V27` 之前，修正数据后直接重跑 Flyway
- 如果失败发生在 `V30-V34` 某一张表：
- 先查看当前表是否已创建新的 `_active` 唯一索引
- 如果新索引已存在但旧索引尚未删除，可以直接删除新索引后重跑
- 如果旧索引已删除且新索引也不可用，立即按该表原始唯一键定义手工重建旧索引
- 如果受影响表超过 1 张，或无法确定执行到哪一步，停止手工修复，直接从 MySQL 逻辑备份恢复

### 4.2 ai-gateway

- 如果启动报向量维度不匹配，先修正配置回 `1024`，不要跳过 guard
- 如果 `V5` 删除了本地旧表但仍需排查旧数据，改从迁移前的 PostgreSQL 备份恢复到独立实例核对
- 如果 HNSW 索引构建失败，修正环境后重跑 Flyway；不要手工修改 `flyway_schema_history`
