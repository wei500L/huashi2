[根目录](../CLAUDE.md) > **app-server (业务后端)**

# app-server -- 主业务服务

## 模块职责

平台核心业务后端，基于 Spring Boot 4.0，提供用户认证授权、词汇管理、智能诊断、个性化训练、学情分析、AI 洞察调度和运维配置中心等全部业务 API。

## 入口与启动

- **Main class**: `com.huashi.eftransfer.app.AppServerApplication`
- **端口**: 8080 (默认)
- **启动命令**: `./mvnw -pl app-server -am spring-boot:run`
- **Profile**: `local` / `dev` / `prod`

## 对外接口

### 公开 API (`/api/...`)

| 模块 | Controller | 路径前缀 | 说明 |
|------|-----------|---------|------|
| auth | `AuthController` | `/api/auth` | 登录、刷新、注销、获取当前用户 |
| user | `AdminUserController` | `/api/admin/users` | 用户管理（Admin） |
| health | `HealthController` | `/api/health` | 健康检查 |
| lexicon | `LexicalPairController` | `/api/lexical-pairs` | 词对 CRUD、CSV 导入导出 |
| lexicon | `LexicalListController` | `/api/lexical-lists` | 词表管理 |
| diagnosis | `StudentDiagnosisTemplateController` | `/api/student/diagnosis-templates` | 学生查看已发布诊断模板 |
| diagnosis | `TeacherDiagnosisTemplateController` | `/api/teacher/diagnosis-templates` | 教师诊断模板 CRUD |
| diagnosis | `DiagnosisSessionController` | `/api/diagnosis/sessions` | 诊断 session 生命周期 |
| training | `TrainingPlanController` | `/api/training/plans` | 训练计划推荐 |
| training | `TrainingSessionController` | `/api/training/sessions` | 训练 session + 错题本 + 复习计划 |
| analytics | `StudentAnalyticsController` | `/api/student/analytics` | 学生学情分析 |
| analytics | `TeacherAnalyticsController` | `/api/teacher/analytics` | 教师班级分析 |
| analytics | `TeacherInterventionController` | `/api/teacher/interventions` | 教师干预记录 |
| ai | `AiInsightController` | `/api/ai` | AI 诊断解释、训练推荐 |
| ai | `TeacherAiController` | `/api/teacher` | 教师干预建议 |
| ai | `LexicalRagController` | `/api/ai/lexical-rag` | 词汇 RAG 查询 |
| opsconfig | `AdminAiConfigController` | `/api/admin/ai-config` | AI 运维配置管理 |

### 内部 API (`/internal/...`, 需 `X-Internal-Token`)

| Controller | 路径 | 说明 |
|-----------|------|------|
| `InternalKnowledgeController` | `/internal/knowledge` | 词汇知识导出（供 ai-gateway 拉取） |
| `InternalAiConfigController` | `/internal/ai-config` | 内部配置查询 |

## 关键依赖与配置

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.3 | Web + Security + Validation + Actuator + AMQP + Data Redis + JDBC + Flyway |
| MyBatis-Plus | 3.5.16 | ORM + 分页 |
| Redisson | 3.50.0 | Redis 客户端（token 存储） |
| JJWT | 0.12.6 | JWT 签发与验证 |
| Commons CSV | 1.11.0 | CSV 导入导出 |
| MySQL Connector | runtime | MySQL 驱动 |
| shared-kernel | 内部 | 共享枚举、DTO、事件 |

配置文件：
- `src/main/resources/application-local.yml` -- 本地开发配置
- `src/main/resources/application-dev.yml` -- 开发环境
- `src/main/resources/application-prod.yml` -- 生产环境
- `src/main/resources/logback-spring.xml` -- 日志配置

## 数据模型

数据库：MySQL (utf8mb4)，Flyway 迁移脚本 V1-V9。

### 核心实体

| 模块 | 实体 | 表 |
|------|------|------|
| user | `UserEntity`, `UserRoleEntity`, `TeacherProfileEntity` | 用户、角色、教师档案 |
| auth | (无独立实体, 使用 Redis) | JWT Token + Redis refresh/blacklist |
| lexicon | `LexicalPairEntity`, `LexicalPairSenseEntity`, `LexicalPairExampleEntity` | 词对、词义、例句 |
| lexicon | `LexicalTagEntity`, `LexicalPairTagRelEntity` | 标签 + 关联 |
| lexicon | `LexicalListEntity`, `LexicalListItemEntity` | 词表 + 条目 |
| diagnosis | `DiagnosisTemplateEntity`, `DiagnosisTemplateItemEntity` | 诊断模板 + 题目 |
| diagnosis | `DiagnosisSessionEntity`, `DiagnosisItemResultEntity`, `DiagnosisSummaryEntity` | 诊断会话 + 结果 + 摘要 |
| training | (多个实体) | 训练计划、训练会话、训练条目、错题本、复习计划 |
| analytics | (聚合表) | 学生/班级分析快照 |
| audit | `AuditLogEntity` | 审计日志 |
| opsconfig | (配置表) | AI 运维配置 |

### 数据库迁移

| 版本 | 脚本 | 说明 |
|------|------|------|
| V1 | `V1__init_base.sql` | 基础表 |
| V2 | `V2__init_auth_schema.sql` | 认证模式 |
| V3 | `V3__init_lexical_schema.sql` | 词汇模式 |
| V4 | `V4__init_diagnosis_schema.sql` | 诊断模式 |
| V5 | `V5__init_training_schema.sql` | 训练模式 |
| V6 | `V6__init_analytics_schema.sql` | 分析模式 |
| V7 | `V7__add_ai_generation_schema.sql` | AI 生成记录 |
| V8 | `V8__init_admin_ai_config.sql` | AI 运维配置 |
| V9 | `V9__training_session_resume_support.sql` | 训练会话恢复支持 |

## 测试与质量

测试框架：Spring Boot Test + JUnit 5 + H2 内存数据库 + Spring Security Test

| 类别 | 测试类 | 数量 |
|------|--------|------|
| 基础设施 | `TestAuthTokenStoreConfiguration`, `MockMvcTestSupport`, `AbstractWebIntegrationTest` | 3 |
| 安全 | `JwtTokenProviderTest`, `JwtAuthenticationFilterTest` | 2 |
| Auth | `AuthControllerIntegrationTest` | 1 |
| User | `AuthorizationAccessIntegrationTest` | 1 |
| Health | `HealthControllerTest` | 1 |
| Lexicon | `LexicalPairControllerIntegrationTest`, `LexicalPermissionAndListIntegrationTest`, `LexicalPairImportIntegrationTest` | 3 |
| Diagnosis | `RuleBasedDiagnosisScoringPolicyTest`, `DiagnosisErrorClassificationTest`, `DiagnosisSessionFlowIntegrationTest` | 3 |
| Training | `RuleBasedTrainingRecommendationEngineTest`, `TrainingSessionFlowIntegrationTest`, `TrainingReviewScheduleIntegrationTest` | 3 |
| Analytics | `AnalyticsIntegrationTest` | 1 |
| AI | `AiInsightIntegrationTest`, `LexicalRagQueryIntegrationTest`, `AiGatewayClientTest` | 3 |
| OpsConfig | `AdminAiConfigControllerIntegrationTest` | 1 |
| Internal | `InternalKnowledgeControllerIntegrationTest` | 1 |

## 常见问题 (FAQ)

- **Q: 如何新增业务模块？**
  A: 在 `modules/` 下创建子包（entity、mapper、service、controller、dto、vo），新增 Flyway 迁移脚本。

- **Q: 如何修改数据库结构？**
  A: 新增 `V{N}__description.sql` 迁移脚本，不修改已发布脚本。

- **Q: 事件机制如何工作？**
  A: 应用内事件（如 `DiagnosisCompletedEvent`）使用 Spring ApplicationEvent；跨服务事件（如 `LexicalKnowledgeChangedEvent`）走 RabbitMQ。

## 相关文件清单

```
app-server/
  pom.xml                                                    # Maven 配置
  src/main/java/com/huashi/eftransfer/app/
    AppServerApplication.java                                # Spring Boot 入口
    common/
      config/                                                # 配置类（JWT、Redis、MyBatis、AI Gateway Client）
      security/                                              # JWT 鉴权（Provider、Filter、Principal、Token Store）
      audit/                                                 # 审计日志
      util/                                                  # 工具类
    modules/
      auth/     controller/ dto/ vo/ service/                # 认证授权
      user/     controller/ entity/ mapper/ vo/ service/     # 用户管理
      health/   controller/ dto/ service/                    # 健康检查
      lexicon/  controller/ entity/ mapper/ dto/ vo/ service/ support/ # 词汇管理
      diagnosis/ controller/ entity/ mapper/ dto/ vo/ service/ support/ event/ # 诊断
      training/ controller/ entity/ mapper/ dto/ vo/ service/ # 训练
      analytics/ controller/ entity/ mapper/ vo/ service/    # 学情分析
      ai/       controller/ service/ entity/ mapper/         # AI 洞察
      opsconfig/ controller/ service/ entity/                # 运维配置
      internal/ controller/ service/                         # 内部 API
  src/main/resources/
    application-{local,dev,prod}.yml                         # 环境配置
    logback-spring.xml                                       # 日志
    db/migration/V1-V9                                       # Flyway 迁移脚本
  src/test/java/                                             # 23 个测试类
```

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-03-22 00:35:46 | 初始创建 | 全量扫描生成 |
