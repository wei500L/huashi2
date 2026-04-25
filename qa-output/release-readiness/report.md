# 上线测试执行报告（本地 Docker）

- 执行时间：2026-04-25 15:04–15:32 CST
- 环境：本地 Docker Compose（frontend/app-server/ai-gateway/mysql/redis/rabbitmq/postgres）
- 结论：不建议上线 / Release Blocked
- 门禁策略：零 P0/P1；当前存在 P1 阻断项和自动化门禁失败

## 执行摘要

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| 前端 typecheck | 通过 | `npm run typecheck` 成功 |
| 前端 lint | 失败 | 3 errors、7 warnings |
| 前端单测 | 通过 | 32 files / 104 tests passed |
| 前端 build | 通过 | `npm run build` 成功；存在 >500KB chunk 警告 |
| 合约检查 | 失败 | `src/lib/contracts/generated/session-domain.ts` 生成后有 diff |
| 后端全量测试 | 失败/中止 | 已出现 OpenAPI 集成测试失败；后续因耗时和已阻断而终止 |
| Docker 健康 | 失败 | frontend 容器 unhealthy；ai-gateway Docker health 标记 healthy，但直接访问 `/actuator/health` 返回 500 |
| 浏览器冒烟 | 失败 | 教师登录可进入，但页面数据存在乱码；截图已保存 |
| 显示名修复复验 | 通过 | teacher/student 登录 API 返回中文名正常 |

## 阻断缺陷

### P1-001 前端 lint 门禁失败

- 证据：`qa-output/release-readiness/frontend-gates.log`
- 失败项：
  - `src/features/teacher-workspace/copy.test.ts:9` 使用 `any`
  - `src/pages/diagnosis/index.tsx:33` 违反 Fast Refresh 组件导出规则
  - `src/pages/student/Assessments.tsx:33` `navigate` 未使用
- 影响：CI/上线质量门禁不通过。
- 建议：修复 3 个 error；7 个 hook warning 可作为 P2 评估，但建议一并处理。

### P1-002 合约生成检查失败

- 证据：`qa-output/release-readiness/frontend-gates.log`
- 差异：`AiOpsProtocolValues` 从 `['openai-compat','qwen-rerank']` 变为 `['openai-compat','openai-rerank','openai-chat-rerank','qwen-rerank']`。
- 影响：前后端合约文件未同步，存在 AI 配置枚举不一致风险。
- 建议：确认枚举变更是否预期；若预期，提交生成后的 `src/lib/contracts/generated/session-domain.ts`；否则回滚后端枚举来源。

### P1-003 后端 OpenAPI 集成测试失败

- 证据：`qa-output/release-readiness/backend-tests.log`
- 失败测试：`ApiDocumentationSecurityIntegrationTest.adminCanAccessOpenApiDocumentInNonLocalProfiles`
- 错误：缺失 JSON path `$.components.schemas.DiagnosisSessionStatus.enum[0]`。
- 影响：OpenAPI 文档 schema 与测试预期不一致，可能影响 API 文档、SDK/合约生成和运维验收。
- 建议：检查 SpringDoc schema 生成配置或更新测试预期；修复后重跑后端全量测试。

### P1-004 Docker 前端容器 unhealthy

- 证据：`qa-output/release-readiness/docker-health.log`
- 表现：`ef-transfer-frontend` `FailingStreak` 持续增长，健康检查超时 5s。
- 补充：宿主机 `curl http://127.0.0.1:3000/` 可返回 200，说明服务可访问但容器内 healthcheck 不稳定/超时。
- 影响：Compose 健康状态不可信，会影响依赖启动、监控和上线判定。
- 建议：排查容器内 `node -e fetch('http://127.0.0.1:3000')` 超时原因；调整 healthcheck timeout/start_period 或改为更稳定探针。

### P1-005 AI Gateway `/actuator/health` 直接访问返回 500

- 证据：`qa-output/release-readiness/docker-health.log`、`qa-output/release-readiness/execution-log.md`
- 表现：`curl http://127.0.0.1:8090/actuator/health` 返回 500 `Unexpected error while handling request /actuator/health`。
- 矛盾：Compose 显示 `ef-transfer-ai-gateway` healthy，需确认 healthcheck 是否命中了不同端口/路径或绕过了异常路径。
- 影响：上线健康探针与真实外部监控可能误判，AI Gateway 可观测性不合格。
- 建议：检查 ai-gateway actuator 配置、异常处理器是否错误拦截 actuator、Compose healthcheck 端口/路径。

### P1-006 教师接口/页面仍存在班级名乱码

- 证据：`qa-output/release-readiness/screenshots/teacher-after-login.png`、API 探针输出
- 表现：`/api/teacher/classes` 返回 `2024çº§è‹±æ³•...`，教师工作台页面显示多条乱码班级/发布项。
- 数据库核对：`teaching_class.class_name` 为正常中文 `2024级英法迁移试点1班`。
- 影响：核心教师工作台展示乱码，生产首发不可接受。
- 建议：定位 `TeachingClass`/workspace VO 映射链路，统一显示字段编码修复；修复后对所有中文字段做 API 级回归。

### P1-007 `/api/teacher/workspace` 返回 500

- 证据：App Server 日志 traceId `39cd8989-be52-4aa4-bda1-484603721475`
- 错误：`No static resource api/teacher/workspace for request '/api/teacher/workspace'`。
- 影响：若前端或后续功能依赖该接口，将直接失败；即使当前页面可渲染，也说明接口契约/路由存在不一致。
- 建议：确认前端实际调用接口与后端 controller 路由；不存在的接口应返回 404/明确错误，不应进入全局 500。

### P1-008 管理员默认测试账号不可登录

- 证据：`qa-output/release-readiness/execution-log.md`
- 表现：`admin / Admin@123456` 返回 401。
- 影响：完整三角色验收无法使用 admin 账号执行管理员链路。
- 建议：准备专用 `admin.qa` 或重置本地 admin 密码；之后补跑管理员用户、审计、AI 配置、词库导入链路。

## 已修复并复验的问题

- 用户显示名乱码：修复 `DisplayNameNormalizer` 后，API 登录返回：
  - `teacher.zhang -> 张老师`
  - `student.li -> 李华`
  - `student.wang -> 王敏`
- 证据：`qa-output/release-readiness/execution-log.md`

## 非阻断风险 / P2

- 前端 build 存在大 chunk 警告：`vendor` 约 1MB，`chart-engine` 约 412KB。
- 前端测试中存在多处 React `act(...)` warning，建议测试稳定性专项处理。
- `npm audit` 显示 6 vulnerabilities（4 moderate，2 high），需要安全评估。
- SpringDoc 在 prod profile 输出启用警告：若生产不应暴露 API docs，应配置关闭或限制。
- 当前验收环境为本地 Docker，不覆盖 HTTPS、真实域名、CDN、生产网络和证书链路。

## 产物

- 初始环境与执行日志：`qa-output/release-readiness/execution-log.md`
- 前端门禁日志：`qa-output/release-readiness/frontend-gates.log`
- 后端测试日志：`qa-output/release-readiness/backend-tests.log`
- Docker 健康日志：`qa-output/release-readiness/docker-health.log`
- 浏览器截图：`qa-output/release-readiness/screenshots/initial-login.png`
- 教师登录后截图：`qa-output/release-readiness/screenshots/teacher-after-login.png`

## 建议下一步

1. 先修复 P1-001 到 P1-008。
2. 准备专用 `admin.qa / teacher.qa / student.qa` 三类账号，避免依赖 demo 密码。
3. 修复后重新执行：前端门禁、后端全量测试、Docker health、三角色浏览器冒烟。
4. 再执行真实 AI/RAG 端到端专项：AI 配置探针、词库导入、RAG 查询、fallback 场景。
