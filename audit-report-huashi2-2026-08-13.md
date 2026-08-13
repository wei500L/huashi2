# Fuck My Shit Mountain Audit Report

**Project:** EF.Transfer / huashi2
**Audit mode:** full
**Date:** 2026-08-13
**Reviewer:** Cursor Grok 4.6
**Remediation update:** 2026-08-13 **已完成三批** High：前五项（问卷默认密钥、内部 token 熵、WS query token、actuator 角色、辅导 grounding 空 citation）；下一批五项（AI 配额/超时、问卷 Redis 限流、schema 文档与 ddl/restore、CI `npm test`、导出脱敏）；第三批五项（XFF 仅信代理 CIDR、练习 `answeredCount`、导入 `TransactionTemplate`、单题讲解 fail-closed、verifier 不信任学生作答）。下文 Evidence 保留审计时快照；`Remediation: Fixed` 与 Fix note 反映当前代码。

---

## 1. Executive Summary

EF.Transfer 是一个已具备生产部署痕迹的英法词汇迁移学习平台：React 19 SPA、Spring Boot 4 业务后端、Spring Boot 3 AI/RAG 网关、MySQL + Redis + RabbitMQ + PostgreSQL/pgvector。安全基线明显做过一轮加固——JWT 密钥熵校验、内部接口 token、公开问卷 cookie 会话、敏感资料 AES-GCM、参与码 HMAC、outbox、单用户 `IN_PROGRESS` 唯一键、RAG grounding 二次校验、生产 overlay 收口端口。2026-08-13 又补上问卷密钥 fail-fast、内部 token `SecretPolicy`、WS 首条 AUTH、actuator ADMIN、辅导空 citation fail-closed；随后补上 AI 用户/IP 限流与 180s 超时对齐、问卷 Redis 限流、`docs/ddl`+restore、CI `npm test`、导出按开关脱敏；再补上 XFF 仅信代理、练习计分对齐、导入行级事务、单题讲解 fail-closed、verifier 隔离学生作答。这不是从零开始的屎山。

**剩余**主要风险集中在 **compose 仍暴露 MySQL/8080**、练习开局 409 映射、公开问卷 CSRF、以及超大文件可维护性。这些仍是可落地的发布前问题。

亮点：模块边界清楚、公开问卷会话 token 不回 JSON、诊断/训练有 DuplicateKey→409、JWT 与问卷/内部密钥启动期拒绝弱值、辅导报告 UI 对 `RULE_FALLBACK` 有明确标记、生产 overlay 把数据面绑到 `127.0.0.1`。优先顺序：收口剩余数据面端口、CSRF 保护 cookie 会话、练习开局唯一键 409。综合评分 **7.2 / B**（审计当日 6.2；前两批 High 后 6.9；第三批五项后再上调）。

### Score Dashboard

```
Security        ███████░░░  7.6  A   导出脱敏与 XFF 代理校验已落地；compose 仍暴露 MySQL/8080
Stability       ███████░░░  7.1  B   outbox 扎实；问卷 Redis 限流；练习计分与导入行事务已修
Performance     ███████░░░  7.4  A   连接池与分页存在；AI 已有用户/IP 限流且超时对齐 nginx 180s
Testing         ██████░░░░  6.6  B   后端集成测试扎实；CI 已跑前端测试；练习页仍零 Vitest
Maintainability ██████░░░░  5.8  B   ConfigCenter 4211 行、contracts.ts 3481 行、AssessmentService 1940 行
Design          ███████░░░  7.1  B   辅导与单题讲解 grounding 已 fail-closed；verifier 不再信任学生作答
Release         ██████░░░░  6.1  B   schema 快照仍无 Flyway；已有 docs/ddl 与 restore；MySQL/8080 仍全接口发布
─────────────────────────────────────
Overall         ███████░░░  7.2  B
```

Each dimension scored 0.0–10.0. **Higher = better (10 = clean, 0 = shit mountain).** Scores are judgment-based, not formula-based. See `rubrics/scoring.md` for anchor descriptions. 维度分在 2026-08-13 三批 High 修复后微调；未重跑全仓审计。

权重推断：本仓库同时是研究问卷（PII）、教学系统和 LLM/RAG 网关，安全、隐私、数据完整性、AI 安全在判断中权重大于纯样式问题。覆盖以静态代码、配置、CI 与 schema 为主，未做渗透或生产运行时验证。

### Finding Statistics

| Severity | Count | Confirmed | Suspected | Remediated |
|----------|-------|-----------|-----------|------------|
| Critical | 0 | 0 | 0 | 0 |
| High | 15 | 15 | 0 | 15 已完成 |
| Medium | 13 | 13 | 0 | 文档 Flyway/无前端测试/Testcontainers API 已补 |
| Low | 1 | 1 | 0 | 0 |
| Info | 0 | 0 | 0 | 0 |
| **Total** | **29** | **29** | **0** | **15 High 已完成** |

## 2. Project Map

主组件：

- `src/`：Vite + React 19 SPA（学生/教师/管理员 + 公开研究问卷）
- `app-server/`：认证、词库、诊断、训练、练习、学情、公开问卷、文件上传、AI 编排、内部知识导出
- `ai-gateway/`：chat / embedding / rerank、RAG 摄取与检索、运行时配置同步
- `shared-kernel/`：DTO、枚举、事件契约
- `deploy/`：Compose、Dockerfile、nginx、备份脚本

入口与初始化：前端 `src/main.tsx` → `App.tsx` 懒加载路由；后端 `AppServerApplication` / `AiGatewayApplication`；本地/dev 用 `spring.sql.init.mode=always` 执行 `schema.sql`，prod 为 `never`，库初始化靠 Docker entrypoint 挂载 schema。

数据流：浏览器 → nginx `/api` → app-server JWT 或公开问卷 cookie → MySQL/Redis/RabbitMQ；AI 场景经 `X-Internal-Token` 调 ai-gateway；词对变更事件驱动定向 reindex。

状态所有权：登录会话在 Redis + JWT；公开问卷会话在 DB digest + httpOnly cookie；诊断/训练/练习 `IN_PROGRESS` 由生成列唯一键约束；AI 异步 job 在 DB。

持久化：MySQL `app-server/.../schema.sql`（1857 行）、PostgreSQL/pgvector `ai-gateway/.../schema.sql`。无 Flyway/Liquibase。备份与 `restore-mysql.sh` / `restore-postgres.sh` 已有；未见还原演练记录。

隐私数据：研究问卷姓名/联系方式（AES-GCM）、参与码 HMAC、访问 IP 密文、上传附件、JWT 在 localStorage。

外部接口：REST、`/ws/notifications`、对象存储（本地或配置的 provider）、LLM HTTP、RabbitMQ。

安全边界：`SecurityConfig` + JWT filter + Internal API filter；ai-gateway 几乎全是 `/internal/**`；公开问卷 `permitAll` 但靠 cookie 会话。

高风险区：公开问卷与文件上传、内部 AI 接口、运维配置中心、RAG 提示词与 grounding、schema 变更、CI。

### Coverage Note

已扫描约 1416 个清单文件（962 Java / 191 TS）。排除 `node_modules`、`target`、`dist`、`.git`、锁文件内容、题库 JSON 全文。未跑完整 `./mvnw test` / `npm test`（耗时与 Docker API 约束）；未做浏览器可达性实测；未扫描生产密钥仓库。命令：`project_inventory.py`、多组 ripgrep、行数统计、`git rev-parse`（HEAD `9f38351`）。后续对照专项审查补强了导出脱敏、练习计分、导入事务、XFF、单题讲解 grounding 与 verifier 围栏；第三批五项已对照当前代码与定向集成测试回写 Fix note。

### Coverage Matrix

| Dimension | Coverage | Evidence inspected | Exclusions / limits |
|-----------|----------|--------------------|---------------------|
| Architecture | High | 模块目录、SecurityConfig、事件/outbox、shared-kernel 契约 | 未画运行时调用图 |
| Security | High | SecurityConfig、JWT/内部 token、公开问卷、WS、actuator、上传扫描 | 未做动态攻击验证 |
| Stability | High | outbox、唯一键、超时、熔断配置、异步线程池、限流实现 | 未做故障注入 |
| Performance | Medium | 连接池、AI 超时、Redis 限流、RAG 线程池 | 无负载测试 |
| Testing | High | CI workflow、46 个前端测试文件、后端集成测试样例 | 未实际跑完整测试套件 |
| Maintainability | High | 最大文件行数、模块结构、过期文档 | 未做完整圈复杂度工具扫描 |
| Design | High | SRP/文件大小、fail-fast、CQS、分层 | 原则检查针对高风险违规 |
| Release | High | CI、Dockerfile、compose、schema init、backup 脚本 | 未验证生产集群实际发布 |
| Documentation | High | README、CLAUDE.md、db-migration-runbook、src/CLAUDE.md | 未逐条核全部 docs/ 历史计划 |
| Configuration | High | application*.yml、.env.example、compose overlay | 未读生产主机真实 .env |
| Observability | High | management endpoints、logback、traceId、healthcheck | 无告警系统配置可查 |
| Data-Integrity | High | schema.sql 唯一键、outbox、init mode、migration runbook | 未执行备份还原 |
| Privacy | High | 敏感资料加密、IP 密文、导出脱敏注释、retention 搜索 | 未做法务/DPIA 审查 |
| Accessibility | Medium | Login 标签、部分 aria、无 axe CI | 未做键盘走查/屏幕阅读器 |
| Supply-Chain | Medium | ci.yml、Dockerfile 基镜像、lockfile 存在、无 SBOM | 未跑 npm/mvn audit |
| Cost | Medium | AI 超时、线程池、max-tokens、用户/IP 限流 | 无账单数据 |
| AI-Safety | High | prompts、grounding、RAG untrusted 标记、内部 RAG 接口 | 无 prompt-injection eval 套件 |
| Fallback | High | RULE_FALLBACK 标记、fallback provider 默认、辅导与单题讲解空 citation 已 fail-closed | fallback 指向同一上游未覆盖 |
| Testing-Authenticity | Medium | 集成测试与 WireMock 样例、前端测试是否进 CI | 未逐个判定全部测试真伪 |
| Type-Safety | Medium | TS `as any` 搜索、session JSON.parse、Java 校验注解 | 未跑 tsc 输出 |
| Frontend-State | Medium | 最大 TSX、useEffect 计数、api.ts 刷新 | 未做运行时渲染分析 |
| Backend-API | High | SecurityConfig 路由、公开问卷、internal、校验与错误码 | 未枚举全部 endpoint 契约差 |
| Dependency-Weight | Medium | package.json 双动画库、playwright 未用于测试 | 未测 bundle 体积 |
| Code-Consistency | Medium | 模块命名、filter 双份拷贝、VO/DTO 模式 | 未跑 formatter diff |
| Comment-Coverage | Medium | 过期 CLAUDE.md、schema 注释、TODO 搜索 | 未统计公开 API Javadoc 覆盖率 |

## 3. Top Risks

1. **研究导出脱敏开关不生效**（High，已修复）— `includeSensitiveFields=false` 时 XLSX 省略资料题原文。
2. **公开问卷加密密钥走仓库默认值**（High，已修复）— 非 `local`/`test` 拒绝默认密钥；compose / `.env.example` 已注入 HMAC 与 profile key。
3. **默认 compose 把数据面端口打到 0.0.0.0**（High，部分修复）— ai-gateway `8090` 已绑 loopback；MySQL/Rabbit/Postgres/app-server `8080` 仍映射全部网卡。
4. **内部 API token 无熵校验**（High，已修复）— 非 `local`/`test` 走 `SecretPolicy`，prod 禁止 `enabled=false`。
5. **X-Forwarded-For 无条件信任**（High，已修复）— 仅对可信代理 CIDR 解析转发头，否则用 socket IP。
6. **练习 complete() 计分错误**（High，已修复）— `answeredCount` 只计非空作答，与结果页对齐。
7. **词库批次导入事务是自调用空操作**（High，已修复）— 每行 `TransactionTemplate`，失败行回滚。
8. **grounding / 单题讲解 fail-open**（High，已修复）— 辅导与单题讲解空 citation / RAG 失败均降级 `RULE_FALLBACK`。
9. **学生答案被校验器当成可信事实**（High，已修复）— `studentAnswer` 进 untrusted 围栏，不得作为词义证据。
10. **CI 不跑前端测试，练习页零覆盖**（High，部分修复）— CI 已跑 `npm test`；练习页仍无 Vitest。

完整证据见第 4 节。

## 4. Detailed Findings

### Finding: 默认 compose 路径下问卷 PII 使用仓库内默认加密密钥

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: 公开研究问卷 / 敏感资料与 IP 加密
- Evidence:
  - File: `app-server/src/main/resources/application.yml:101-103`
  - Function / Module: `AssessmentParticipantProfileCipher` / `AssessmentParticipantCodeCodec`
  - Relevant behavior: **审计时** HMAC 与敏感资料密钥带本地默认值；profile cipher 用 SHA-256(密钥) 作为 AES 密钥。当时 `deploy/docker-compose.yml` 未传递 `APP_ASSESSMENT_CODE_HMAC_SECRET` / `APP_ASSESSMENT_SENSITIVE_PROFILE_KEY`，`.env.example` 也没有这两项。
- Problem: JWT 密钥有启动期熵校验，问卷加密密钥没有同等 fail-fast。按文档复制 `.env.example` 并用默认 compose 启动 prod profile 时，姓名、联系方式、访问 IP 都用公开默认密钥加密。
- Why it matters: 获得数据库备份的人可以用仓库里的默认密钥解密研究参与者 PII。这把“加密存储”变成了可逆混淆。
- Realistic failure scenario: 运维按 README 起 compose 且未叠加 production overlay；之后 MySQL 卷或备份流出，攻击者还原资料密文。
- Minimal fix: 与 JWT 一样在非 local/test 拒绝默认/短密钥；把两个变量写入 `.env.example` 和 `docker-compose.yml`；生产启动校验缺失即失败。
- Better long-term fix: 密钥纳入独立 secret store，支持 key version 轮换与历史密文重加密。
- Regression test suggestion: 以 prod profile、不设这两项环境变量启动 `app-server`，断言启动失败；另测默认密钥字符串被拒绝。
- Estimated effort: 4 小时
- Fix note: `SecretPolicy` 在非 `local`/`test` 拒绝仓库默认串；`AssessmentSecretValidator` 启动校验 HMAC 与 profile key；`deploy/docker-compose.yml` 与 `.env.example` 已注入两项变量。local/test 仍可用 YAML 默认值。未做历史密文轮换。

### Finding: 内部 API token 只检查非空，示例占位符即可调用网关

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: `/internal/**`（app-server 知识导出与 ai-gateway 全量 AI 接口）
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/common/security/InternalApiTokenAuthenticator.java:23-35`
  - Function / Module: `InternalApiTokenAuthenticator.validateConfiguration` / `authenticate`
  - Relevant behavior: **审计时** 仅要求 token 非空，无最小长度、无占位符拒绝、无熵检查。`PLATFORM_INTERNAL_API_ENABLED` 为 false 时直接放行。当时 `deploy/docker-compose.yml` 将 MySQL `3306`、RabbitMQ `5672/15672`、Postgres `5432`、ai-gateway `8090`、app-server `8080` 映射到全部网卡。
- Problem: JWT 有 32 字符+熵校验，内部 token 没有。示例文件里的占位符能通过启动检查。拿到该共享密钥即可触发 chat、RAG、reindex、配置 apply。
- Why it matters: 内部接口是最高权限机器身份。弱共享密钥等于把 LLM 账单和知识库操作暴露给能访问 8090 的人。
- Realistic failure scenario: 复制 `.env.example` 后只改了 JWT；8090 对实验室网段开放；有人用示例 token 打 `/internal/ai/chat` 或 `/internal/ai/rag/reindex`。
- Minimal fix: 复用 `JwtSecretValidator` 同类规则；compose 默认不映射 8090 到 `0.0.0.0`；禁止 `enabled=false` 出现在 prod。
- Better long-term fix: mTLS 或独立服务账号，按接口最小权限拆分 token。
- Regression test suggestion: 占位符/短 token 在 prod profile 启动失败；enabled=false + prod 启动失败。
- Estimated effort: 3 小时
- Fix note: app-server 与 ai-gateway 的 `InternalApiTokenAuthenticator` 在非 `local`/`test` 走 `SecretPolicy`，并禁止 `enabled=false`。默认 compose 将 ai-gateway 映射为 `127.0.0.1:${AI_GATEWAY_PORT}:8090`。MySQL/Rabbit/Postgres/8080 全网卡暴露仍是后续 Finding。

### Finding: 通知 WebSocket 把 access token 放进 URL 查询参数

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: `/ws/notifications`
- Evidence:
  - File: `src/lib/notifications.ts:16`
  - Function / Module: `buildNotificationWebSocketUrl`
  - Relevant behavior: **审计时** `wsUrl.searchParams.set('access_token', accessToken)`。服务端从 query 读取该参数。握手本身会校验 JWT 与黑名单，但传输位置不安全。
- Problem: query string 会出现在 nginx/access 日志、浏览器历史、某些 APM 和 Referer。Access token TTL 默认 30 分钟。
- Why it matters: 日志系统通常比应用日志保留更久、权限更宽。一条 WS 升级日志就能变成会话劫持材料。
- Realistic failure scenario: 前端 nginx 或宿主机反代记录完整 URI；运维或攻击者从日志取出 token，调用 `/api/student/**`。
- Minimal fix: 仅用 `Authorization` 头或 WS 首条消息传 token；删除 query 回退；同步改前端。
- Better long-term fix: 短时单次 ticket（一次性 WS 票据）。
- Regression test suggestion: 拦截器拒绝仅带 query token 的握手；前端单测断言 URL 不含 `access_token`。
- Estimated effort: 3 小时
- Fix note: 前端 URL 不再带 token；`NotificationBell` 在 `onopen` 发送 `{type:AUTH,accessToken}`。拦截器拒绝 query `access_token`，仍接受 `Authorization` 头。未鉴权握手 5 秒超时关闭。一次性 WS ticket 未做。

### Finding: 任意已登录用户可以读取 Actuator metrics 与 prometheus

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: app-server 管理端点
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/common/config/SecurityConfig.java:56-67,83`
  - Function / Module: `appSecurityFilterChain`
  - Relevant behavior: **审计时** 仅 `/actuator/health` permitAll；`/actuator/info` 等落入 `anyRequest().authenticated()`。集成测试只断言 info 需登录，未限制角色。
- Problem: 学生或教师 JWT 即可拉 Prometheus 与 metrics，观察流量、错误率、JVM、可能的自定义业务指标。
- Why it matters: 指标能辅助探测和容量推断；也不应成为低权限用户的侦察面。
- Realistic failure scenario: 学生打开 `/actuator/prometheus` 保存全文，用于推断在线人数或 AI 调用量。
- Minimal fix: `/actuator/**` 除 health 外要求 `ADMIN`，或把 management 绑到 localhost 独立端口（ai-gateway 已这样做）。
- Better long-term fix: 指标网关鉴权 + 网络策略。
- Regression test suggestion: 学生 token 访问 `/actuator/prometheus` 期望 403；管理员 200。
- Estimated effort: 2 小时
- Fix note: `/actuator/health` 仍匿名；其余 `/actuator/**` 要求 `ADMIN`。未拆 management 独立端口。

### Finding: Grounding 校验在空 citationIds 时直接返回通过

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: AI 辅导 / 诊断解释 / 训练推荐
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/ai/service/AiInsightService.java:923-925`
  - Function / Module: `verifyGuidanceGrounding`
  - Relevant behavior: **审计时** `grounding == null || payload.citationIds().isEmpty()` 时 `return true`。RAG 失败传入 `null` grounding 后二次 LLM 校验被跳过，成功路径仍标 `GENERATION_SOURCE_AI`。
- Problem: 模型只要不填 citation 就能绕过 grounding。这与“词义必须有证据”的产品声明相反。
- Why it matters: 教学场景下未支撑的词义会被当成 AI 结论；研究/辅导报告可能把幻觉标成已校验。
- Realistic failure scenario: 练习辅导模型返回解释且 `citationIds: []`；服务当作 grounded AI 写回 `tutoring_json`，学生页展示为 AI 来源。
- Minimal fix: 空 citation 视为校验失败并走 `RULE_FALLBACK`；对必须引用的场景拒绝空列表。
- Better long-term fix: 确定性核对：每个事实句映射到 chunk，而不是再问一次模型“算不算支撑”。
- Regression test suggestion: 构造无 citation 的 structured 输出，断言 generationSource 为 `RULE_FALLBACK` 且不把结果标成 AI。
- Estimated effort: 4 小时
- Fix note: `verifyGuidanceGrounding` 在 `grounding == null` 或空 `citationIds` 时失败并走 `RULE_FALLBACK`。单题讲解 grounding 见后续 Finding（已于同日修复）。

### Finding: AI 调用无用户配额，且网关超时与反向代理不一致

- Severity: High
- Confidence: High
- Category: Performance
- Status: Confirmed
- Remediation: Fixed / 已完成 (2026-08-13)
- Affected area: `/api/ai/**`、异步 job、ai-gateway
- Evidence:
  - File: `app-server/src/main/resources/application.yml:209`
  - Function / Module: **审计时** `integration.ai.read-timeout` 默认 `PT10M`；`AiAsyncConfiguration.java:17-19` 核心 2 / 最大 4 / 队列 32，默认 AbortPolicy；`deploy/frontend/nginx.conf:36-44` 对 `/api/ai/` 读超时 180s。当时 Auth 限流只覆盖登录注册，未见 AI 用户配额。
- Problem: 登录学生可并发打昂贵 RAG/structured chat。nginx 180 秒断开后，app-server 仍可能把对网关的调用撑到 10 分钟，线程与 token 继续消耗。
- Why it matters: 账单与线程池会在正常课堂并发下被打满，表现为全校 AI 功能失败，而不是单个用户 429。
- Realistic failure scenario: 一个班同时点“AI 讲解”；队列 32 满后新 job 失败；部分请求 nginx 已 504，上游仍在计费。
- Minimal fix: 按用户/IP 对 `/api/ai/**` 限流；把 `AI_GATEWAY_READ_TIMEOUT` 降到与 nginx 一致（例如 180s）；异步池设明确拒绝策略与指标。
- Better long-term fix: 日配额、token 预算、按场景并发上限，以及取消已断开客户端的上游调用。
- Regression test suggestion: 超配额返回 429；超时配置与 nginx 文档/测试对齐。
- Estimated effort: 1 天
- Fix note: POST `/api/ai/**` 与 `/api/teacher/intervention-suggest` 走 Bucket4j（用户 30/10min、IP 200/10min）；`AI_GATEWAY_READ_TIMEOUT` 默认 PT180S；nginx 为教师干预单独 180s。GET job 轮询不计费。未做日配额/取消上游。

### Finding: 公开问卷验证限流存在进程内存且 IP 键不回收

- Severity: High
- Confidence: High
- Category: Stability
- Status: Confirmed
- Remediation: Fixed / 已完成 (2026-08-13)
- Affected area: `PublicAssessmentService.verify` / `enterByQr`
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/service/PublicAssessmentService.java:120,1208-1218`
  - Function / Module: `verificationAttempts` / `enforceRateLimit`
  - Relevant behavior: **审计时** `ConcurrentHashMap<String, Deque<LocalDateTime>>` 按 IP 计数。窗口内会弹出过期时间戳，但空 deque **不会从 map 删除**。多实例不共享。进程重启限流归零。当时登录接口已用 Redis Bucket。
- Problem: 公开入口的爆破防护弱于登录。水平扩展后每实例独立计数。扫描大量伪造 IP 会让 map 只增不减。
- Why it matters: 研究问卷是未登录攻击面；限流失效等于参与码可被离线猜（即便空间大，仍失去减速），内存会随独立 IP 增长。
- Realistic failure scenario: 两个 app-server 副本后，攻击速率翻倍；或爬虫用大量 IP，map 持续膨胀。
- Minimal fix: 改用现有 Redis 限流组件；定期 remove 空 deque；多实例必须共享。
- Better long-term fix: 与登录同一套 bucket 配置，并监控限流拒绝指标。
- Regression test suggestion: 超限 429；验证过期后 map 不含该 IP；文档或测试说明 Redis 后端。
- Estimated effort: 4 小时
- Fix note: 删除进程内 map。verify/QR 走共享 `RateLimitBucketResolver`（Redis idle TTL，test 为本地 Bucket）。键 `assessment:rl:verify:ip:` 与 `assessment:rl:qr-entry:ip:`。XFF 伪造已于同日后续 Finding 修复。

### Finding: 生产库结构变更没有版本化迁移，存在毁库重建指引

- Severity: High
- Confidence: High
- Category: Release
- Status: Confirmed
- Remediation: Fixed / 已完成 (2026-08-13) — 最小修复（文档 + ddl 约定 + restore）；未引入 Flyway
- Affected area: MySQL 与 pgvector schema
- Evidence:
  - File: `app-server/src/main/resources/schema.sql:3`
  - Function / Module: 快照建表；`application-prod.yml:5-7` `spring.sql.init.mode=never`；`CLAUDE.md` 开发流程为改 DDL、清空数据库、重启；`docs/db-migration-runbook.md` 要求已有环境手工 DDL 且不要删卷。`CREATE TABLE` 无 `IF NOT EXISTS`。备份脚本有，还原脚本无。
- Problem: 开发文档鼓励 drop database。生产靠人工对比快照写一次性 DDL。两套说法并存，新同事容易在有数据的环境执行清空。
- Why it matters: 研究答卷、练习记录、词库一旦被 compose `down -v` 或 drop 就不可恢复（除非备份刚做过且能还原）。
- Realistic failure scenario: schema 加列后有人按 CLAUDE.md 清空生产同构环境；或生产只改了 Java 实体忘了手工 DDL，写入失败/脏数据。
- Minimal fix: CLAUDE.md 明确禁止在非空库使用 snapshot reset；每次 schema 变更附带 `docs/ddl/` 一次性向前脚本；补 restore 演练。
- Better long-term fix: 重新引入版本化迁移（Flyway）仅用于生产，快照仍可给测试用。
- Regression test suggestion: CI 对比 schema.sql 与上一 tag 的 diff，要求同时存在 ddl 文件；restore 脚本在测试环境跑通。
- Estimated effort: 2 天
- Fix note: 新增 `docs/ddl/` 与 `restore-mysql.sh` / `restore-postgres.sh`（需 `CONFIRM_RESTORE=YES`）。根/模块 CLAUDE 与 runbook 写明仅 local 空库可 `down -v`。未引入 Flyway，未补历史 DDL。

### Finding: CI 不执行前端测试，文档还声称没有前端测试

- Severity: High
- Confidence: High
- Category: Testing
- Status: Confirmed
- Remediation: Fixed / 已完成 (2026-08-13) — CI 已跑 `npm test`；练习页仍无专项 Vitest
- Affected area: `.github/workflows/ci.yml` 与前端质量门禁
- Evidence:
  - File: `.github/workflows/ci.yml:10-27`
  - Function / Module: `frontend` job
  - Relevant behavior: 只跑 `check:contracts`、lint、typecheck、build。`package.json` 有 `test: vitest run`，`src/` 下至少 46 个测试文件。`src/CLAUDE.md:73` 写“当前无前端自动化测试”。`src/` 测试中无 `practice` 匹配，而 `src/pages/practice/index.tsx` 有 1253 行。后端 job 是 `./mvnw -B -ntp test`，未加文档要求的 Testcontainers `api.version`。
- Problem: 公开问卷、登录刷新、练习页等前端回归不会挡住 PR。文档会让后续贡献者继续不跑测试。
- Why it matters: 前端是研究问卷与教学的真实入口；CI 绿不代表 Vitest 绿。
- Realistic failure scenario: 破坏 `api.ts` 刷新逻辑的 PR 通过 typecheck 合并，生产出现循环登录失败。
- Minimal fix: frontend job 加 `npm test`；修正 `src/CLAUDE.md`；backend 加 `-Dapi.version=1.44`（或锁定 docker-java）。
- Better long-term fix: 关键路径 Playwright smoke（现依赖已在 package.json 却未用于测试）。
- Regression test suggestion: 故意失败的 Vitest 应让 CI 变红（在修复后用独立分支验证）。
- Estimated effort: 2 小时
- Fix note: frontend job 在 typecheck 后跑 `npm test`；backend 加 `-DargLine=-Dapi.version=1.44`。`src/CLAUDE.md` 与 README 已去掉「无前端测试」和 Flyway，文档链接改为相对路径。练习页 Vitest / Playwright 未做。

### Finding: 全局关闭 CSRF，但公开问卷使用 Cookie 会话

- Severity: Medium
- Confidence: High
- Category: Security
- Status: Confirmed
- Affected area: `/api/public/assessments/**`
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/common/config/SecurityConfig.java:48,63`
  - Function / Module: `appSecurityFilterChain`
  - Relevant behavior: `csrf.disable()`。公开问卷 `PublicAssessmentController` 设置 httpOnly、SameSite=Lax、path 限定的 `LEXIBRIDGE_SESSION` cookie。token 不在 JSON 里。生产 overlay 打开 `APP_CORS_ALLOW_CREDENTIALS=true`。
- Problem: SameSite=Lax 能挡住多数跨站 POST，但 CSRF 保护被整体关掉。同源子域、浏览器不一致、或未来改 SameSite 都会回到经典 CSRF。
- Why it matters: 公开问卷 cookie 能保存/提交答卷与上传文件。跨站伪造提交会污染研究数据。
- Realistic failure scenario: 参与者登录问卷后访问恶意页；若 cookie 被当作 same-site 发送，攻击者 POST responses。
- Minimal fix: 对 cookie 会话端点启用 CSRF token 或自定义 header 校验；保持 JWT 无 cookie 的 API 不需要 CSRF。
- Better long-term fix: 双重提交或 `SameSite=Strict` + 显式 CSRF。
- Regression test suggestion: 无 CSRF/自定义头的跨站形态 POST 应 403。
- Estimated effort: 1 天

### Finding: 登录 JWT 存在 localStorage，XSS 即可窃取会话

- Severity: Medium
- Confidence: High
- Category: Security
- Status: Confirmed
- Affected area: 前端会话
- Evidence:
  - File: `src/lib/session.ts:12-28`
  - Function / Module: `readStoredSession` / `writeStoredSession`
  - Relevant behavior: `ef-transfer-session` 写入 localStorage，含 access 与 refresh。公开问卷已改用 httpOnly cookie，登录会话没有。
- Problem: 任意 XSS（依赖、markdown、未来 HTML 渲染）都能读 refresh token（默认 7 天）。
- Why it matters: 教师/管理员会话能看班级与部分研究数据。
- Realistic failure scenario: 依赖 XSS 后脚本读取 localStorage 并回传。
- Minimal fix: refresh 改 httpOnly cookie；access 内存或短 cookie。
- Better long-term fix: BFF 模式，浏览器不持有 refresh。
- Regression test suggestion: 登录响应不把 refresh 放进可由 JS 读取的存储。
- Estimated effort: 1 天

### Finding: 研究附件“安全扫描通过”实际只是魔数与扩展名检查

- Severity: Medium
- Confidence: High
- Category: Security
- Status: Confirmed
- Affected area: `ResearchFileService.uploadContent`
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/service/ResearchFileService.java:124-144`
  - Function / Module: `uploadContent`
  - Relevant behavior: 可执行/脚本头拒绝后，`detectMime` 成功就把 `scanStatus` 设为 `CLEAN`。错误文案是 “security scan”。未见杀毒/内容沙箱。
- Problem: 教师下载“已扫描清洁”的附件时，得到的是类型检查而非恶意软件扫描。
- Why it matters: 研究上传允许的办公文档仍可带宏或漏洞利用。产品语义过度承诺。
- Realistic failure scenario: 参与者上传带宏的允许扩展名文件；教师按 CLEAN 本地点开。
- Minimal fix: 状态改为 `SIGNATURE_OK` 或接入真实扫描；UI 不要写“病毒扫描通过”。
- Better long-term fix: 异步杀毒 + 沙箱预览。
- Regression test suggestion: 上传合法 PDF 断言状态名不再叫 CLEAN，或文档/API 明确语义。
- Estimated effort: 3 小时

### Finding: 研究 PII、IP 密文和附件没有保留期限与删除级联

- Severity: Medium
- Confidence: High
- Category: Release
- Status: Confirmed
- Affected area: 研究问卷治理
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/support/AssessmentParticipantProfileCipher.java:28-41`
  - Function / Module: 加密存储路径；全库搜索 `retention` / `gdpr` / `purge` 无实现。`ResearchFileService` 有孤儿附件定时清理（`orphan-after`），没有参与者资料/IP 访问日志 TTL。
- Problem: 姓名、联系方式、加密 IP、答卷、附件会无限期留在 MySQL 与对象存储。导出脱敏不能替代删除权。
- Why it matters: 公开研究问卷明确收集联系方式。无限期保留放大泄露影响面。
- Realistic failure scenario: 课题结束后数年数据库仍能解密历史参与者联系方式。
- Minimal fix: 配置保留期；到期删除或再加密销毁密钥；文档写清。
- Better long-term fix: 参与者删除/导出 API，并清理 RAG/日志派生数据。
- Regression test suggestion: 过期资料任务把 ciphertext 清空且文件对象删除。
- Estimated effort: 2 天

### Finding: 多个核心文件远超 1000 行，变更半径过大

- Severity: Medium
- Confidence: High
- Category: Maintainability
- Status: Confirmed
- Affected area: 前端配置中心、契约层、问卷与 AI 服务
- Evidence:
  - File: `src/pages/admin/ConfigCenter.tsx:1-4211`
  - Function / Module: 行数统计（排除 vendor）：`ConfigCenter.tsx` 4211、`src/lib/contracts.ts` 3481、`AssessmentService.java` 1940、`AiInsightService.java` 1749、`TrainingSessionService.java` 1671、`PublicAssessmentService.java` 1295、`practice/index.tsx` 1253
- Problem: 单文件承担 UI、校验、密钥编辑、运维操作或多个业务用例。原则 1.1/1.2 违规。
- Why it matters: 评审无法完整阅读；回归测试难选；冲突频繁。
- Realistic failure scenario: 改 RAG 参数 UI 误碰 provider 密钥提交逻辑，生产写坏 AI 配置。
- Minimal fix: 先拆 ConfigCenter 按 tab；contracts 按域拆文件（已有 generated 可继续扩）。
- Better long-term fix: Assessment/AI 服务按用例拆类。
- Regression test suggestion: 拆分后 ConfigCenter 现有测试仍通过。
- Estimated effort: 1 周（分批）

### Finding: 文档与真实质量门禁、schema 策略不一致

- Severity: Medium
- Confidence: High
- Category: Maintainability
- Status: Confirmed
- Remediation: Fixed (2026-08-13) — `.env.example` 问卷密钥、Flyway 残留、「无前端测试」与绝对路径已改；schema 清空库指引已限定 local
- Affected area: README / 模块 CLAUDE.md / 环境模板
- Evidence:
  - File: `src/CLAUDE.md:73`
  - Function / Module: 文档
  - Relevant behavior: 写“当前无前端自动化测试”，与 46 个测试文件相反。`README.md` 仍提 `spring.flyway.baseline-on-migrate=false`，仓库已无 Flyway。部分文档链接写成宿主机绝对路径。**审计时** `.env.example` 未列出问卷加密密钥（2026-08-13 已补）。CLAUDE.md 开发期清空库与 `docs/db-migration-runbook.md` 冲突。
- Problem: 发布与交接会按过期文档操作。
- Why it matters: 错误的“没有测试”和 “flyway” 会直接导致漏测或误迁移。
- Realistic failure scenario: 新成员不跑 `npm test`；或在生产找 flyway 配置。
- Minimal fix: 改这三处文档；`.env.example` 密钥项已补，仍须去掉 Flyway / “无前端测试”。
- Better long-term fix: CI 检查 README 关键命令与真实脚本一致。
- Regression test suggestion: 文档链接/关键词检查（无 flyway、有 npm test）。
- Estimated effort: 1 小时
- Fix note: README 去掉 Flyway 并改为相对文档链接；`src/CLAUDE.md` 写明 Vitest；CLAUDE/runbook/`docs/ddl` 对齐已有环境禁止删卷。

### Finding: 有 metrics 和 health，没有告警与事故 runbook

- Severity: Medium
- Confidence: High
- Category: Release
- Status: Confirmed
- Affected area: 可观测性 / 运维
- Evidence:
  - File: `app-server/src/main/resources/application.yml:42-57`
  - Function / Module: Spring Actuator
  - Relevant behavior: 暴露 health/info/metrics/prometheus（2026-08-13 起非 health 需 ADMIN），日志带 traceId，compose healthcheck 打 `/actuator/health`。仓库内无 Alertmanager/规则/on-call。`docs/db-migration-runbook.md` 覆盖 DDL，不覆盖 AI 熔断、问卷超时、outbox 积压。
- Problem: 生产可以挂死很久直到人工发现 AI 或问卷异常。
- Why it matters: 研究窗口是限时的；AI 账单异常也需要尽快发现。
- Realistic failure scenario: outbox 卡住导致 RAG 不更新，prometheus 有数据但无人看。
- Minimal fix: 至少告警：实例 down、outbox 积压、AI 错误率、问卷 5xx。写一页 runbook。
- Better long-term fix: SLO + 追踪后端。
- Regression test suggestion: 契约测试保证关键业务 metrics 名称稳定。
- Estimated effort: 1 天

### Finding: CI 权限未收口，镜像未钉 digest，无 SBOM

- Severity: Medium
- Confidence: High
- Category: Release
- Status: Confirmed
- Affected area: 供应链
- Evidence:
  - File: `.github/workflows/ci.yml:1-39`
  - Function / Module: CI
  - Relevant behavior: 未设 `permissions:`（默认偏宽）。Dockerfile 使用浮动标签 `eclipse-temurin:25-jdk`、`nginx:1.27-alpine`、`node:22-bookworm`。无 SBOM、无签名、无 dependency audit 步骤。`package-lock.json` 与 Maven wrapper 存在，可复现性中等。
- Problem: 供应链基线低于对外研究平台应有水平。
- Why it matters: 浮动 tag 重建可能换 JDK；CI token 权限过宽扩大 workflow 被改写后的影响。
- Realistic failure scenario: 上游 tag 被替换或 CI 被投毒后 checkout 权限过大。
- Minimal fix: `permissions: contents: read`；钉 digest；加 `npm audit`/`osv-scanner` 非阻塞起步。
- Better long-term fix: SBOM + 镜像签名。
- Regression test suggestion: workflow 文件断言包含 `permissions:`。
- Estimated effort: 4 小时

### Finding: AI fallback provider 默认回落到同一上游

- Severity: Medium
- Confidence: High
- Category: Stability
- Status: Confirmed
- Affected area: ai-gateway bootstrap
- Evidence:
  - File: `ai-gateway/src/main/resources/application.yml:101-110`
  - Function / Module: `ai.provider.providers.deepseek`
  - Relevant behavior: fallback 的 URL/key/model 默认 `${AI_CHAT_*}`。README 已警告。Compose 同样用 `:-${AI_CHAT_*}` 回退。配置中心会 warning，但运行时仍会“failover”到同一供应商。
- Problem: 主供应商故障时所谓 fallback 不会提高可用性，只增加一次失败。
- Why it matters: 运维以为有独立第二通道，实际没有。
- Realistic failure scenario: 主 chat 上游 5xx；网关切到 fallback，请求打到同一 URL，课堂辅导连续失败，监控却显示“已 failover”。
- Minimal fix: prod 启动时若 fallback 与 active 解析到同一 host+key+model 则 fail 或禁用 fallback。
- Better long-term fix: 强制独立 `AI_FALLBACK_*`。
- Regression test suggestion: 相同上游时 health/config validate 报 error 而非仅 warning。
- Estimated effort: 3 小时

### Finding: CI 未设置 Testcontainers Docker API 版本，ai-gateway 测试可能被跳过或失败

- Severity: Medium
- Confidence: Medium
- Category: Testing
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: `.github/workflows/ci.yml` backend job
- Evidence:
  - File: `.github/workflows/ci.yml:38-39`
  - Function / Module: `./mvnw -B -ntp test`
  - Relevant behavior: CLAUDE.md 写明宿主机 Docker ≥26 时需 `-Dapi.version=1.44`，否则 docker-java 默认 1.32 被拒绝。CI 未传该参数。本审计未实际跑完整 Maven 测试，故标 Suspected。
- Problem: pgvector 集成测试是 RAG 正确性的主要网，CI 可能没跑到或红得不稳定。
- Why it matters: schema 维度/HNSW 守卫可能只在本地被验证。
- Realistic failure scenario: GitHub runner Docker API 拒绝协商，相关测试直接失败或被环境跳过。
- Minimal fix: 在 backend job 加文档中的 argLine。
- Better long-term fix: 升级 testcontainers/docker-java。
- Regression test suggestion: CI 日志断言 Testcontainers 容器成功启动。
- Estimated effort: 1 小时
- Fix note: backend job 已加 `-DargLine=-Dapi.version=1.44`。未在 CI 日志中断言容器启动。

### Finding: 登录会话边界用断言代替 schema 校验

- Severity: Medium
- Confidence: High
- Category: Maintainability
- Status: Confirmed
- Affected area: 前端 session 水合
- Evidence:
  - File: `src/lib/session.ts:17`
  - Function / Module: `readStoredSession`
  - Relevant behavior: `JSON.parse(raw) as LoginResponse`。catch 只处理 JSON 语法错误，不验证字段。`src/pages/admin/ConfigCenter.tsx` 等处还有 `as unknown as`。全前端几乎无 `as any`，整体类型纪律尚可。
- Problem: 损坏或过期的 session 形态会以错误形状进入 auth store，直到某次属性访问才爆。
- Why it matters: 升级 LoginResponse 后旧 localStorage 会导致“半登录”状态。
- Realistic failure scenario: 发版增加必填字段后，老用户一直卡在异常首页。
- Minimal fix: 用 zod 校验 LoginResponse，失败则 clear。
- Better long-term fix: 会话版本号。
- Regression test suggestion: 缺字段的 localStorage 被清除且不崩溃。
- Estimated effort: 2 小时

### Finding: 练习拼写输入没有可访问名称，选项也不是 radio 语义

- Severity: Medium
- Confidence: High
- Category: Testing
- Status: Confirmed
- Affected area: `/practice` 与公开问卷表单
- Evidence:
  - File: `src/pages/practice/index.tsx:760-768`
  - Function / Module: PracticePage 拼写输入
  - Relevant behavior: `<input type="text">` 只有 placeholder，没有 `label` / `htmlFor` / `aria-label`。选择题是普通 `button`，没有 `radiogroup` / `aria-checked`。Login 有 `htmlFor` 与 `aria-invalid`。公开问卷参与码错误是独立 `role="alert"`，未用 `aria-describedby` 挂到输入框。无 axe CI。
- Problem: 核心学生练习流对辅助技术不友好；公开问卷错误也没绑到控件。
- Why it matters: 拼写题是练习主路径。无障碍缺陷会让部分学生无法独立完成。
- Realistic failure scenario: 读屏用户听到一串无标签文本框，无法把题干和输入对应起来。
- Minimal fix: 拼写框加 `sr-only` label；选项用 radio/group；参与码错误加 `aria-describedby`。
- Better long-term fix: 统一 Field 组件；CI 对 Login/练习/问卷加 axe。
- Regression test suggestion: 练习页每个拼写 input 有可访问名称；校验失败时 invalid 输入有描述 id。
- Estimated effort: 1 天

### Finding: Playwright 与双动画库增加依赖重量但未形成测试资产

- Severity: Low
- Confidence: High
- Category: Release
- Status: Confirmed
- Affected area: 前端依赖
- Evidence:
  - File: `package.json:18-41,59`
  - Function / Module: dependencies / devDependencies
  - Relevant behavior: 同时依赖 `gsap`+`@gsap/react` 与 `framer-motion`。`playwright` 在 devDependencies，源码测试未引用，仅可能被手工脚本使用。`html2canvas`+`jspdf` 用于 PDF。
- Problem: 安装与供应链表面积大于实际自动化收益。
- Why it matters: CI 每次 `npm ci` 拉 Playwright 浏览器依赖（若安装钩子触发）会变慢；两个动画栈增加包体积。
- Realistic failure scenario: 依赖 CVE 出现在从未跑过的 Playwright 树上，仍然要应急升级。
- Minimal fix: 若无 E2E，移出 playwright 或真正接入 smoke；动画库选一个主栈。
- Better long-term fix: bundle 预算。
- Regression test suggestion: 依赖审查列表与 bundle analyze 基线。
- Estimated effort: 4 小时

### Finding: 研究导出 includeSensitiveFields 只做权限检查，写出内容从不脱敏

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed / 已完成 (2026-08-13)
- Affected area: 教师研究数据导出
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/service/ResearchExportService.java:108-126,245-254`
  - Function / Module: `createExport` / `processJobInternal` / `writeResearchXlsx`
  - Relevant behavior: 创建 job 时校验 `includeSensitiveFields` 权限并落库。处理 job 时 `writeResearchXlsx(publishId, filter)` 与 `writeCsv(...)` 都不读取该标志。`ResearchExportWorkbook` 说明写明「资料汇总」含姓名、联系方式。
- Problem: 产品有「非敏感导出」开关，实际 XLSX 仍展开资料题。CSV 摘要不含姓名，但默认/常用 XLSX 路径会泄露。
- Why it matters: 教师以为关掉敏感字段就合规，把文件发给助教或外发分析，等于明文交出联系方式。
- Realistic failure scenario: 课题负责人按 UI 导出「不含敏感字段」的 xlsx，资料汇总表仍有姓名和手机。
- Minimal fix: `writeResearchXlsx` / 资料汇总 / 宽表资料列按 `job.getIncludeSensitiveFields()` 剥离或打码。
- Better long-term fix: 导出流水线单一 redaction 函数，单测覆盖 false/true 两种文件内容。
- Regression test suggestion: `includeSensitiveFields=false` 的 xlsx 不得出现资料题原文；true 且无权限创建 job 仍 403。
- Estimated effort: 4 小时
- Fix note: `writeResearchXlsx` 读取 job 标志。false 时资料汇总只写省略说明，宽表/逐题明细/题目说明去掉非正式题。非 owner `true` 仍 403。未加 UI 开关。

### Finding: 无条件信任客户端 X-Forwarded-For

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: 公开问卷限流与访问日志
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/common/security/ClientRequestContextResolver.java:22-28`
  - Function / Module: `resolveIpAddress`
  - Relevant behavior: **审计时** 只要存在 `X-Forwarded-For` 就取第一跳，不校验是否来自可信代理。当时 `PublicAssessmentService.verify` 用该 IP 做内存限流。前端 nginx 也优先采用客户端 XFF。
- Problem: 直连 app-server 或未正确覆盖 XFF 的反代时，攻击者可随意换 IP。
- Why it matters: 公开问卷验证限流本来就弱；再叠加可伪造 IP 等于没有减速。
- Realistic failure scenario: 对 `8080` 直接 POST verify，每次换 `X-Forwarded-For`，绕过 10 次/10 分钟限制。
- Minimal fix: 仅对已知代理 CIDR 解析转发头，否则用 `remoteAddr`；nginx 用真实连接 IP 覆盖 XFF。
- Better long-term fix: Spring ForwardedHeaderFilter + 明确 `server.forward-headers-strategy` 与可信代理列表。
- Regression test suggestion: 无代理时带伪造 XFF 的请求，限流 key 必须是 socket 地址。
- Estimated effort: 3 小时
- Fix note: `ClientRequestContextResolver` 仅当 `remoteAddr` 落在 `app.security.trusted-proxy.cidrs`（默认 loopback + RFC1918/Docker）时解析 XFF/`X-Real-IP`，非法头回退 socket。前端 nginx 用 `set_real_ip_from` + `real_ip_header` 后以 `$remote_addr` 覆盖转发头。prod `forward-headers-strategy` 改为 `native`。测试：非代理 socket + 伪造 XFF 必须用 socket。LAN 直连 RFC1918 仍可伪造，取决于 compose 端口暴露。

### Finding: 练习 complete() 把请求条数当成已答数，结果页与落库口径不同

- Severity: High
- Confidence: High
- Category: Stability
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: 学生自测练习评分
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/practice/service/PracticeSessionService.java:229-253,282-287`
  - Function / Module: `complete` / `getResult`
  - Relevant behavior: **审计时** 空作答会把 `isCorrect/answeredAt` 置空，但 `answeredCount = request.answers().size()` 仍计入空白项。`getResult()` 按 `answeredAt != null` 重算。未提交的题保留草稿。`complete()` 返回值未检查更新行数。
- Problem: 历史进度、辅导统计、RAG 上下文可能吃到虚高已答数；结果页刷新后又变一版数字。
- Why it matters: 练习正确率是后续错词再练和辅导报告的输入。
- Realistic failure scenario: 前端提交整卷含大量空字符串；库里 answered_count=239，结果页只算真正有内容的题。
- Minimal fix: 只统计非空作答；未出现在 payload 的题明确清空或保留需文档化；`complete` 更新行数必须为 1。
- Better long-term fix: 计数只从 answer 表派生，禁止双写摘要字段。
- Regression test suggestion: 提交含空白项的整卷，断言 answered_count 等于非空题数，且 getResult 与之相同。
- Estimated effort: 4 小时
- Fix note: `complete()` 循环内只累计非空 `answered`；`mapper.complete` 更新行数必须为 1，否则 409。`PracticeSessionFlowIntegrationTest.completeCountsOnlyNonBlankAnswersAndMatchesResultPage` 覆盖空白提交与结果页一致。

### Finding: 练习开局未把唯一键冲突映射为 409

- Severity: Medium
- Confidence: High
- Category: Stability
- Status: Confirmed
- Affected area: `PracticeSessionService.createSession`
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/practice/service/PracticeSessionService.java:76-118`
  - Function / Module: `createSession`
  - Relevant behavior: 先 `hasActiveSession` 再 insert。schema 有 `uk_practice_session_active_owner`。诊断/训练捕获 `DataIntegrityViolationException` 转 409；练习模块没有。
- Problem: 双击或双标签会撞唯一键，变成 500 而不是 `ACTIVE_SESSION_EXISTS`。
- Why it matters: 数据仍被 UK 保护，但学生看到内部错误，前端也无法走「恢复已有会话」。
- Realistic failure scenario: 网络慢时连点开始练习，一个成功一个 500。
- Minimal fix: 与 `DiagnosisSessionService` 一样捕获唯一键冲突并返回 409。
- Better long-term fix: 统一 SessionStart 模板。
- Regression test suggestion: 并发两次 create，一次 201、一次 409，无 500。
- Estimated effort: 2 小时

### Finding: 词库批次导入的 @Transactional 因自调用不生效

- Severity: High
- Confidence: High
- Category: Stability
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: `LexicalImportBatchService.importBatch`
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/service/LexicalImportBatchService.java:394-438`
  - Function / Module: `importBatch` / `processReadyRow`
  - Relevant behavior: **审计时** `importBatch` 无事务，循环同对象调用带 `@Transactional` 的 `processReadyRow`。Spring 代理不会切入。`createFromImport` 可能已提交，行状态更新与批次 FAILED 标记不在同一事务。对比 `LexicalPairService.importCsv` 使用 `TransactionTemplate`。
- Problem: 中途崩溃会留下已导入词对、行仍 READY、批次 FAILED，并可能漏发或错发知识变更事件。
- Why it matters: 词库是 RAG 源。脏导入会让检索和教学数据不一致。
- Realistic failure scenario: 第 80 行创建词对后进程被杀；重跑重复或跳过，RAG 只索引了部分词。
- Minimal fix: 每行用 `TransactionTemplate`（与 CSV 路径一致），成功后再发事件。
- Better long-term fix: 行级幂等键 + outbox 与批次状态同一事务。
- Regression test suggestion: mock 第 N 行失败，断言已成功行提交、失败行未提交、批次状态与事件与之一致。
- Estimated effort: 1 天
- Fix note: 去掉行上无效 `@Transactional`。每行 `TransactionTemplate` 包裹 `createFromImport` + 行状态；异常回滚该行后另开事务标 `INVALID`。全部成功后再 `publishKnowledgeChangedEvent`。`LexicalImportBatchIntegrationTest.shouldKeepSuccessfulRowsAndMarkDuplicateRowInvalidWithoutOrphanPairs` 覆盖第 N 行冲突。

### Finding: 练习单题讲解不走 grounding 校验仍标记为 AI

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: `explainPracticeQuestion`
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/ai/service/AiInsightService.java:437-469`
  - Function / Module: `explainPracticeQuestion`
  - Relevant behavior: **审计时** 只要求 `explanation` 非空，过滤 citation 后直接 `GENERATION_SOURCE_AI`。不调用 `AiResponseValidator` / `verifyGuidanceGrounding`。整卷辅导走二次校验，单题路径没有。
- Problem: 每题「AI 讲解」是高点击同步接口，也是幻觉出口。
- Why it matters: 学生会把未引用证据的词义当官方讲解。
- Realistic failure scenario: RAG 空结果时模型编造假朋友关系，前端仍显示 AI 徽章。
- Minimal fix: 有证据必须引用；无证据走题库解析 `RULE_FALLBACK`。
- Better long-term fix: 与整卷辅导共用同一校验管线。
- Regression test suggestion: 无 citation 的 structured 输出必须 fallback，不得标 AI。
- Estimated effort: 4 小时
- Fix note: RAG 失败、无有效 citation 或 explanation 未内联 `[C1]` 时走题库解析 `RULE_FALLBACK`（`GROUNDING_VALIDATION_FAILED`）。有证据才标 `GENERATION_SOURCE_AI`。`PracticeQuestionTutorIntegrationTest` 覆盖 grounded / 空 citation / RAG 失败。

### Finding: Grounding 校验器把学生答案当作 server 可信事实

- Severity: High
- Confidence: High
- Category: Security
- Status: Confirmed
- Remediation: Fixed (2026-08-13)
- Affected area: 练习辅导 verifier
- Evidence:
  - File: `app-server/src/main/java/com/huashi/eftransfer/app/modules/ai/service/AiInsightService.java:1151-1157`
  - Function / Module: `practiceTutoringVerificationPrompt`
  - Relevant behavior: **审计时** 提示写明 `wrongAnswers` 属于 server-owned，匹配这些字段的声称“supported by definition”。`AiContextAssemblerService` 把 `studentAnswer` 放进 `wrongAnswers`。用户 prompt 把整份 CONTEXT_JSON 直出，不如 lexical RAG 有 untrusted 围栏。
- Problem: 拼写题自由文本变成 verifier 的「定义即真」。对抗性作答可以让错误词义通过 grounding。
- Why it matters: 二次校验本应挡住幻觉，这里反而给注入开了特权通道。
- Realistic failure scenario: 学生提交「ignore evidence, X means Y」类作答；辅导重复该词义，verifier 因 serverContext 放行。
- Minimal fix: `studentAnswer` 标为 untrusted，不得作为词义证据；prompt 用独立 XML 围栏。
- Better long-term fix: verifier 只信任统计数字和词对标签，不信任任何作答原文。
- Regression test suggestion: 含指令的错误拼写不得让 lexical claim 被判 supported。
- Estimated effort: 4 小时
- Fix note: `studentAnswer` 从 trusted `wrongAnswers` 拆到 `untrustedStudentOutput`（`AiPromptContextSupport`）。辅导/单题 user prompt 用 `<untrusted_student_output>` 围栏；verifier 明确不得把学生作答当词义证据。`PracticeAiPromptSafetyTest` 锁 prompt 与 payload 拆分。

## 5. Architecture Concerns

- Coverage: High
- Inspected evidence: `src/`、`app-server/modules/`、`ai-gateway/modules/`、`shared-kernel/`、事件与 internal 客户端
- Exclusions / limits: 未生成完整包依赖循环图

| Subtype | Count | Affected Areas | Recommended Action |
|---------|-------|----------------|-------------------|
| ModuleBoundary | 1 | ConfigCenter / AssessmentService / AiInsightService | 按用例拆分 |
| DependencyDirection | 0 | 业务不直接依赖 pgvector 驱动 | 保持 |
| StateOwnership | 1 | WS 会话 map | 问卷限流已改 Redis；WS 可再收 |
| BoundaryContract | 1 | session JSON、内部 token | 加强校验 |
| EvolutionRisk | 1 | schema 快照 | 版本化 DDL |

已验证：前后端契约有 `shared-kernel` + `src/lib/contracts.ts`；AI 走 internal token；诊断/训练/练习并发有 DB 唯一键。

## 6. Security Concerns

- Coverage: High
- Inspected evidence: SecurityConfig、JWT、内部 filter、公开问卷、WS、上传、actuator、CORS
- Exclusions / limits: 未做动态利用

相关发现：导出脱敏开关已按 job 标志过滤；问卷默认密钥 / 内部 token / WS query / actuator 角色 / XFF / 单题讲解 grounding / 学生答案隔离已于 2026-08-13 修复；剩余端口暴露、CSRF、localStorage、扫描语义。

已验证：公开问卷 token 不回 JSON；JWT 与问卷/内部密钥启动熵校验；内部接口缺 token 或 prod 关闭保护时启动失败；上传有扩展名/魔数白名单；WS 握手拒绝 query token，首条 AUTH 或 Bearer 验 JWT 与黑名单。

## 7. Stability Concerns

- Coverage: High
- Inspected evidence: outbox、唯一键、超时、熔断、异步池、限流
- Exclusions / limits: 未做混沌测试

相关发现：练习开局未映射 409、fallback 同上游。问卷 verify/QR 限流已改 Redis/Bucket4j；练习 `answeredCount` 与导入行事务已修；AI 超时已对齐 180s。

已验证：Rabbit listener `default-requeue-rejected: false`；Resilience4j 配置存在；session 唯一键在 schema 中。

## 8. Performance Concerns

- Coverage: Medium
- Inspected evidence: Hikari 池、AI 超时、RAG 线程池、Redis 限流、nginx 超时
- Exclusions / limits: 无压测

相关发现：F-06（用户/IP 限流与 180s 超时已落地；日配额未做）。

已验证：DB 池有上限；RAG executor 有队列；multipart 50MB 上限。

## 9. Testing Gaps

- Coverage: High
- Inspected evidence: ci.yml、Vitest 文件、后端 `*IntegrationTest`
- Exclusions / limits: 未完整执行测试

相关发现：F-09、F-19、练习页零 Vitest、F-21 无障碍。

已验证：后端有 MockMvc 集成测试、公开问卷与 AI insight 测试、部分 Testcontainers。

## 10. Maintainability Concerns

- Coverage: High
- Inspected evidence: 最大文件、文档、模块结构
- Exclusions / limits: 未跑复杂度工具

相关发现：F-14、F-15、F-20。

已验证：模块按 entity/mapper/service/controller 切分；前端有 services/contracts 分层。

## 11. Design / Principles Concerns

- Coverage: High
- Inspected evidence: principles 对照、fail-fast、SRP、配置
- Exclusions / limits: 只报告有真实风险的违规

相关发现：F-05 单题讲解已 fail-closed；F-02 可关鉴权已在 prod 禁止；F-14 SRP。导出开关已按 `includeSensitiveFields` 过滤。

已验证：JWT、问卷密钥与内部 token 在非 local/test 拒绝弱值/占位符；公开问卷 cookie 路径收敛；辅导空 citation 走 `RULE_FALLBACK`。

## 12. Release Concerns

- Coverage: High
- Inspected evidence: CI、Dockerfile、compose、backup、schema init
- Exclusions / limits: 未观察真实生产发布

相关发现：F-08、F-09、F-16、F-17、F-19。

已验证：生产 overlay 绑定 127.0.0.1；镜像以非 root 跑 Java；有 backup-all.sh。

## 13. Documentation Analysis

- Coverage: High
- Inspected evidence: README、CLAUDE.md、runbook、模块文档
- Exclusions / limits: docs/superpowers 历史计划未逐份核对

| Subtype | Count | Affected Docs | Recommended Action |
|---------|-------|---------------|-------------------|
| UserDocs | 0 | 产品文案未深审 | — |
| OperatorDocs | 1 | 缺告警 runbook | 补事故页 |
| DeveloperDocs | 0 | src/CLAUDE.md 已写 Vitest | 保持 |
| ApiDocs | 0 | 契约有 generated 检查 | 保持 |
| DecisionRecord | 0 | README Flyway 行已删 | 保持 |
| StaleDocs | 0 | 清空库指引已限定 local | 保持 |

## 14. Configuration Safety Analysis

- Coverage: High
- Inspected evidence: application.yml、prod/local、.env.example、compose
- Exclusions / limits: 未读真实生产 secret

| Subtype | Count | Affected Keys / Files | Recommended Action |
|---------|-------|-----------------------|-------------------|
| SchemaValidation | 1 | 问卷密钥已有启动校验 | 保持 |
| UnsafeDefault | 1 | HMAC/PII YAML 默认仅限 local/test | prod 已拒绝 |
| EnvironmentSeparation | 1 | compose 默认 prod，密钥已从 `.env.example` 注入 | 运维须替换占位符 |
| SecretConfig | 0 | 内部 token、问卷密钥已拒绝占位符 | 保持 |
| FeatureFlag | 0 | — | — |
| ConfigDocs | 0 | `.env.example` 已列问卷密钥 | 保持 |

## 15. Observability / Operability Analysis

- Coverage: High
- Inspected evidence: actuator、log pattern、healthcheck、OTEL 开关
- Exclusions / limits: 无真实 Grafana

| Subtype | Count | Critical Signals Missing | Recommended Action |
|---------|-------|--------------------------|-------------------|
| Logging | 0 | traceId 已有 | 继续避免 PII |
| Metrics | 0 | 非 health 已限 ADMIN | 保持；告警仍缺 |
| Tracing | 0 | OTEL 可开 | — |
| HealthCheck | 0 | compose 已探活 | — |
| Alerting | 1 | 无规则 | 加 4 条基础告警 |
| Runbook | 1 | 无事故手册 | 补 AI/outbox/问卷 |
| Debuggability | 0 | 管理端口分离在 gateway | app-server 应对齐 |

## 16. Data Integrity Analysis

- Coverage: High
- Inspected evidence: schema 唯一键、outbox、init mode、runbook
- Exclusions / limits: 未跑备份还原

| Subtype | Count | Invariants at Risk | Recommended Action |
|---------|-------|-------------------|-------------------|
| TransactionBoundary | 0 | outbox 模式存在 | 保持 |
| Idempotency | 0 | 导入/reindex 有约束迹象 | 保持 |
| ConcurrencyConsistency | 0 | IN_PROGRESS 生成列唯一键 | 保持 |
| MigrationSafety | 1 | 无版本迁移 | 见 F-08 |
| InvariantValidation | 0 | 题库软删等有测试 | — |
| BackupRestore | 0 | 已有 restore 脚本（需 `CONFIRM_RESTORE=YES`） | 演练还原 |
| Reconciliation | 0 | analytics compensation 开关 | — |

## 17. Privacy / Data Governance Analysis

- Coverage: High
- Inspected evidence: profile/IP 加密、导出脱敏、retention 搜索
- Exclusions / limits: 未审日志采样内容

| Subtype | Count | Affected Data | Recommended Action |
|---------|-------|----------------|-------------------|
| DataInventory | 0 | 加密字段明确 | 写一份数据清单 |
| Minimization | 0 | 研究问卷字段有产品原因 | — |
| AccessBoundary | 0 | 导出敏感开关已按标志剥离资料列 | 保持 |
| Retention | 1 | 无 TTL | 见保留期发现 |
| Deletion | 1 | 无主体删除 | 见保留期发现 |
| Export | 0 | 普通导出脱敏有设计 | 保持 |
| TelemetryPrivacy | 0 | WS 已改为首条 AUTH，不再走 query | 保持 |

## 18. Accessibility / UX Correctness Analysis

- Coverage: Medium
- Inspected evidence: Login 标签、部分 aria、无 axe、ErrorBoundary
- Exclusions / limits: 未键盘走查

| Subtype | Count | Affected Workflows | Recommended Action |
|---------|-------|-------------------|-------------------|
| SemanticStructure | 1 | 长页面大量 div | 关键流改 semantic |
| KeyboardFocus | 1 | 未验证 | 问卷/登录走查 |
| ResponsiveVisual | 0 | 有 viewport 脚本 | — |
| ErrorState | 1 | 练习拼写无 label；参与码错误未绑定 | 补名称与 describedby |
| LoadingState | 0 | FeedbackState/RouteSkeleton 存在 | — |
| UXStateCorrectness | 0 | react-query 为主 | — |

## 19. Supply Chain / Reproducibility Analysis

- Coverage: Medium
- Inspected evidence: lockfile、ci.yml、Dockerfiles
- Exclusions / limits: 未跑漏洞扫描

| Subtype | Count | Affected Surface | Recommended Action |
|---------|-------|------------------|-------------------|
| DependencyProvenance | 1 | 浮动基镜像 | 钉 digest |
| Reproducibility | 0 | npm lock + mvnw | 保持 |
| CIIntegrity | 1 | 无 permissions | F-17 |
| ArtifactProvenance | 1 | 无 SBOM/签名 | 补 |
| RegistryHygiene | 0 | 私有应用 | — |

## 20. Cost / Resource Economics Analysis

- Coverage: Medium
- Inspected evidence: max-tokens、线程池、超时、用户/IP 限流
- Exclusions / limits: 无账单

| Subtype | Count | Cost Driver | Recommended Action |
|---------|-------|-------------|-------------------|
| UnboundedWork | 0 | AI POST 已按用户/IP 限流 | 可补日配额 |
| ExternalApiCost | 1 | LLM | 日配额/token 预算 |
| LLMCost | 0 | 读超时已对齐 nginx 180s | 可取消已断开客户端的上游 |
| InfrastructureSizing | 0 | 池大小有默认 | — |
| ObservabilityCost | 0 | 采样 0.1 | — |
| CostVisibility | 1 | 无 per-user 指标 | 加 |

## 21. AI / LLM Safety Analysis

- Coverage: High
- Inspected evidence: prompts、RagService untrusted 包装、grounding、内部 RAG
- Exclusions / limits: 无 injection eval

| Subtype | Count | Boundary Crossed | Recommended Action |
|---------|-------|------------------|-------------------|
| PromptInjection | 0 | 学生答案已拆到 untrusted 围栏 | 保持围栏 |
| ToolAuthorization | 0 | 未见任意工具调用 | 保持禁用 |
| RAGLeakage | 0 | 共享词库/题库语料 | 私有内容接入前加租户过滤 |
| ModelFallback | 0 | 单题讲解空 citation 已 fail-closed | 保持 |
| OutputValidation | 0 | 单题讲解要求有效 citation 与内联引用 | 可再抽共用校验管线 |
| EvalGap | 1 | 无注入回归；空 citation 已有集成测试 | 加最小注入集 |
| AbuseCost | 0 | 已有用户/IP 限流 | 可补日配额 |

## 22. Fallback / Defensive Code Analysis

- Coverage: High
- Inspected evidence: RULE_FALLBACK、fallback provider、grounding 空列表、catch 样例
- Exclusions / limits: 未穷尽所有 catch

| Subtype | Count | KeepWithAlert | FailFast | Remove |
|---------|-------|---------------|----------|--------|
| SilentFallback | 2 | 1 | 1 | 0 |
| EmptyCatch | 0 | 0 | 0 | 0 |
| CompatibilityBranch | 1 | 1 | 0 | 0 |
| SilentCorrection | 0 | 0 | 0 | 0 |
| DefensiveGuess | 1 | 0 | 1 | 0 |

`RULE_FALLBACK` 作为显式来源是正确模式；辅导与单题讲解空 citation 已按失败降级。fallback 指向同一上游仍是缺口。

## 23. Testing Authenticity Analysis

- Coverage: Medium
- Inspected evidence: 集成测试样例、CI、前端测试文件
- Exclusions / limits: 未审全部测试断言质量

| Test Area | Real Confidence | Risk | Action |
|-----------|---------------|------|--------|
| app-server Web 集成测试 | High | 漏掉前端与真实 MySQL 约束（部分有 Testcontainers） | Keep |
| ai-gateway Testcontainers | Medium | CI 已加 `-Dapi.version=1.44`；未在日志断言容器启动 | 保持 |
| 前端 Vitest | Medium | 已进 CI；练习页仍无专项测试 | 补练习页覆盖 |
| Playwright | None | 依赖存在无测试 | Use or remove |

有价值：`PublicAssessmentIntegrationTest`、`AiInsightIntegrationTest`、`ActuatorSecurityIntegrationTest`、`PracticeQuestionTutorIntegrationTest`、`PracticeSessionFlowIntegrationTest`、契约 `check:contracts`。
可疑：练习页仍无 Vitest。
缺失：练习页自动化、AI 日配额。prod 缺密钥启动失败与 AI 限流已有测试。

## 24. Type Safety Analysis

- Coverage: Medium
- Inspected evidence: `as any` 搜索、session parse、Java `@Validated`
- Exclusions / limits: 未跑 tsc

| Subtype | Count | Critical | High | Medium | Low |
|---------|-------|----------|------|--------|-----|
| UnsafeBlock | 0 | 0 | 0 | 0 | 0 |
| TypeAssertion | 1 | 0 | 0 | 1 | 0 |
| InputBoundary | 1 | 0 | 0 | 1 | 0 |
| OutputLeak | 0 | 0 | 0 | 0 | 0 |
| BooleanTrap | 0 | 0 | 0 | 0 | 0 |
| StringlyTyped | 0 | 0 | 0 | 0 | 0 |
| ErrorType | 0 | 0 | 0 | 0 | 0 |

前端几乎不用 `as any`；主要边界问题是 session 水合。后端 DTO 普遍有 validation。

## 25. Frontend State Analysis

- Coverage: Medium
- Inspected evidence: 最大页面、useEffect 计数、api.ts、ErrorBoundary
- Exclusions / limits: 未做运行时 profiling

| Subtype | Count | Affected Components |
|---------|-------|-------------------|
| ComponentSize | 1 | ConfigCenter、LexicalPairsWorkspace、practice/training |
| StateDuplication | 0 | react-query 为主 |
| PropDrilling | 0 | 未系统性证明 |
| EffectChain | 1 | research/training 多 effect |
| UIBusinessCoupling | 1 | ConfigCenter |
| DOMasState | 0 | — |
| RequestState | 0 | axios 刷新有共享 Promise |
| RenderPerf | 0 | 未测 |

## 26. Backend API Analysis

- Coverage: High
- Inspected evidence: SecurityConfig 路由表、公开问卷、internal、错误码
- Exclusions / limits: 未对照全部 VO 字段

| Subtype | Count | Affected Endpoints |
|---------|-------|-------------------|
| ApiConsistency | 0 | ApiResponse 包装统一 | 
| Validation | 0 | `@Valid` 普遍 |
| Auth | 0 | actuator 已限 ADMIN；internal token 已有熵校验 |
| NplusOne | 0 | 未证实热点 |
| Caching | 0 | — |
| ErrorResponse | 0 | ResultCode 存在 |
| BusinessLogic | 1 | PublicAssessmentService 过大 |
| DataFlow | 0 | outbox 清晰 |

## 27. Dependency Weight Analysis

- Coverage: Medium
- Inspected evidence: package.json、两个后端 pom
- Exclusions / limits: 未测 jar/bundle 大小

| Dependency | Status | Weight | Transitives | Used For | Recommended Action |
|------------|--------|--------|-------------|----------|-------------------|
| playwright | Unused in tests | 大 | 浏览器驱动 | 计划中的 E2E | 接入或移除 |
| gsap + framer-motion | Overweight | 中 | 动画 | 两套动画 | 收敛一套 |
| echarts | Healthy | 中 | 图表 | 学情 | Keep |
| html2canvas + jspdf | Healthy | 中 | PDF | 报告 | Keep |
| fingerprintjs | Healthy | 小 | QR 问卷 | Keep，注意隐私披露 |

## 28. Code Consistency Analysis

- Coverage: Medium
- Inspected evidence: 双份 InternalApiAuthenticationFilter、模块命名
- Exclusions / limits: 未跑 format 全量 diff

内部 token filter 在 app-server 与 ai-gateway 几乎复制，但校验已抽到 `shared-kernel` `SecretPolicy`。模块命名 `modules/<域>/` 一致。前端服务层集中。文档与 CI 已对齐 `npm test`；练习页仍无专项测试。

## 29. Comment Coverage Analysis

- Coverage: Medium
- Inspected evidence: CLAUDE.md、代码 TODO 搜索、schema 注释
- Exclusions / limits: 未统计 Javadoc 覆盖率

过期注释/文档已在本批收口（“无前端测试”、Flyway、清空库指引）。生产代码 TODO 很少。公开 API 更多靠 VO 命名而非长注释，可接受。

## 30. Principles Compliance

整体：JWT、问卷密钥与内部 token 已统一 fail-fast；辅导与单题讲解空 citation 已 fail-closed；XFF 仅信代理。剩余原则缺口主要是超大文件结构债。

### Principles Violated

| Principle | Violations | Severity | Affected Areas |
|-----------|------------|----------|----------------|
| Single Responsibility (1.1) | 3 | Medium | ConfigCenter、AssessmentService、AiInsightService |
| File Size Limit (1.2) | 8+ | Medium | 见 F-14 |
| Fail-Fast (4.4) | 0 | — | 导出开关与单题讲解已 fail-closed |
| Don't Swallow Errors (6.1) | 0 | — | 未发现空 catch 作为主路径 |
| Configuration Over Hardcoding (9.1) | 0 | — | 问卷默认值仅 local/test |
| Fail on Missing Configuration (9.2) | 0 | — | 内部 token / 问卷密钥已拒绝占位符 |
| Timeout Every External Call (10.4) | 0 | — | AI 读超时已对齐 nginx 180s；取消已断开客户端仍缺 |
| Unbounded Resources (10.2) | 0 | — | verificationAttempts map 已改 Redis |
| Test Behavior (8.1) | 1 | Medium | 练习页 Vitest 仍缺 |
| Least Privilege (4.6) | 0 | — | XFF 仅信代理 CIDR |

### Principles Respected

- 内部接口空 token 拒绝启动
- JWT / 问卷 HMAC / 内部 token 弱密钥或占位符拒绝启动（非 local/test）
- 诊断/训练/练习单用户 IN_PROGRESS 用数据库约束而不是“尽力而为”
- RAG 提示词把检索内容标为 untrusted；辅导与单题讲解空 citation 走 RULE_FALLBACK
- 学生作答进 untrusted 围栏，不得作为 grounding 词义证据
- XFF 仅对可信代理 CIDR 解析
- 公开问卷会话 token 不返回给 JS
- 生产 schema init 为 never，避免启动时误建表覆盖心智（尽管迁移仍缺）
- `/actuator/**` 除 health 外要求 ADMIN

## 31. Architecture Analysis

见第 5 节表格。系统分层（SPA / app-server / ai-gateway / shared-kernel）方向正确：UI 不碰向量库，业务 AI 经网关。主要演化风险是超大应用服务类和 schema 快照流程，而不是循环依赖。

## 32. Recommended Fix Order

### Fix Immediately

- ~~导出 `includeSensitiveFields=false` 时剥离资料题/姓名/联系方式~~ 已完成
- ~~问卷 HMAC / 敏感资料密钥：prod 无显式配置则拒绝启动；写入 compose 与 `.env.example`~~ 已修复
- 默认 compose 把数据面端口绑到 `127.0.0.1`（8090 已绑 loopback；其余数据面端口未收口）
- ~~内部 token 熵/占位符校验~~ 已修复
- ~~不信任客户端 X-Forwarded-For~~ 已修复（仅信代理 CIDR + nginx 覆盖）
- ~~WebSocket 停止使用 `access_token` query~~ 已修复（首条 AUTH 消息）
- ~~Actuator metrics/prometheus 限 ADMIN 或独立端口~~ 已修复（角色限制；未拆端口）
- ~~修复练习 `answeredCount`；导入批次改 `TransactionTemplate`~~ 已修复
- ~~grounding / 单题讲解 fail-closed；verifier 不信任学生作答原文~~ 已修复

### Fix Before Stable Release

- ~~grounding 空 citation 失败并降级规则~~ 已修复
- ~~AI 用户级限流 + 对齐 nginx/后端超时~~ 已完成
- ~~公开问卷限流改 Redis~~ 已完成
- ~~CI 跑 `npm test`，backend 加 Testcontainers API 版本~~ 已完成
- ~~文档去掉 Flyway / “无前端测试”，对齐 schema 变更 runbook（含 `docs/ddl/` 与 restore 脚本）~~ 已完成
- CSRF 保护 cookie 会话端点

### Schedule Later

- 研究数据保留/删除
- 拆 ConfigCenter 与超大 service
- 告警与事故 runbook
- CI permissions、镜像 digest、SBOM
- refresh token 改 httpOnly
- 附件真实扫描或更名状态
- fallback 与 active 相同则启动失败
- 会话 zod 校验

### Ignore for Now

- 双动画库收敛（无安全期限）
- Playwright 去留（与 E2E 计划绑定）
- 全面 Javadoc

## 33. Quick Wins

- ~~`.env.example` 增加问卷密钥项，compose 传递它们（1 小时）~~ 已修复
- ~~CI `npm test` + `permissions: contents: read`（1 小时）~~ `npm test` 已完成；permissions 仍属后续
- ~~`src/CLAUDE.md` / README Flyway 行（15 分钟）~~ 已完成
- ~~`verifyGuidanceGrounding` 空列表/RAG null 返回 false（1 小时含测试）~~ 已修复
- ~~练习 `answeredCount` 只计非空（2 小时）~~ 已修复
- ~~导入批次每行 `TransactionTemplate`（1 天）~~ 已修复
- ~~单题讲解空 citation / RAG 失败走 RULE_FALLBACK~~ 已修复
- ~~verifier 拆 trusted / untrusted 学生作答~~ 已修复
- ~~XFF 仅信代理 CIDR + nginx 覆盖~~ 已修复
- 练习开局捕获唯一键 → 409（1 小时）
- ~~导出 false 时不写资料汇总原文（3 小时）~~ 已完成
- ~~actuator 角色限制（1 小时含测试）~~ 已修复
- ~~WS 去掉 query token（2 小时）~~ 已修复

## 34. Long-term Refactor Plan

1. **问卷与配置密钥统一校验框架**  
   动机：JWT 已有成熟校验，问卷/内部 token 当时是第二套更弱规则。  
   做法：已抽 `SecretPolicy` 到 shared-kernel，按 profile 强制。后续可纳入独立 secret store 与密钥轮换。  
   风险：错误拒绝合法旧密钥。  
   测试：各 profile 启动测试。

2. **超大 UI/服务按用例拆分**  
   动机：ConfigCenter 与 AssessmentService 是后续功能的碰撞点。  
   做法：按 tab/用例抽 hook 与 application service，不先上新框架。  
   风险：拆分引入行为漂移。  
   测试：先锁现有集成测试再搬代码。

3. **生产迁移从快照切到正向 DDL**  
   动机：研究数据不能靠 drop 升级。  
   做法：保留 schema.sql 作目标快照，发布只跑评审过的增量。  
   风险：两份真相漂移。  
   测试：空库快照 vs 增量应用到上一版本，结构对比。
