# EF Transfer Platform

多模块仓库，面向英语-法语迁移诊断、训练与 AI 辅助教学场景。

## 模块

- `src/`: React + TypeScript + Vite 前端
- `app-server/`: Spring Boot 主业务服务，负责认证、诊断、训练、分析、后台配置
- `ai-gateway/`: AI / RAG 网关，负责 chat、embedding、rerank、知识库重建
- `shared-kernel/`: 前后端共享契约、枚举、事件模型
- `deploy/`: Docker Compose、本地联调依赖和镜像构建

## 真实主链路

### 业务链路

`登录 -> JWT / refresh token / Redis session -> diagnosis session -> diagnosis summary -> analytics snapshot -> training plan -> training session -> wrong book / review schedule -> AI context assembly -> ai-gateway`

### 知识链路

`词对/词义/例句维护 -> app-server 发布 LexicalKnowledgeChangedEvent -> RabbitMQ -> ai-gateway 定向 reindex -> RAG retrieve / rerank -> 业务 AI 场景消费`

## 角色与能力

- `STUDENT -> STUDENT_WORKSPACE`
- `TEACHER -> TEACHING_WORKSPACE`
- `ADMIN -> ADMIN_CONSOLE + TEACHING_WORKSPACE + STUDENT_WORKSPACE`
- enabled 用户必须至少分配一个 role；零角色账户不会进入登录主链路
- 前端路由、导航和默认首页基于 `CurrentUserVO.capabilities` 判定，不再只看 `primaryRole`

## 安全与内部接口

- `X-Internal-Token` 是内部接口统一鉴权头
- `PLATFORM_INTERNAL_API_ENABLED=true` 时，`/internal/**` 一律 fail-close
- `PLATFORM_INTERNAL_API_TOKEN` 由 `app-server` 与 `ai-gateway` 共享，缺失时两个服务都会显式拒绝启动
- `APP_JWT_ACTIVE_KID` 与 `APP_JWT_KEYS_*` 控制当前签名 key ring；新 access token 始终带 `kid`
- `APP_JWT_LEGACY_SECRET` 仅用于旧 token 兼容验签窗口
- `APP_OPS_CONFIG_ENCRYPTION_SECRET` 与 JWT 密钥职责分离，非 `local/test` 缺失时 `app-server` 不会启动
- demo 用户初始化仅在 `local/test` profile 且 `APP_DEMO_DATA_ENABLED=true` 时启用

## 本地启动

### 1. 准备环境

```bash
cd deploy
cp .env.example .env
```

至少补齐：

- `APP_OPS_CONFIG_ENCRYPTION_SECRET`
- `APP_JWT_ACTIVE_KID`
- `APP_JWT_KEYS_0_KID`
- `APP_JWT_KEYS_0_SECRET`
- `APP_JWT_KEYS_1_KID`
- `APP_JWT_KEYS_1_SECRET`
- `APP_JWT_LEGACY_SECRET`（仅在旧 token 兼容窗口需要时填写）
- `PLATFORM_INTERNAL_API_TOKEN`
- `REDIS_PASSWORD`
- AI 供应商相关变量：`AI_OPENAI_API_KEY`、`AI_OPENAI_BASE_URL`、`AI_CHAT_MODEL`、`AI_EMBEDDING_BASE_URL`、`AI_EMBEDDING_MODEL`、`AI_RERANK_PROTOCOL`、`AI_RERANK_BASE_URL`、`AI_RERANK_MODEL`
- 如需真实 fallback：`AI_FALLBACK_CHAT_*`、`AI_FALLBACK_EMBEDDING_*`、`AI_FALLBACK_RERANK_*` 这三组变量都应显式填写

JWT key 需要使用随机高熵值，示例可用：`openssl rand -base64 48`

AI fallback 说明：

- `AI_FALLBACK_*` 需要显式填写，failover 才会真正切到另一套上游
- 如果省略这些变量，bootstrap 默认值会回退到 active provider 使用的同一组 URL / API key / model
- 后台 AI 配置中心会对“fallback 与 active 实际指向同一上游”给出 warning

生产环境额外建议：

- `APP_DB_SSL_MODE=REQUIRED`
- `APP_AUTH_LOCKOUT_ENABLED=true`
- `APP_AUTH_LOCKOUT_THRESHOLD=5`
- `APP_AUTH_LOCKOUT_DURATION=PT15M`
- `SPRING_PROFILES_ACTIVE=prod`，并保持 `spring.flyway.baseline-on-migrate=false`
- `AI_EMBEDDING_DIMENSION=1024`，不要在运行时切换 pgvector 维度

### 2. 启动依赖

```bash
cd deploy
docker compose --env-file .env up -d mysql redis rabbitmq postgres
```

### 3. 启动后端

```bash
./mvnw -pl ai-gateway -am spring-boot:run
./mvnw -pl app-server -am spring-boot:run
```

### 4. 启动前端

```bash
npm install
npm run dev
```

如果你当前最关心的是“怎么导入数据并继续用起来”，先看这份实操指南：

- [数据导入与使用指南](/mnt/d/huashi2/docs/data-import-and-usage.md)
- [数据库迁移执行手册](/mnt/d/huashi2/docs/db-migration-runbook.md)

## 验证命令

```bash
npm run lint
npm run typecheck
npm run build
./mvnw test
```

图表包分析：

```bash
npm run build:analyze
```

## 健康检查

- 对外健康检查：`http://localhost:8080/api/health`
- Actuator：`http://localhost:8080/actuator/health`
- OpenAPI：`http://localhost:8080/swagger-ui.html`
- OpenAPI：`http://localhost:8080/v3/api-docs`
- AI Gateway Actuator（直启服务时）：`http://127.0.0.1:18090/actuator/health`
- `http://localhost:8090/internal/ai/health` 为内部接口，需要 `X-Internal-Token`

Docker Compose 现在也为 `app-server` 和 `ai-gateway` 启用了容器级健康检查，并以非 root 用户运行镜像。`ai-gateway` 的 management 端口仅绑定到容器内 loopback，不映射到宿主机。

## 备份脚本

- `deploy/scripts/backup-mysql.sh`
- `deploy/scripts/backup-postgres.sh`
- `deploy/scripts/backup-all.sh`

默认读取 `deploy/.env`，备份输出到 `BACKUP_DIR`，保留天数由 `BACKUP_RETENTION_DAYS` 控制。示例 cron：

```bash
0 2 * * * cd /path/to/repo && ./deploy/scripts/backup-all.sh
```

## 当前架构决策

- diagnosis/training 完成事件默认只在 `app-server` 进程内驱动 analytics 聚合
- RabbitMQ 当前正式职责是跨服务知识同步，不承担本地 analytics 投影
- diagnosis / training 都限制为单用户单活跃 `IN_PROGRESS` session，并支持历史查询与进度保存
- 前端采用路由级懒加载、`echarts/core` 按需注册和手动分包，减少图表运行时膨胀

更多本地联调、环境变量和行为说明见 [docs/local-development.md](/mnt/d/huashi2/docs/local-development.md)。
