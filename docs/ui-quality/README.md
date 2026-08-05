# EF.Transfer UI Quality

## 当前状态

| 字段 | 值 |
| --- | --- |
| 审计日期 | 2026-08-05 |
| 当前阶段 | A3 React Bits 采用矩阵与依赖预算已完成；尚未进入组件实现 |
| 审计方式 | 当前源码静态审查 + 历史 QA 截图复核 |
| 代码改动 | 仅更新 `docs/ui-quality/` 决策文档，无业务代码改动 |
| 验证边界 | 本轮未运行任何测试、lint、typecheck 或 build，也未启动页面 |

## 覆盖范围

- 全局基础：`src/index.css`、`src/App.css`、`tailwind.config.js`。
- 共享层：`src/components/layout`、`src/components/common`；为解释截图，最小补读 onboarding、teacher workspace 组件和 ECharts 注册。
- 真实页面：学生 `/dashboard`、教师 `/teacher/workspace`、管理员 `/admin/dashboard`。
- 核心流程：学生 `/assessments/attempts/:attemptId`；历史移动训练截图仅作响应式补充证据。
- QA 证据：2026-04-25 三角色 release/systematic QA 与 2026-07-19 学生答题 dogfood 截图。截图已在 `62a4e32` 从工作树清理，本审计从 `9e4c372` 只读提取；旧报告中的功能缺陷不自动视为当前开放问题。

## 已完成

- 建立按品牌识别、信息层级、组件状态、角色差异、响应式、性能风险分组的证据地图。
- 基于 A1 confirmed P0/P1 证据，确认 Lexical Cartography（语言迁移地图）为品牌叙事方向；将 React Bits 评估拆为已确认、候选、明确排除，定义三角色密度与比赛演示路径。
- 完成 18 个 React Bits 候选的采用矩阵：记录语义用途、目标路由、来源链接、实现变体、motion/GSAP/OGL/Three.js 边界、性能/可访问性风险、静态降级与 adopt/adapt/evaluate/reject 状态。
- 锁定依赖预算：本轮新增运行时依赖为 0，不安装 React Bits 整库，不新增 GSAP、OGL、Three.js 或第二套动画库；白名单优先复用现有 Framer Motion/CSS/DOM，WebGL/Canvas 上下文预算为 0。
- 标记 1 个 P0：管理员仪表盘、测评作答页及学生 Dashboard 局部硬编码中文，英文界面必然混排。
- 确认“通用 AI 紫 + 全域玻璃 + 大圆角 + 发光/磁吸/3D”同时覆盖导航、数据、反馈和操作，削弱业务层级与角色差异。
- 记录窄屏溢出、低动效缺口、首屏引导遮挡、异步状态不一致及历史 bundle 风险。
- 未改业务逻辑、API、权限、国际化契约或真实状态；本轮只新增 `docs/ui-quality/` 沉淀。

## 阅读顺序

1. [完整视觉证据审计](./01-global-visual-evidence-audit.md)
2. [品牌方向决策](./02-brand-direction-decision.md)
3. [React Bits 采用矩阵与依赖预算](./03-react-bits-adoption-matrix.md)
4. [最新交接](./handoff.md)
5. [设计决策日志](./decision-log.md)

## 下一条建议 Prompt

执行下一轮低保真视觉验证：先读 `02-brand-direction-decision.md` 与 `03-react-bits-adoption-matrix.md`，只用现有 CSS/DOM 和 Framer Motion 对学生 `/diagnosis`、`/assessments/attempts/:attemptId` 做 Topography、TrueFocus、Stepper 静态对照，再验证教师队列/关系视图。验证品牌 5 秒辨识、首屏任务可见性、真实长英法文案、键盘焦点、reduced-motion 与 375px 溢出；不安装新依赖，不做整站换肤。
