# UI Quality Handoff

更新时间：2026-08-05  
当前阶段：A4 完整品牌审查完成；低风险视觉清理已落地，下一步是带真实文案的静态截图与键盘走查。

## 新窗口最短接手路径

1. 读 [README](./README.md)。
2. 读 [A1 完整证据地图](./01-global-visual-evidence-audit.md)，优先看 BR-03、IH-01/02、CS-04、RD-01/02、RS-01、PF-01/02/03。
3. 读 [A2 品牌方向决策](./02-brand-direction-decision.md)，先区分已确认、候选和明确排除。
4. 读 [A3 React Bits 采用矩阵与依赖预算](./03-react-bits-adoption-matrix.md)，按 adopt/adapt/evaluate/reject 和静态降级执行。
5. 执行 `git status --short`，保留新窗口开始时的任何未提交修改；视觉原型阶段仍不改业务代码。
6. 进入低保真验证，只验证关键路径和白名单组件边界，不做整站换肤。

## 本轮结论

- 品牌方向：确认 **Lexical Cartography / 语言迁移地图**。EF.Transfer 是连接英语起点、迁移证据和法语目标的地图；AI 是测绘工具，不是主角。
- 品牌锚点：EN↔FR 迁移焦点 + 单条可读路线 + 证据图钉。颜色以墨色/纸张为基底，来源暖矿物色、目标冷水域色、进展亮色路线；紫色降为辅助。
- React Bits 白名单：Stepper 为 adopt；Topography、TrueFocus、CountUp、AnimatedList、Radar、WebThreads 为 adapt。MaskedHeading、Scanner 仅 evaluate；Stack、StrokeText、TextLoop、CardSwap、PillNav、SpotlightCard、Masonry、ElasticSlider、MorphSlider 为 reject；训练页使用 `LearningCardStack` 单表面静态降级。
- 依赖预算：本轮新增运行时依赖 0；不安装 React Bits 整库，不新增 GSAP、OGL、Three.js 或第二套动画库。动效复用现有 Framer Motion，WebGL/Canvas 上下文预算为 0；关系/地形优先 CSS/SVG/DOM，并必须提供文本或列表等价物。
- 高频工作台边界：`/teacher/workspace` 与 `/admin/dashboard` 禁止 cursor-only、全屏 WebGL、玻璃拟态、默认霓虹、自动轮播、持续 RAF 和指针驱动 3D/磁吸。
- 三角色：学生是路线行者，教师是地图编辑者，管理员是测绘控制台；共享地图语法但分别强调下一步、队列/对比、密度/审计。

- P0：`/admin/dashboard` 与 `/assessments/attempts/:attemptId` 大量硬编码中文，`/dashboard` 也有局部中文；英文 shell 下必然混排。当前 UI locale 仅中/英，法文按业务内容和排版韧性处理。
- P1 主线：同一套紫色、玻璃、3rem 圆角、渐变 CTA、发光边框、磁吸/3D 被用于所有层级和角色，形成通用 AI 皮肤。
- 信息层级：Topbar title + PageHeader + Hero 重复，卡片套卡片把任务、数据和状态拉成同一权重。
- 角色差异：业务路由与数据不同，但视觉语法基本相同；管理员尤其不应继续使用 3D StatCard 和持续发光图表。
- 状态：AssessmentAttempt 的保存/锁定/超时/提交确认值得保留；ChartCard/FeedbackState 是统一状态的基础，但页面、通知、Select/建议输入仍有缺口。
- 响应式：学生概览在 375px 存在 `p-10` 后容器小于 `min-w-[280px]` 的确定溢出条件。
- 性能基线：A3 已将全局过渡收窄到控件，fixed blend/大面积 blur 已有稳定覆盖；A4 进一步移除 StatCard 指针/3D 与登录循环动画。旧页面兼容 class 和 onboarding 视觉强度仍待后续基础清理。

## A4 最终品牌审查结论（2026-08-05）

### 覆盖路径

登录 → 学生 Dashboard → 诊断准备/运行/结果 → 训练；教师工作区 → 测评编辑/发布；管理员工作区 → 配置中心/词对导入。路由和权限仍由 `src/App.tsx` 原样负责。

### 已关闭的低风险不一致

- 登录页移除无语义光晕与循环语言焦点，保留静态 English → Français → 中文焦点。
- `StatCard` 不再响应指针做 3D/spotlight，删除未使用的 `Magnetic`；训练卡层删除透视、背层和拖拽，保留按钮/键盘切换。
- 测评进度使用 `--progress` 实色；导入冲突/重复使用 amber；Stepper 当前态不再带紫色阴影。
- onboarding 仅保留一次性轻量 spotlight，降低遮罩、去掉第二层发光与 blur。
- 教师判断面板改为 `DecisionCard`；`03-react-bits-adoption-matrix.md` 已将 Stack 撤回为 reject 并记录静态降级。
- 学生/教师/管理员工作区首页隐藏 Topbar 的重复 route title；子页面仍显示上下文标题。

### 角色品牌状态

- 学生：语言焦点、迁移地图、单条路线、进展与下一步可读，保留生命力但默认静止。
- 教师：工作台已用紧凑指标、待办、风险学生与活动队列，判断效率优先；发布流程用 WorkflowStepper 解释状态、回退和下一步。
- 管理员：配置中心/导入/仪表盘使用高密度不透明表面、健康状态和审计线索；不使用 3D、磁吸、持续发光或 WebGL。

## QA 证据说明

- 历史截图已由 commit `62a4e32` 从工作树删除，本轮从 `9e4c372` 只读提取查看，没有恢复到仓库。
- 关键路径：三角色 `qa-output/release-readiness/screenshots/prod-rerun-*.png`；核心流程 `dogfood-output/answering-flow/screenshots/assessment-*.png`；移动补充 `mobile-student-dashboard.png`、`training-mobile-375x812.png`。
- 旧 QA 报告中的功能缺陷多已注明复验关闭；不要把截图中的历史状态直接当成当前功能回归。

## 未验证风险与遗留问题

- 未启动当前页面，无法确认当前字体实际渲染、精确对比度、focus、软键盘和横向 scrollWidth。
- 没有当前英文长文案/法文业务内容截图；RS-04 保持 blocked。
- 未生成当前 bundle；历史 vendor/chart-engine 体积只作为待复核风险，PF-04 保持 blocked。
- 未检查所有页面，结论是全局样式 + 四条代表路径的证据基线，不是最终验收。
- `AssessmentAttempt`、结果页、部分学生 Dashboard 以及管理员配置/导入仍有硬编码中文；i18n key、长英法文案和日期/数字格式需要单独范围。
- Topbar route title 与页面 `PageHeader`/工作区标题仍有重复，旧页面仍有卡片套内层表面；信息架构调整需产品/业务确认后再做。
- onboarding 的全屏模态时机和步骤逻辑未改；本轮只降低遮罩/光晕。
- `liquid-glass`、`backdrop-blur` 等兼容 class 仍存在于旧 markup，但 `src/index.css` 已强制关闭 blur 并使用稳定 surface；后续可做无行为基础清理。

## 下一步

下一步维护：先用真实中英/法文案做登录、学生诊断/答题/结果、教师发布、管理员配置/导入的 1280×720、768、390、375px 静态截图与键盘走查；再处理 P0 i18n 和页面层级。任何 React Bits 候选必须先通过静态等价物、reduced-motion、状态矩阵和性能证据，不安装新依赖。

本轮未运行任何测试、lint、typecheck 或 build。

## 2026-08-09 公开研究答卷页动效与排版补充

- 路由：`/research/:releaseCode`，实现文件为 `src/pages/research/index.tsx`，样式集中在 `src/index.css` 的 `research-*` 区段。
- 信息架构改为“全局保存状态 + 作答路线进度 + 当前题位置 + 问题卡片”，桌面显示路线侧栏，860px 以下收敛为单栏，390px 下主操作、回答反馈、上一题依次排列。
- 选项点击反馈包括键位标识、选中轨、单选/多选控制图形、`已选择` 与 `本题回答已记录`；下一步文案区分“已记录，继续”“暂不回答，下一题”“开始作答”。未改变答题、自动保存、恢复、提交或权限/API 契约。
- 题目切换使用现有 Framer Motion 做 220ms 方向过渡；进度只动画 transform，选项只动画 transform/opacity/边框状态。继续遵守全局 `MotionConfig reducedMotion="user"`，没有新增 GSAP 或其他动画依赖。
- 本轮当前验证：研究专项测试 8/8、定向 ESLint、TypeScript、Vite 生产构建均通过；桌面 1440×1024、移动 390×844 浏览器截图与横向溢出检查通过。全仓 ESLint 仍被既有 `.tmp`、脚本和 `AssessmentEditor` 问题阻塞，未在本轮越界修复。
- 生产已按白名单部署到 `publishId=5 / RES-AFC02D0823F2`：双容器健康，二维码最终保持关闭；专用测试批次完成生成、分页、停用、422 统一错误码、Secure Cookie、70 项自动保存、完整提交/result 与加密 IP 审计验证。保留参与码未用于测试。
