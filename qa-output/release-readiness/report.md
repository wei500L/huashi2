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

## 生产前端复测（2026-04-25 15:54–16:03 CST）

复测基线：

- `frontend` 已切换为生产构建 + 静态托管 + `/api` `/ws` 同源反代
- 访问入口：`http://127.0.0.1:3000`
- 管理员验收账号：`admin.qa`

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| frontend 容器健康 | 通过 | `ef-transfer-frontend` 为 `healthy`，`FailingStreak=0` |
| 生产前端入口 | 通过 | 首页由 `nginx/1.27.5` 提供，不再包含 `@vite/client` |
| teacher 登录与工作台 | 失败 | 可登录，但工作台班级/发布项仍出现中文乱码 |
| student 登录与学习总览 | 失败 | 可登录，但欢迎文案中的学生名显示为乱码 |
| admin 登录 | 通过 | `admin.qa / QaAdmin@123456` 可登录 |
| admin 仪表盘 | 通过 | `/admin/dashboard` 可正常打开 |
| admin 配置中心 | 通过 | `/admin/config-center` 可正常读取当前 AI 配置 |
| app-server 代理 AI 健康 | 通过 | `/api/admin/ai-config/health` 返回 200 |
| ai-gateway 外部 actuator | 失败 | `http://127.0.0.1:8090/actuator/health` 仍返回 500 |
| teacher workspace API | 失败 | `/api/teacher/workspace` 仍返回 500 |

复测结论：

- 前端容器 `unhealthy` 问题是旧 dev Docker 前端拓扑带来的噪音；切到生产前端后已消失。
- 管理员链路已补齐，不再受默认 demo 管理员账号缺失阻断。
- 真正仍然阻断上线的是：自动化门禁失败、AI Gateway 外部健康探针异常、教师/学生中文字段乱码、`/api/teacher/workspace` 500。

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
- 当前状态：已通过生产前端容器切换复测消除；当前默认验收基线应改为生产前端而非 Docker dev 前端。

### P1-005 AI Gateway `/actuator/health` 直接访问返回 500

- 证据：`qa-output/release-readiness/docker-health.log`、`qa-output/release-readiness/execution-log.md`
- 表现：`curl http://127.0.0.1:8090/actuator/health` 返回 500 `Unexpected error while handling request /actuator/health`。
- 矛盾：Compose 显示 `ef-transfer-ai-gateway` healthy，需确认 healthcheck 是否命中了不同端口/路径或绕过了异常路径。
- 影响：上线健康探针与真实外部监控可能误判，AI Gateway 可观测性不合格。
- 建议：检查 ai-gateway actuator 配置、异常处理器是否错误拦截 actuator、Compose healthcheck 端口/路径。

### P1-006 教师接口/页面仍存在班级名乱码

- 证据：`qa-output/release-readiness/screenshots/teacher-after-login.png`、`qa-output/release-readiness/screenshots/prod-teacher-after-login.png`、`qa-output/release-readiness/tmp/teacher_classes.json`
- 表现：`/api/teacher/classes` 返回 `2024çº§è‹±æ³•...`，教师工作台页面显示多条乱码班级/发布项。
- 复测补充：student 生产前端学习总览欢迎文案也显示 `æŽåŽ`，但 `/api/auth/me` 返回 `displayName=李华` 正常，说明至少存在一条前端显示链路仍在错误解码或消费了错误来源。
- 数据库核对：`teaching_class.class_name` 为正常中文 `2024级英法迁移试点1班`。
- 影响：核心教师工作台展示乱码，生产首发不可接受。
- 建议：定位 `TeachingClass`/workspace VO 映射链路以及 student dashboard 文案来源，统一做中文字段编码/解码修复；修复后对 teacher/student 全链路做 API + UI 回归。

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
- 当前状态：已通过 `admin.qa` 完成管理员链路复测；该项不再阻断本地验收，但默认 demo 管理员不可用的事实仍需在文档或环境准备中说明。

## 已修复并复验的问题

- 用户显示名乱码：修复 `DisplayNameNormalizer` 后，API 登录返回：
  - `teacher.zhang -> 张老师`
  - `student.li -> 李华`
  - `student.wang -> 王敏`
- 证据：`qa-output/release-readiness/execution-log.md`

- 前端 Docker 健康误报：切换到生产前端容器后，`ef-transfer-frontend` 复测为 `healthy`，`/healthz` 探针稳定通过。
- 证据：`qa-output/release-readiness/screenshots/prod-login-teacher-initial.png`、`qa-output/release-readiness/execution-log.md`

- 管理员本地验收阻断：已补充 `admin.qa` 并完成登录、仪表盘、配置中心复测。
- 证据：`qa-output/release-readiness/screenshots/prod-admin-dashboard-after-nav.png`、`qa-output/release-readiness/screenshots/prod-admin-config-center-direct.png`

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
- 生产前端复测截图：
  - `qa-output/release-readiness/screenshots/prod-login-teacher-initial.png`
  - `qa-output/release-readiness/screenshots/prod-teacher-after-login.png`
  - `qa-output/release-readiness/screenshots/prod-student-after-login.png`
  - `qa-output/release-readiness/screenshots/prod-admin-dashboard.png`
  - `qa-output/release-readiness/screenshots/prod-admin-dashboard-after-nav.png`
  - `qa-output/release-readiness/screenshots/prod-admin-config-center-direct.png`
- 生产前端复测 API 探针：`qa-output/release-readiness/tmp/*.json`、`qa-output/release-readiness/tmp/ai_gateway_actuator_health.txt`

## 建议下一步

1. 优先修复当前仍成立的阻断项：P1-001、P1-002、P1-003、P1-005、P1-006、P1-007。
2. 以生产前端容器作为后续本地 release-readiness 默认入口，不再用 Docker dev 前端做上线判定。
3. 保留 `admin.qa` 作为本地管理员验收账号；修复后重跑三角色浏览器冒烟和 API 探针。
4. 阻断项收敛后，再执行真实 AI/RAG 端到端专项：AI 配置探针、词库导入、RAG 查询、fallback 场景。

## 修复后复测（2026-04-25T16:39:46+08:00）

本轮针对 P1-005、P1-006、P1-007 已完成代码修复，并在生产前端入口 `http://127.0.0.1:3000` 下重跑三角色验收。

复测结论：

- P1-005 已修复：`http://127.0.0.1:8090/actuator/health` 现返回 `HTTP/1.1 200`，外部 actuator 健康探针恢复正常。
- P1-006 已修复：教师工作台、教师班级列表、学生学习总览中的中文姓名/班级名均恢复正常显示。
- P1-007 已修复：`/api/teacher/workspace` 现返回 `SUCCESS`，与 `/api/teacher/workspace/overview` 保持兼容。

新增 API 证据：

- `qa-output/release-readiness/tmp/teacher_workspace.json`
  - `code=SUCCESS`
  - `teacherName=张老师`
- `qa-output/release-readiness/tmp/teacher_workspace_overview.json`
  - `recentClasses[0].className=2024级英法迁移试点1班`
- `qa-output/release-readiness/tmp/teacher_classes.json`
  - `data[0].className=2024级英法迁移试点1班`
- `qa-output/release-readiness/tmp/student_analytics_overview.json`
  - `studentName=李华`
- `qa-output/release-readiness/tmp/ai_gateway_actuator_health.txt`
  - `HTTP/1.1 200`

新增浏览器证据：

- 教师工作台：`qa-output/release-readiness/screenshots/prod-rerun-teacher-workspace.png`
- 学生学习总览：`qa-output/release-readiness/screenshots/prod-rerun-student-dashboard.png`
- 管理员仪表盘：`qa-output/release-readiness/screenshots/prod-rerun-admin-dashboard-direct.png`
- 运维配置中心：`qa-output/release-readiness/screenshots/prod-rerun-admin-config-center-direct.png`

三角色复测摘要：

- teacher：`teacher.zhang / Teacher@123456` 登录成功，落在 `/teacher/workspace`，班级卡片与最近发布标题不再乱码。
- student：`student.li / Student@123456` 登录成功，落在 `/dashboard`，标题显示 `李华，当前主风险为 低风险`。
- admin：`admin.qa / QaAdmin@123456` 登录成功；直接访问 `/admin/dashboard` 与 `/admin/config-center` 均可正常渲染。

当前仍未处理的阻断项收敛为：

- P1-001 前端 lint 门禁失败
- P1-002 合约生成检查失败
- P1-003 后端 OpenAPI 集成测试失败

更新建议下一步：

1. 优先处理剩余门禁类阻断：P1-001、P1-002、P1-003。
2. 保持生产前端容器作为本地 release-readiness 默认入口。
3. 在门禁问题收敛后，再补跑一次完整 release-readiness 和 AI/RAG 端到端专项。
