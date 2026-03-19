# 本地开发说明

## 1. 版本基线

- `app-server`: Spring Boot `4.0.3` + Java `25` + MyBatis-Plus `3.5.16`
- `ai-gateway`: Spring Boot `3.5.3` + Spring AI `1.1.3` + PostgreSQL `18.3` + pgvector `0.8.2`

说明：

- `app-server` 采用 `mybatis-plus-spring-boot4-starter`，避免误用 Boot 3 starter。
- `ai-gateway` 当前使用 Spring AI `1.1.3`，通过 OpenAI compatible 协议接入 Qwen，并预留 DeepSeek 回切配置。
- 当前仓库同时保留原有前端 Vite 工程与新增 Java 多模块后端。

## 2. 目录说明

```text
root
├── app-server
├── ai-gateway
├── shared-kernel
├── deploy
└── docs
```

## 3. 环境变量准备

```bash
cd deploy
cp .env.example .env
```

如需真实 AI 联调，至少修改以下变量：

- `AI_OPENAI_API_KEY`
- `AI_OPENAI_BASE_URL`
- `AI_CHAT_MODEL`
- `AI_EMBEDDING_MODEL`
- `AI_RERANK_URL`
- `AI_RERANK_MODEL`
- `APP_JWT_SECRET`
- `APP_JWT_REFRESH_TTL`

## 4. Docker 本地联调

只启动基础依赖：

```bash
cd deploy
docker compose --env-file .env up -d mysql redis rabbitmq postgres
```

启动全链路：

```bash
cd deploy
docker compose --env-file .env up --build
```

健康检查地址：

- `http://localhost:8080/api/health`
- `http://localhost:8090/internal/ai/health`
- `http://localhost:8080/actuator/health`
- `http://localhost:8090/actuator/health`

## 5. 本地 Maven 启动

前提：

- JDK `25`
- Maven `3.9.11+`
- MySQL / Redis / RabbitMQ / PostgreSQL 已启动

启动 `ai-gateway`：

```bash
mvn -pl ai-gateway -am spring-boot:run
```

启动 `app-server`：

```bash
mvn -pl app-server -am spring-boot:run
```

运行测试：

```bash
mvn test
```

## 6. 当前已实现内容

- 多模块 Maven 根工程
- 统一返回结构 `ApiResponse<T>`
- 统一结果码 `ResultCode`
- 分页模型 `PageQuery` / `PageResult`
- 审计字段基类 `BaseAuditEntity`
- 通用枚举
- `app-server` 基础安全、JWT、Redis、RabbitMQ、MyBatis-Plus、Flyway、健康检查
- `app-server` 真实认证授权链路：登录、刷新、注销、当前用户、管理员用户列表、Redis refresh token 与 access token 拉黑
- `ai-gateway` OpenAI compatible 配置、pgvector 连接、向量库启动配置、Rerank HTTP 客户端、健康检查
- 本地 Docker 开发环境与服务镜像构建

## 7. 默认测试账号

- 管理员：`admin` / `Admin@123456`
- 教师：`teacher.zhang` / `Teacher@123456`
- 学生：`student.li` / `Student@123456`
- 学生：`student.wang` / `Student@123456`

## 8. 当前未完成内容

- 登录、用户、班级、词表、诊断、训练、分析等业务接口
- 审计日志落库与幂等性拦截器的完整链路
- RAG 文档入库、召回、重排、教师建议问答接口
- 前后端真实业务契约联调
