[根目录](../CLAUDE.md) > **src (前端 SPA)**

# 前端 SPA -- ef-transfer-web

## 模块职责

基于 React 19 + TypeScript + Vite 构建的单页应用，服务三类用户角色：

- **学生 (STUDENT_WORKSPACE)**: 学习总览、智能诊断、个性化训练、自测练习、学情分析、错题与复习、AI 词汇检索助手
- **教师 (TEACHING_WORKSPACE)**: 班级总览、诊断模板管理、词对/词表管理、干预工作台
- **管理员 (ADMIN_CONSOLE)**: 用户管理、语料库管理、AI 运维配置中心

## 入口与启动

- **入口**: `src/main.tsx` -- 创建 React Query Client + BrowserRouter + App
- **路由**: `src/App.tsx` -- 全部路由定义，使用 `React.lazy` 页面级懒加载
- **启动**: `npm run dev` -> `http://localhost:3000`
- **构建**: `npm run build` (含 TypeScript 类型检查)
- **代理**: Vite 开发服务器将 `/api` 代理到 `http://localhost:8080`

## 对外接口

前端所有 API 调用统一走 `src/lib/services.ts`，底层使用 `src/lib/api.ts` 封装的 Axios 实例。

主要服务层：
- `authService` -- 登录、刷新、注销、获取当前用户
- `studentService` -- 学生分析（概览、趋势、热力图、散点图、高风险词对、错误分布）
- `diagnosisTemplateService` -- 诊断模板 CRUD
- `diagnosisSessionService` -- 诊断 session 生命周期（创建、答题、进度保存、完成、结果）
- `trainingService` -- 训练计划、错题本、复习计划、训练 session
- `aiService` -- 诊断解释、训练推荐、词汇 RAG 查询、教师干预建议、练习辅导/单题讲解
- `practiceService` -- 学生自测练习 session
- `publicAssessmentService` -- 公开研究问卷（`withCredentials` + `X-Requested-With`）
- `teacherAnalyticsService` -- 教师班级分析
- `teacherInterventionService` -- 教师干预记录
- `lexicalPairService` -- 词对 CRUD、CSV 导入导出
- `lexicalListService` -- 词表管理
- `adminService` -- 用户管理、AI 配置管理、RAG 重建索引

## 关键依赖与配置

| 依赖 | 用途 |
|------|------|
| react 19 + react-dom 19 | UI 框架 |
| react-router-dom 7 | 路由 |
| @tanstack/react-query 5 | 服务端状态管理 |
| zustand 5 | 客户端状态管理（auth store + UI store） |
| axios | HTTP 客户端（含 401 自动刷新） |
| echarts 5.6 | 图表（按需注册 via `src/lib/echarts.ts`） |
| tailwindcss 3.4 + tailwindcss-animate | 样式 |
| framer-motion 11 | 动画 |
| lucide-react | 图标 |
| react-hook-form + zod 4 | 表单验证 |

配置文件：
- `vite.config.ts` -- Vite 配置（别名、代理、手动分包）
- `tailwind.config.js` -- TailwindCSS 主题（HSL 变量、glass 效果、自定义动画）
- `tsconfig.app.json` -- TypeScript 严格模式
- `eslint.config.js` -- ESLint 9 配置

## 数据模型

前端类型定义集中在 `src/lib/contracts.ts`，与后端 VO/DTO 一一对应，主要类型：

- 用户与权限：`CurrentUserVO`, `Role`, `Capability`, `LoginResponse`
- 词汇：`LexicalPairSummaryVO`, `LexicalPairDetailVO`, `LexicalListSummaryVO`
- 诊断：`DiagnosisTemplateSummaryVO`, `DiagnosisSessionCreatedVO`, `DiagnosisResultDetailVO`
- 训练：`RecommendedTrainingPlanVO`, `TrainingNextItemVO`, `TrainingSessionSummaryVO`
- 分析：`StudentAnalyticsOverviewVO`, `ClassAnalyticsOverviewVO`, `AnalyticsTrendVO`
- AI：`AiGuidanceResponseVO`, `LexicalRagAnswerVO`, `AiOpsConfigPayload`
- 运维：`AdminAiConfigViewVO`, `AiGatewayHealthResponse`, `RagReindexJobResponse`

## 测试与质量

- 前端自动化测试：Vitest + Testing Library，命令 `npm test`（含练习页 a11y：`src/pages/practice/index.test.tsx`）
- 质量保障另含 `npm run lint` + `npm run typecheck`
- 构建验证 `npm run build`
- 包体积分析 `npm run build:analyze`

## 常见问题 (FAQ)

- **Q: 页面路由守卫如何工作？**
  A: `RequireAuth` 检查认证状态，`RequireCapability` 按 capability 校验访问权限。未认证跳转 `/login`，无权限跳转到用户默认首页。

- **Q: Token 刷新机制？**
  A: `src/lib/api.ts` 中 Axios 响应拦截器在遇到 401 时自动使用 refreshToken 刷新，支持并发请求共享同一个刷新 Promise。

- **Q: 如何添加新页面？**
  A: 在 `src/pages/` 下创建组件，在 `App.tsx` 中添加 `Route`（用 `React.lazy` 包裹），根据权限配置 `RequireCapability`。

## 相关文件清单

```
src/
  main.tsx                          # 入口
  App.tsx                           # 路由定义
  index.css                         # 全局样式
  lib/
    api.ts                          # Axios 封装 + 401 自动刷新
    contracts.ts                    # 全部 TypeScript 类型定义
    services.ts                     # API 服务层
    session.ts                      # 本地 session 存储
    format.ts                       # 格式化工具
    echarts.ts                      # ECharts 按需注册
    cursor.ts                       # 自定义光标
    axios.ts / compat-axios.ts      # Axios 兼容层
  store/
    index.ts                        # useAuthStore + useUIStore (Zustand)
    useAuthStore.ts                 # re-export
  components/
    layout/index.tsx                # AppLayout + Sidebar + Topbar + AssistantDrawer
    common/                         # 通用组件（ChartCard, EChart, WorkflowStepper 等）
  pages/
    Login.tsx                       # 登录页
    dashboard/index.tsx             # 学生总览
    diagnosis/index.tsx             # 智能诊断
    training/index.tsx              # 个性化训练
    practice/index.tsx              # 自测练习（假朋友题库，无计时整卷作答 + LLM 辅导）
    practice/index.test.tsx         # 拼写可访问名称与选项 radio 语义
    analytics/index.tsx             # 学情分析
    student/Errors.tsx              # 错题与复习
    student/Settings.tsx            # 设置
    teacher/Classes.tsx             # 班级总览
    teacher/ClassDetail.tsx         # 班级详情
    teacher/StudentDetail.tsx       # 学生详情
    teacher/Templates.tsx           # 诊断模板
    teacher/LexicalPairs.tsx        # 词对管理
    teacher/LexicalLists.tsx        # 词表管理
    teacher/Interventions.tsx       # 干预工作台
    admin/index.tsx                 # 用户管理
    admin/ConfigCenter.tsx          # AI 运维配置中心
    admin/LexicalPairs.tsx          # 语料库管理
    shared/LexicalPairsWorkspace.tsx # 词对管理共享组件
```

## 变更记录 (Changelog)

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-08-13 | 文档 | 更正「无前端自动化测试」；CI 已跑 `npm test` |
| 2026-08-13 | 审计第四批 | 公开问卷请求带 `X-Requested-With`；练习页 a11y Vitest；附件状态文案改为类型校验 |
