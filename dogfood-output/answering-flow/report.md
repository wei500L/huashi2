# Dogfood Report: 英语语义迁移学习平台（学生答题链路）

| Field | Value |
|-------|-------|
| **Date** | 2026-07-19 |
| **App URL** | http://127.0.0.1:4173 |
| **Session** | huashi-answering-flow |
| **Scope** | 学生进入答题、作答、保存/恢复、提交、判分、结果反馈与异常状态 |

## Summary

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High | 1 |
| Medium | 0 |
| Low | 0 |
| **Total** | **1** |

## Issues

### ISSUE-001 — “所有学生可见”的已发布诊断任务导致学生模板接口 500

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Area** | 学生风险诊断 / 教师发布诊断任务 / 后端模板查询 |
| **Evidence** | [教师发布成功](screenshots/teacher-published-diagnosis.png) · [学生端错误与空态并存](screenshots/issue-001-step-2-student-error.png) · [复现视频](videos/issue-001-global-diagnosis-template-500.webm) |

**Reproduction**

1. 教师创建并发布一份发布目标为“所有学生可见”的有效诊断模板。
2. 学生登录并进入“开始风险诊断”。
3. 等待已发布任务列表加载。

**Actual:** `GET /api/student/diagnosis-templates` 返回 500；页面同时显示“Unexpected error while handling request”和“当前没有已发布诊断任务”，学生无法开始诊断。

**Expected:** 返回包含刚发布模板的列表，学生可创建诊断 session 并进入答题。

**Backend evidence:** 全局模板的 `target_class_id` 为 `NULL`。`DiagnosisTemplateService.pagePublishedTemplates()` 在第 278 行执行 `classMap.get(entity.getTargetClassId())`；空映射由 `Map.of()` 构造，`get(null)` 抛出 `NullPointerException`。服务日志记录同一路径 500 和该堆栈。

**Product impact:** 默认“所有学生可见”发布方式会截断诊断入口，并连带阻断诊断结果、训练计划、错题本、复习计划和学情聚合。
