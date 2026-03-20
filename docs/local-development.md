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
- 前端页面显隐、默认首页、侧边栏和 AI 助手入口都基于 `capabilities`，不再只看 `primaryRole`

## 4. 安全默认值

- `APP_JWT_SECRET` 必须显式提供，非测试环境不再使用可预测默认值
- `PLATFORM_INTERNAL_API_TOKEN` 是 `app-server` 与 `ai-gateway` 的统一内部令牌
- `platform.internal-api.enabled=true` 时，所有 `/internal/**` 接口都要求 `X-Internal-Token`
- `APP_DEMO_DATA_ENABLED=false` 是默认值；demo 用户初始化仅在 `local/test` profile 且显式打开时执行
- `ai-gateway` 的 `/internal/ai/**`、`/internal/ai/rag/**` 与 `app-server` 的 `/internal/**` 都采用同一内部鉴权头

## 5. 环境变量

```bash
cd deploy
cp .env.example .env
```

至少需要检查并填写：

- `APP_JWT_SECRET`
- `PLATFORM_INTERNAL_API_TOKEN`
- `APP_DEMO_DATA_ENABLED`
- `AI_OPENAI_API_KEY`
- `AI_OPENAI_BASE_URL`
- `AI_CHAT_MODEL`
- `AI_EMBEDDING_MODEL`
- `AI_RERANK_URL`
- `AI_RERANK_MODEL`

说明：

- `APP_DEMO_DATA_ENABLED=true` 时，本地会注入默认管理员、教师、学生测试账号
- `APP_DEMO_DATA_ENABLED=false` 时，登录账号需要自行准备
- `PLATFORM_INTERNAL_API_TOKEN` 必须同时提供给 `app-server` 和 `ai-gateway`

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

## 7. 本地命令启动

前提：

- JDK `25`
- Maven `3.9.11+`
- Node.js `20+`
- MySQL / Redis / RabbitMQ / PostgreSQL 已启动

启动 `ai-gateway`：

```bash
mvn -pl ai-gateway -am spring-boot:run
```

启动 `app-server`：

```bash
mvn -pl app-server -am spring-boot:run
```

启动前端：

```bash
npm install
npm run dev
```

## 8. 健康检查

- `http://localhost:8080/api/health`
- `http://localhost:8080/actuator/health`
- `http://localhost:8090/actuator/health`

注意：

- `http://localhost:8090/internal/ai/health` 是内部接口，需要携带 `X-Internal-Token`
- 本地排查内部接口时可以使用：

```bash
curl -H "X-Internal-Token: $PLATFORM_INTERNAL_API_TOKEN" http://localhost:8090/internal/ai/health
```

## 9. 当前已实现内容

- 登录、刷新、注销、当前用户、Redis refresh token 与 access token blacklist
- 基于角色并集生成 `capabilities`，前后端权限矩阵已对齐到能力模型
- 诊断模板发布、diagnosis session、summary、结果页、进度保存与恢复
- training plan、training session、错误本、复习计划、training progress 保存与恢复
- analytics 学生概览/详情聚合链路
- AI 诊断解释、训练推荐、教师干预建议、后台 AI 配置中心
- app-server 内部知识导出
- ai-gateway embedding / retrieve / rerank / reindex
- RabbitMQ 驱动的知识同步：`LexicalKnowledgeChangedEvent -> ai-gateway targeted reindex`

## 10. 事件边界

- diagnosis completed：应用内事件，用于本服务 analytics/read model 更新
- training completed：应用内事件，用于本服务 analytics/read model 更新
- lexical knowledge changed：RabbitMQ 跨服务事件，用于驱动 `ai-gateway` 定向知识重建

当前设计意图：

- 不把 RabbitMQ 当作本地 analytics 总线
- 只把 RabbitMQ 用在真实跨服务解耦场景

## 11. 默认测试账号

仅当 `APP_DEMO_DATA_ENABLED=true` 时存在：

- 管理员：`admin` / `Admin@123456`
- 教师：`teacher.zhang` / `Teacher@123456`
- 学生：`student.li` / `Student@123456`
- 学生：`student.wang` / `Student@123456`

## 12. 前端工程约束

- 路由采用 `React.lazy` 做页面级懒加载
- `vite.config.ts` 已做手动分包
- 前端真实链路统一走 `src/lib/services.ts`
- `src/hooks/useAnalytics.ts`、`src/hooks/useDashboard.ts`、`src/store/diagnosis.store.ts` 等旧实验流仍在仓库中，但不再是主业务链路

## 13. 验证命令

```bash
npm run lint
npm run typecheck
npm run build
mvn test
```

如果当前环境没有全局 `mvn`，Java 测试与启动命令无法执行，需要先安装 Maven。
