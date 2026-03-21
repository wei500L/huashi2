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
- `PLATFORM_INTERNAL_API_TOKEN` 由 `app-server` 与 `ai-gateway` 共享，缺失时 `ai-gateway` 的 app-server 内部调用配置不会通过校验
- `APP_JWT_SECRET` 不再有不安全默认值
- demo 用户初始化仅在 `local/test` profile 且 `APP_DEMO_DATA_ENABLED=true` 时启用

## 本地启动

### 1. 准备环境

```bash
cd deploy
cp .env.example .env
```

至少补齐：

- `APP_JWT_SECRET`
- `PLATFORM_INTERNAL_API_TOKEN`
- AI 供应商相关变量：`AI_OPENAI_API_KEY`、`AI_OPENAI_BASE_URL`、`AI_CHAT_MODEL`、`AI_EMBEDDING_MODEL`、`AI_RERANK_URL`、`AI_RERANK_MODEL`

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
- Actuator：`http://localhost:8090/actuator/health`
- `http://localhost:8090/internal/ai/health` 为内部接口，需要 `X-Internal-Token`

## 当前架构决策

- diagnosis/training 完成事件默认只在 `app-server` 进程内驱动 analytics 聚合
- RabbitMQ 当前正式职责是跨服务知识同步，不承担本地 analytics 投影
- diagnosis / training 都限制为单用户单活跃 `IN_PROGRESS` session，并支持历史查询与进度保存
- 前端采用路由级懒加载、`echarts/core` 按需注册和手动分包，减少图表运行时膨胀

更多本地联调、环境变量和行为说明见 [docs/local-development.md](/mnt/d/huashi2/docs/local-development.md)。
