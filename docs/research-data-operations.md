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

## 群体 AI 报告

- 默认最小样本 5。不足时接口拒绝生成模型报告。
- 报告绑定 `research_aggregate_snapshot`，同一快照 + promptVersion 幂等。
- `FALLBACK` 表示规则摘要，不得当作模型结论。
- 模型输入只有聚合数据、匿名题号和题干摘要。

## 配置

```
app.assessment.research.min-sample-size
app.assessment.research.ai-enabled
app.assessment.research.local-root
app.assessment.research.max-file-bytes
```

开发期 schema 变更：更新 `schema.sql` / `schema-h2.sql`，并在 `docs/ddl/` 增加向前脚本。仅 local 空库可清空后重启建表；已有环境按 `docs/db-migration-runbook.md` 执行。
