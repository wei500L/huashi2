# 研究问卷数据、附件与群体报告运维说明

## 统计口径

- 完成率 = `submittedAttemptCount / startedAttemptCount`。分母为 0 时接口返回 `value: null`，页面显示 `—`。
- 邀请码兑换率 = `verifiedCodeCount / nonRevokedCodeCount`。二维码免码参与没有预生成总人数，不并入该分母。
- 题目难度 = `correctCount / validAnsweredCount`，不使用所有进入者。
- 反应时只统计有效 timing 样本。

## 权限与隐私

- 公开研究答卷以 `participant_id` 为身份，教师看到 `P-000137` 这类匿名编号。
- 管理员不自动获得敏感资料查看权；敏感导出仅限问卷所有者，并写入 `RESEARCH_SENSITIVE_EXPORT_CREATED`。
- 普通导出不含 IP、参与码明文、姓名、联系方式和 objectKey。
- 附件下载走 `/api/teacher/research/files/{fileId}/download`，再次校验 paper/publish 权限。类型校验未通过（非 `CLEAN`）不可下载。存储值 `CLEAN` 表示魔数与扩展名校验通过，不是杀毒扫描。
- 研究 PII 保留期默认 `P730D`（`app.assessment.research.retention` / `APP_ASSESSMENT_RESEARCH_RETENTION`），锚点为 `consented_at`，空则 `created_at`。
- `ResearchRetentionService` 每 30 分钟扫描到期记录：清空 `sensitive_profile_*` 并写 `anonymized_at`；物理删除到期 `assessment_participant_access`；删除到期已绑定（BOUND）对象并标文件删除。
- 答卷、分数与匿名编号保留，研究分析仍可用。未到期记录不动。
- 这不是参与者自助删除 API，也不做法务 DPIA。缩短保留期改 env 后等下一轮调度即可，已匿名行不会自动恢复密文。

## 群体 AI 报告

- 默认最小样本 5。不足时接口拒绝生成模型报告。
- 报告绑定 `research_aggregate_snapshot`，同一快照 + promptVersion 幂等。
- `FALLBACK` 表示规则摘要，不得当作模型结论。
- 模型输入只有聚合数据、匿名题号和题干摘要。

## 验收与试运行数据收尾

生产验收不得物理删除已有答卷或发布记录。使用下列可审计、可验证的收尾流程：

1. 教师在“研究问卷 → 发布”选择目标发布并执行“结束发布”。该操作会关闭二维码入口、将截止时间收敛到当前时间，并停用全部未使用参与码；已开始和已提交答卷继续保留。
2. 教师从测试班级移出测试学生并归档测试班级。
3. 管理员在“用户管理”禁用测试学生账号。账号状态或角色一旦变更，服务端会立即撤销 refresh session 并拉黑当前 access token，而不是等待 JWT 自然过期。
4. 研究参与者 PII、访问 IP 和附件按本页“权限与隐私”中的 retention 策略匿名化/删除；匿名答卷和统计事实保留用于审计与研究复现。

测试数据应使用统一、可搜索的前缀（例如 `E2E-TEST-<日期>-<批次>`），收尾时记录班级 ID、用户 ID、publish ID 和 release code。问卷或发布已有答卷时不提供物理删除按钮，这是数据完整性约束，不是清理遗漏。

## 配置

```
app.assessment.research.min-sample-size
app.assessment.research.ai-enabled
app.assessment.research.local-root
app.assessment.research.max-file-bytes
app.assessment.research.retention   # 默认 P730D，可用 APP_ASSESSMENT_RESEARCH_RETENTION 覆盖
```

Compose 把该变量传给 `app-server`。值为 ISO-8601 Duration。`PT0S` 或非正值时任务跳过，不会误删。

开发期 schema 变更：更新 `schema.sql` / `schema-h2.sql`，并在 `docs/ddl/` 增加向前脚本。仅 local 空库可清空后重启建表；已有环境按 `docs/db-migration-runbook.md` 执行。
