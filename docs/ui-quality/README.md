# EF.Transfer UI Quality

## 当前状态

| 字段 | 值 |
| --- | --- |
| 审计日期 | 2026-08-05 |
| 当前阶段 | A4 完整品牌审查完成；低风险视觉清理已落地，白名单组件仍以静态降级为准 |
| 审计方式 | 当前源码静态审查 + 历史 QA 截图复核 |
| 代码改动 | 仅做表现层与低风险组件清理；未改业务逻辑、API、权限、i18n 契约或未提交范围 |
| 验证边界 | 本轮未运行任何测试、lint、typecheck 或 build，也未启动页面；结论来自源码、路由、文案和历史 QA 证据 |

## 覆盖范围

- 全局基础：`src/index.css`、`tailwind.config.js`；未导入的 Vite `src/App.css` 已在 A4 删除。
- 共享层：`src/components/layout`、`src/components/common`；为解释截图，最小补读 onboarding、teacher workspace 组件和 ECharts 注册。
- 真实页面：学生 `/dashboard`、教师 `/teacher/workspace`、管理员 `/admin/dashboard`。
- 核心流程：学生 `/assessments/attempts/:attemptId`；历史移动训练截图仅作响应式补充证据。
- 完整品牌路径：登录 → 学生 `/dashboard` → `/diagnosis` 准备/运行/结果 → `/training`；教师 `/teacher/workspace` → 测评编辑/发布；管理员 `/admin/dashboard` → 配置中心或词对导入。
- QA 证据：2026-04-25 三角色 release/systematic QA 与 2026-07-19 学生答题 dogfood 截图。截图已在 `62a4e32` 从工作树清理，本审计从 `9e4c372` 只读提取；旧报告中的功能缺陷不自动视为当前开放问题。

## 已完成

- 建立按品牌识别、信息层级、组件状态、角色差异、响应式、性能风险分组的证据地图。
- 基于 A1 confirmed P0/P1 证据，确认 Lexical Cartography（语言迁移地图）为品牌叙事方向；将 React Bits 评估拆为已确认、候选、明确排除，定义三角色密度与比赛演示路径。
- 完成 18 个 React Bits 候选的采用矩阵：记录语义用途、目标路由、来源链接、实现变体、motion/GSAP/OGL/Three.js 边界、性能/可访问性风险、静态降级与 adopt/adapt/evaluate/reject 状态。
- 锁定依赖预算：本轮新增运行时依赖为 0，不安装 React Bits 整库，不新增 GSAP、OGL、Three.js 或第二套动画库；白名单优先复用现有 Framer Motion/CSS/DOM，WebGL/Canvas 上下文预算为 0。
- 标记 1 个 P0：管理员仪表盘、测评作答页及学生 Dashboard 局部硬编码中文，英文界面必然混排。
- 确认“通用 AI 紫 + 全域玻璃 + 大圆角 + 发光/磁吸/3D”同时覆盖导航、数据、反馈和操作，削弱业务层级与角色差异。
- 记录窄屏溢出、低动效缺口、首屏引导遮挡、异步状态不一致及历史 bundle 风险。
- 未改业务逻辑、API、权限、国际化契约或真实状态；本轮只做低风险表现层清理并同步 `docs/ui-quality/` 沉淀。

## A4 最终品牌审查（2026-08-05）

### 结论

Lexical Cartography 已形成可辨识的共享骨架：登录页用静态 EN→FR 语言焦点和稀疏等高线建立入口；学生以迁移地图、下一步和进展为主；教师以队列、风险和批量处理为主；管理员以时间范围、健康状态、异常和审计线索为主。共享颜色来自语义 token，角色差异来自信息密度与首要动作，不再靠三套皮肤。

### 本轮关闭的问题

- 删除登录页无语义光晕和循环语言焦点动画，保留静态来源/目标色与下划线；默认动效回到“状态才动”。
- 移除 `StatCard` 的 pointer tracking、3D rotate、磁吸组件和对应 CSS；数字仍保留一次性、可取消的 `requestAnimationFrame` 与 reduced-motion 降级。
- 将测评进度由紫粉渐变改为 `--progress`，将词对导入冲突/重复由紫色改为 amber 处理态；删除 WorkflowStepper 当前态的紫色阴影。
- 降低 onboarding 遮罩与 spotlight 对比，移除二次发光边框和 panel blur；训练 `LearningCardStack` 回退为单表面、可见按钮和键盘等价物。
- 将教师判断面板命名为 `DecisionCard`，不再保留会误导新窗口复用的 `SpotlightCard`；React Bits 矩阵已把 Stack 标记为 reject 并记录静态降级。
- 工作区首页不再在 Topbar 重复渲染第二行 route title；子页面仍保留标题上下文，页面自身继续提供唯一主标题。
- 删除未导入的 Vite `src/App.css`，消除孤立 3D transform 入口；全局样式入口收敛到 `src/index.css`。

### 遗留风险与不扩大范围的事项

- `AssessmentAttempt`、结果页、部分学生 Dashboard 及管理员配置/导入仍有硬编码中文；这是 i18n key 与真实长英法文案工作，不在本轮以视觉修复名义擅改。
- Topbar route title 与部分页面 `PageHeader`/工作区标题仍有重复，旧页面仍存在较多内层 border/背景块；信息架构和业务优先级需单独评审。
- onboarding 仍是可跳过的全屏模态；本轮只减弱遮罩和光晕，未改变出现时机、步骤或恢复逻辑。
- 当前没有运行时截图、focus/对比度、375px scrollWidth 或 bundle 数据；历史 ECharts 体积风险继续保持 blocked。
- ECharts 图表仍依赖图表自身的可读标签；若后续用于高风险审计页，应补充表格/文本摘要，不替换业务图表引擎。

### 维护建议

- 新增页面只使用 `--action/--progress/--teacher/--error` 等语义 token；禁止重新引入 `from-violet-*`、`to-fuchsia-*`、pointer tracking、磁吸、持续动画或全局 blur。
- React Bits 只作为源码参考；任何候选都必须先有真实中英/法文案、键盘焦点、静态等价物、reduced-motion 和错误状态证据，撤回时同步更新 `03-react-bits-adoption-matrix.md`。
- 继续保留现有 `liquid-glass`/`backdrop-blur` 兼容层的稳定不透明覆盖，后续基础清理再删除无效遗留 class；不要在业务页逐个恢复玻璃表面。
- 下一轮若获授权，优先做四个视口的静态截图与键盘走查，再处理 P0 文案/i18n 和信息层级；仍应避免整站换肤。

## 阅读顺序

1. [完整视觉证据审计](./01-global-visual-evidence-audit.md)
2. [品牌方向决策](./02-brand-direction-decision.md)
3. [React Bits 采用矩阵与依赖预算](./03-react-bits-adoption-matrix.md)
4. [最新交接](./handoff.md)
5. [设计决策日志](./decision-log.md)

## 下一条建议 Prompt

执行下一轮低保真视觉验证：先读 `02-brand-direction-decision.md` 与 `03-react-bits-adoption-matrix.md`，只用现有 CSS/DOM 和 Framer Motion 对学生 `/diagnosis`、`/assessments/attempts/:attemptId` 做 Topography、TrueFocus、Stepper 静态对照，再验证教师队列/关系视图。验证品牌 5 秒辨识、首屏任务可见性、真实长英法文案、键盘焦点、reduced-motion 与 375px 溢出；不安装新依赖，不做整站换肤。
