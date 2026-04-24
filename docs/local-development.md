# 本地开发说明

## 1. 仓库结构

```text
root
├── src                     # React + TypeScript + Vite 前端
├── app-server              # 主业务服务
├── ai-gateway              # AI / RAG 网关
├── shared-kernel           # 共享契约、枚举、事件
├── deploy                  # Docker Compose 与本地依赖
└── docs
```

## 2. 真实链路

### 登录与权限

`登录 -> JWT access token + refresh token -> Redis refresh token / blacklist -> /api/auth/me -> CurrentUserVO.capabilities -> 前端路由与菜单`

### 学生学习链路

`诊断模板 -> diagnosis session -> summary -> analytics 聚合 -> training plan -> training session -> wrong book / review schedule`

### AI 链路

`analytics snapshot + diagnosis/training 结果 -> app-server 上下文组装 -> ai-gateway chat / rag -> 诊断解释、训练建议、教师干预建议`

### 知识链路

`词对/词义/例句变更 -> app-server 发布 LexicalKnowledgeChangedEvent -> RabbitMQ -> ai-gateway 消费并定向 reindex -> retrieve / rerank`

## 3. 角色与能力模型

- `STUDENT -> STUDENT_WORKSPACE`
- `TEACHER -> TEACHING_WORKSPACE`
- `ADMIN -> ADMIN_CONSOLE + TEACHING_WORKSPACE + STUDENT_WORKSPACE`
- 多角色用户取能力并集
- enabled 用户必须至少有一个 role；零角色账户会被后端直接拒绝登录
- 前端页面显隐、默认首页、侧边栏和 AI 助手入口都基于 `capabilities`，不再只看 `primaryRole`

## 4. 安全默认值

- `APP_JWT_ACTIVE_KID` 与 `APP_JWT_KEYS_*` 必须显式提供，新 access token 会写入 `kid`
- `APP_JWT_LEGACY_SECRET` 仅用于旧 token 兼容验签窗口，不应作为长期主配置
- `APP_OPS_CONFIG_ENCRYPTION_SECRET` 必须与 JWT 密钥分离，非 `local/test` 缺失时应用拒绝启动
- `PLATFORM_INTERNAL_API_TOKEN` 是 `app-server` 与 `ai-gateway` 的统一内部令牌
- `platform.internal-api.enabled=true` 时，所有 `/internal/**` 接口都要求 `X-Internal-Token`
- `APP_DEMO_DATA_ENABLED=false` 是默认值；demo 用户初始化仅在 `local/test` profile 且显式打开时执行
- `ai-gateway` 的 `/internal/ai/**`、`/internal/ai/rag/**` 与 `app-server` 的 `/internal/**` 都采用同一内部鉴权头
- `app-server` 与 `ai-gateway` 现在都要求显式提供 `PLATFORM_INTERNAL_API_TOKEN`，缺失时不会启动为“半可用”状态

## 5. 环境变量

```bash
cd deploy
cp .env.example .env
```

至少需要检查并填写：

- `APP_OPS_CONFIG_ENCRYPTION_SECRET`
- `APP_JWT_ACTIVE_KID`
- `APP_JWT_KEYS_0_KID`
- `APP_JWT_KEYS_0_SECRET`
- `APP_JWT_KEYS_1_KID`
- `APP_JWT_KEYS_1_SECRET`
- `APP_JWT_LEGACY_SECRET`（仅在旧 token 兼容窗口需要时填写）
- `PLATFORM_INTERNAL_API_TOKEN`
- `REDIS_PASSWORD`
- `APP_DEMO_DATA_ENABLED`
- `AI_OPENAI_API_KEY`
- `AI_OPENAI_BASE_URL`
- `AI_CHAT_MODEL`
- `AI_EMBEDDING_BASE_URL`
- `AI_EMBEDDING_MODEL`
- `AI_RERANK_PROTOCOL`
- `AI_RERANK_BASE_URL`
- `AI_RERANK_MODEL`
- `AI_FALLBACK_CHAT_*`
- `AI_FALLBACK_EMBEDDING_*`
- `AI_FALLBACK_RERANK_*`

JWT key 建议直接用随机源生成，例如：`openssl rand -base64 48`

说明：

- `APP_DEMO_DATA_ENABLED=true` 时，本地会注入默认管理员、教师、学生测试账号
- `APP_DEMO_DATA_ENABLED=false` 时，登录账号需要自行准备
- `APP_OPS_CONFIG_ENCRYPTION_SECRET` 建议使用独立 secret；只在本地临时调试时才允许回退到旧 JWT secret
- `REDIS_PASSWORD` 不能为空；Compose 中的 Redis 现在启用密码认证并仅绑定到 `127.0.0.1`
- `PLATFORM_INTERNAL_API_TOKEN` 必须同时提供给 `app-server` 和 `ai-gateway`
- `AI_FALLBACK_*` 需要显式提供；如果留空，bootstrap fallback 可能仍与 active provider 指向同一上游
- `diagnosis` 与 `training` 都只允许同一用户保留一个进行中的 `IN_PROGRESS` session；刷新后前端优先恢复该 session
- 生产环境建议显式设置 `APP_DB_SSL_MODE=REQUIRED`
- 默认登录锁定策略由 `APP_AUTH_LOCKOUT_*` 控制，默认值是 5 次失败锁定 15 分钟
- `app-server` / `ai-gateway` 在 `local` / `dev` 会执行各自的 `schema.sql` 建表；`prod` 不自动初始化
- `AI_EMBEDDING_DIMENSION` 与 pgvector schema 固定为 `1024`；修改维度前先更新 `ai-gateway/src/main/resources/schema.sql` 并重建数据库

如果你想直接跑通“导入词对 -> 继续接到模板 / 词表 / RAG”的完整流程，优先看：

- [数据导入与使用指南](/mnt/d/huashi2/docs/data-import-and-usage.md)

## 6. Docker 本地联调

只启动基础依赖：

```bash
cd deploy
docker compose --env-file .env up -d mysql redis rabbitmq postgres
```

启动完整后端链路：

```bash
cd deploy
docker compose --env-file .env up --build app-server ai-gateway
```

启动完整栈：

```bash
cd deploy
docker compose --env-file .env up --build
```

前端容器现在默认以 Docker 开发模式运行：

- `frontend` 会把仓库根目录 bind mount 到容器内 `/workspace`
- `node_modules` 使用独立 volume，避免被宿主机目录覆盖
- 容器启动时会执行 `npm install --include=dev`，确保 `package.json` 新增依赖后 volume 内依赖自动补齐
- 默认开启 Vite 轮询监听，兼容 Docker Desktop / WSL 下的热更新
- 修改前端源码后不需要重建 `frontend` 镜像；只有依赖变更时才建议重新 `docker compose up -d --build frontend`

## 7. 本地命令启动

前提：

- JDK `25`
- Node.js `20+`
- MySQL / Redis / RabbitMQ / PostgreSQL 已启动
- 仓库已内置 Maven Wrapper，无需全局安装 Maven

启动 `ai-gateway`：

```bash
./mvnw -pl ai-gateway -am spring-boot:run
```

启动 `app-server`：

```bash
./mvnw -pl app-server -am spring-boot:run
```

启动前端：

```bash
npm install
npm run dev
```

## 8. 健康检查

- `http://localhost:8080/api/health`
- `http://localhost:8080/actuator/health`
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`
- `http://127.0.0.1:18090/actuator/health`（仅直启 `ai-gateway` 时可访问）

注意：

- `http://localhost:8090/internal/ai/health` 是内部接口，需要携带 `X-Internal-Token`
- Compose 不会把 `ai-gateway` 的 management 端口映射到宿主机；容器健康检查走容器内 `127.0.0.1:${AI_GATEWAY_MANAGEMENT_PORT:-18090}`
- 本地排查内部接口时可以使用：

```bash
curl -H "X-Internal-Token: $PLATFORM_INTERNAL_API_TOKEN" http://localhost:8090/internal/ai/health
```

Compose 中的 `app-server` 与 `ai-gateway` 也已配置容器健康检查，依赖关系会等待健康状态再继续拉起。

## 9. 备份脚本

- `deploy/scripts/backup-mysql.sh`
- `deploy/scripts/backup-postgres.sh`
- `deploy/scripts/backup-all.sh`

默认行为：

- 读取 `deploy/.env`
- 输出目录使用 `BACKUP_DIR`，默认 `./backups`
- 保留天数使用 `BACKUP_RETENTION_DAYS`，默认 `7`

cron 示例：

```bash
0 2 * * * cd /path/to/repo && ./deploy/scripts/backup-all.sh
```

## 10. 当前已实现内容

- 登录、刷新、注销、当前用户、Redis refresh token 与 access token blacklist
- access token `kid` 轮换与 legacy 无 `kid` token 兼容验签
- 基于角色并集生成 `capabilities`，前后端权限矩阵已对齐到能力模型
- 诊断模板发布、diagnosis session、summary、结果页、进度保存与恢复
- training plan、training session、错误本、复习计划、training progress 保存与恢复
- analytics 学生概览/详情聚合链路
- AI 诊断解释、训练推荐、教师干预建议、后台 AI 配置中心
- app-server 内部知识导出
- ai-gateway embedding / retrieve / rerank / reindex
- RabbitMQ 驱动的知识同步：`LexicalKnowledgeChangedEvent -> ai-gateway targeted reindex`

## 11. 事件边界

- diagnosis completed：应用内事件，用于本服务 analytics/read model 更新
- training completed：应用内事件，用于本服务 analytics/read model 更新
- lexical knowledge changed：RabbitMQ 跨服务事件，用于驱动 `ai-gateway` 定向知识重建

当前设计意图：

- 不把 RabbitMQ 当作本地 analytics 总线
- 只把 RabbitMQ 用在真实跨服务解耦场景

## 12. 默认测试账号

仅当 `APP_DEMO_DATA_ENABLED=true` 时存在：

- 管理员：`admin` / `Admin@123456`
- 教师：`teacher.zhang` / `Teacher@123456`
- 学生：`student.li` / `Student@123456`
- 学生：`student.wang` / `Student@123456`

如果你不知道导入入口在哪里，或者不知道导入后为什么学生端还看不到，请直接跳到：

- [数据导入与使用指南](/mnt/d/huashi2/docs/data-import-and-usage.md)

## 13. 前端工程约束

- 路由采用 `React.lazy` 做页面级懒加载
- 图表运行时统一走 `src/lib/echarts.ts`，使用 `echarts/core` 做按需注册
- `vite.config.ts` 已做手动分包，并提供 `npm run build:analyze`
- 前端真实链路统一走 `src/lib/services.ts`
- 旧实验 hooks / store / types 已从主仓库链路中清理，不再保留误导性 mock 实现

## 14. 验证命令

```bash
npm run lint
npm run typecheck
npm run build
./mvnw test
npm run build:analyze
```

如果当前环境没有 `java` 或没有 JDK `25`，即使仓库已带 `./mvnw`，Java 启动与测试命令仍无法执行。
