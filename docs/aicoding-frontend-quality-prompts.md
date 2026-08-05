# EF.Transfer 前端比赛版 AI Coding Prompt Pack

> 目的：把 EF.Transfer Platform 打磨成具有独立品牌记忆点、成熟信息层级和真实业务可信度的前端产品。
>
> 使用方式：每次只复制一个完整的 Prompt 代码块到 AI Coding 会话。不要把所有任务一次性粘贴，以免上下文稀释和改动失控。

## 使用规则

1. 按推荐顺序逐条执行；每条 Prompt 都能独立理解，不依赖上一条会话的隐式记忆。
2. 执行前先读取 `git status --short`，保留当前工作区中与本任务无关的改动。
3. 每条任务只解决一个视觉层、一个组件族、一个页面或一条用户流程。
4. 本 Prompt Pack 明确不要求运行测试、lint、typecheck 或 build。优先源码、CSS、组件组合、状态分支、现有截图和文案的纯静态审查；只有静态审查无法判断时，才做最小范围的页面检查。
5. 不要安装整个 React Bits，也不要把它当成完整的可访问性设计系统。只选择任务中有语义价值的组件，复制或改写 TS/TW 源码，并适配 EF.Transfer 的 token 和现有依赖。
6. 如果 React Bits 组件需要 `motion/react`、GSAP、OGL、Three.js 等额外依赖，先评估是否值得；不要为了一个装饰效果引入整套运行时。复制 substantial source 时保留 React Bits 的版权与许可说明。
7. 每条任务结束时只返回：修改文件、设计判断、静态审查范围、未验证风险，并明确写出“本轮未运行任何测试”。

## 审计与跨窗口沉淀协议

全局审计、品牌决策、组件采用决策和最终验收不能只留在对话里。凡是任务中出现“审计、决策、证据、白名单、黑名单、遗留风险、下一步”这些内容，都要同步沉淀到 `docs/ui-quality/`，让新窗口可以通过仓库文件恢复上下文。

建议的持久化结构：

```text
docs/ui-quality/
├── README.md                         # 当前状态、阅读顺序、下一条建议 Prompt
├── 01-global-visual-evidence-audit.md # 全局视觉证据与优先级
├── 02-brand-direction-decision.md     # 品牌叙事、色彩、字体和角色气质
├── 03-react-bits-adoption-matrix.md   # 组件白名单、排除项和依赖预算
├── decision-log.md                    # 跨任务追加的设计决策与取舍
└── handoff.md                         # 最新交接摘要和未完成事项
```

审计类 Prompt 的执行要求：

- 先读取 `docs/ui-quality/README.md` 以及它链接的相关文档；不存在时创建最小可用结构。
- 事实、判断、建议和未验证假设分开写；每条重要结论尽量附真实路由、文件、组件、截图或状态证据。
- 不覆盖已有历史判断。若结论发生变化，在 `decision-log.md` 追加原因、影响和日期，并在当前文档标记最新状态。
- `README.md` 始终维护当前阶段、已完成任务、下一条建议 Prompt 和关键风险，控制在新窗口可快速阅读的长度。
- `handoff.md` 只保留最新交接摘要：本轮完成、当前视觉语言、已采用/排除组件、未验证风险和下一步。
- 不把完整源码、巨大截图或测试输出复制进审计文档；引用路径即可。本 Prompt Pack 已要求不运行测试。

新窗口的最短恢复方式：先读 `docs/ui-quality/README.md`，再读当前任务对应的审计文档和 `handoff.md`，最后执行 `git status --short`。

## React Bits 选择原则

React Bits 当前仓库更适合作为“动效与品牌组件源”，而不是 EF.Transfer 的基础控件库。下面的参考组件按用途分层：

| 层级 | 组件 | EF.Transfer 的建议用途 |
|---|---|---|
| 品牌锚点 | [Topography](https://reactbits.dev/backgrounds/topography)、[TrueFocus](https://reactbits.dev/text-animations/true-focus)、[StrokeText](https://reactbits.dev/text-animations/stroke-text) | 语言迁移地图、品牌标题、公开入口 |
| 语言叙事 | [VariableProximity](https://reactbits.dev/text-animations/variable-proximity)、[TextLoop](https://reactbits.dev/text-animations/text-loop)、[MaskedHeading](https://reactbits.dev/text-animations/masked-heading) | 英语/法语词汇切换、研究页、首屏 |
| 学习交互 | [Stack](https://reactbits.dev/components/stack)、[CardSwap](https://reactbits.dev/components/card-swap)、[Stepper](https://reactbits.dev/components/stepper)、[CountUp](https://reactbits.dev/text-animations/count-up) | 词汇卡、训练步骤、结果数字 |
| 工作台增强 | [PillNav](https://reactbits.dev/components/pill-nav)、[AnimatedList](https://reactbits.dev/components/animated-list)、[SpotlightCard](https://reactbits.dev/components/spotlight-card)、[Masonry](https://reactbits.dev/components/masonry) | Tabs、活动列表、单个重点行动、资源布局 |
| 研究隐喻 | [Scanner](https://reactbits.dev/backgrounds/scanner)、[Radar](https://reactbits.dev/backgrounds/radar)、[WebThreads](https://reactbits.dev/backgrounds/web-threads)、[ElasticSlider](https://reactbits.dev/components/elastic-slider) | 诊断扫描、语言关联、风险或难度选择 |
| 克制动效 | [FadeContent](https://reactbits.dev/animations/fade-content)、[AnimatedContent](https://reactbits.dev/animations/animated-content)、[Noise](https://reactbits.dev/animations/noise)、[HalftoneReveal](https://reactbits.dev/animations/halftone-reveal) | 进入、揭示、微纹理、成果封面 |

以下组件默认排除在核心工作台之外：FluidGlass、GlassSurface、SpecularButton、BorderGlow、ReflectiveCard、Aurora、SoftAurora、BlobCursor、GhostCursor、TargetCursor、SplashCursor、Galaxy、Hyperspeed、Lightning、Lanyard、DomeGallery、FlyingPosters、GlitchText、DecryptedText。除非某条任务明确说明使用场景，否则不要引入它们。

## 任务索引

| 阶段 | 任务 | 目标 |
|---|---|---|
| A | A1-A3 | 审计、品牌叙事和视觉决策 |
| B | B1-B4 | Token、字体、表面和动效基础 |
| C | C1-C5 | Shell 与共享组件 |
| D | D1-D2 | 登录、注册和公开研究入口 |
| E | E1-E4 | 学生工作区与学习核心路径 |
| F | F1-F3 | 教师工作区 |
| G | G1-G2 | 管理员工作区 |
| H | H1-H3 | 响应式、可访问性和最终品牌验收 |

---

## A1：全局视觉证据审计

```text
你正在改造 EF.Transfer Platform 的真实前端，而不是制作静态演示稿。

技术栈是 React 19 + TypeScript + Vite，使用 Tailwind CSS、Framer Motion、Lucide React、ECharts、React Router、React Query 和 Zustand。产品是英语-法语迁移诊断、训练与 AI 辅助教学平台，包含 STUDENT_WORKSPACE、TEACHING_WORKSPACE、ADMIN_CONSOLE 三类工作区。

当前界面已经有大量液态玻璃、紫色主色、渐变、发光边框、大圆角和装饰性动效。目标是建立专业但不冷漠、精确但不僵硬的“可靠语言学习实验室”，摆脱通用 AI 紫色皮肤。必须保留业务逻辑、API 契约、权限、国际化和当前未提交改动；不要用 mock 掩盖真实状态，不要把所有内容做成卡片。

本任务不要运行任何测试、lint、typecheck 或 build。优先采用纯静态审查；只有源码无法判断时才做最小范围页面检查。交付时明确写出“本轮未运行任何测试”。

请审查 `src/index.css`、`src/App.css`、`tailwind.config.js`、`src/components/layout`、`src/components/common`，以及学生、教师、管理员各一张真实页面和一张核心流程页面。结合现有 QA 截图，建立一张“视觉证据地图”：哪些地方造成廉价感、哪些地方缺少层级、哪些地方动效过度、哪些地方状态不完整、哪些地方在中英文/法文下会失控。

请把问题按“品牌识别、信息层级、组件状态、角色差异、响应式、性能风险”分组，给出 P0/P1/P2 优先级和证据位置。每一条至少包含：证据路径或路由、观察到的事实、用户影响、建议方向、验证方式和状态（confirmed / proposed / blocked）。

请创建或更新以下沉淀文件：
- `docs/ui-quality/README.md`：写入本次审计日期、覆盖范围、当前阶段、已完成任务、下一条建议 Prompt，并链接其他审计文档；
- `docs/ui-quality/01-global-visual-evidence-audit.md`：写入完整证据地图、优先级、角色差异、响应式问题、性能风险和明确排除项；
- `docs/ui-quality/handoff.md`：写入新窗口可以直接接手的摘要、未验证风险和下一步；
- `docs/ui-quality/decision-log.md`：只在本次产生了重要设计取舍时追加一条记录。

先读取已有文件再更新，不要覆盖历史判断。不要在本轮大规模改代码；只允许修正明显会阻塞后续工作的极小问题。审计文档应短、可检索、可引用，不要复制完整源码或巨大截图。
```

## A2：比赛级品牌叙事与视觉主张

```text
你正在改造 EF.Transfer Platform 的真实前端。这是 React 19 + TypeScript + Vite 的英语-法语迁移诊断、训练和 AI 教学平台，分为学生、教师、管理员工作区。当前风格偏液态玻璃、紫色渐变和发光边框；本轮要把它提升为有独立记忆点的比赛级产品，而不是继续做通用 AI SaaS。

保留业务逻辑、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先纯静态审查，必要时只做最小页面检查，并明确写出“本轮未运行任何测试”。

请先读取 `docs/ui-quality/README.md`、`01-global-visual-evidence-audit.md` 和 `handoff.md`，再基于 A1 的审计证据，为产品提出一条可落地的品牌叙事。优先探索“Lexical Cartography / 语言迁移地图”方向：英语与法语之间的焦点、路径、地形、关联和进展。评估 [Topography](https://reactbits.dev/backgrounds/topography)、[TrueFocus](https://reactbits.dev/text-animations/true-focus)、[StrokeText](https://reactbits.dev/text-animations/stroke-text)、[VariableProximity](https://reactbits.dev/text-animations/variable-proximity)、[TextLoop](https://reactbits.dev/text-animations/text-loop) 和 [WebThreads](https://reactbits.dev/backgrounds/web-threads) 哪些适合成为品牌锚点。

输出一份短的视觉决策：品牌关键词、图形隐喻、主/辅/语义颜色方向、字体气质、动效节奏、学生/教师/管理员的角色差异、适合比赛演示的三条关键路径。不要锁死具体像素和色值，不要复制 React Bits 默认紫色。

将结果写入 `docs/ui-quality/02-brand-direction-decision.md`，并同步更新 `README.md`、`handoff.md`，必要时在 `decision-log.md` 追加“为什么选择/排除某个品牌方向”的记录。把“已确认”“候选”“明确排除”分开，避免后续窗口把探索方案误当成最终规范。不要在本轮实现整站换肤。
```

## A3：React Bits 组件与依赖决策

```text
你正在改造 EF.Transfer Platform 的真实前端。项目使用 React 19、TypeScript、Vite、Tailwind 3.4、Framer Motion 和现有业务组件；React Bits 只能作为选定组件的源码参考，不要安装整个组件库。

保留业务逻辑、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查源码、依赖和页面语义，并明确写出“本轮未运行任何测试”。

请先读取 `docs/ui-quality/README.md`、`01-global-visual-evidence-audit.md`、`02-brand-direction-decision.md` 和 `handoff.md`，再检查 EF.Transfer 当前依赖和目标页面，针对下面组件做采用/改写/排除决策：
- 品牌：Topography、TrueFocus、StrokeText、MaskedHeading、TextLoop；
- 学习：Stack、CardSwap、Stepper、CountUp、AnimatedList；
- 工作台：PillNav、SpotlightCard、Masonry；
- 研究：Scanner、Radar、WebThreads、ElasticSlider、MorphSlider。

对每个候选说明：语义用途、页面位置、是否需要 motion/GSAP/OGL/Three.js、是否会带来性能或可访问性风险、是否应适配现有 Framer Motion、是否需要静态降级。禁止把 cursor-only、全屏 WebGL、玻璃拟态或默认霓虹组件放入高频工作台。

本轮不要求安装依赖或实现组件；产出一份可直接指导后续任务的组件白名单、黑名单和依赖预算。将结果写入 `docs/ui-quality/03-react-bits-adoption-matrix.md`，同步更新 `README.md`、`handoff.md`，重要取舍追加到 `decision-log.md`。每个组件记录语义用途、目标路由、来源链接、实现变体、依赖、性能/可访问性风险、静态降级和当前状态（adopt / adapt / evaluate / reject）。
```

## B1：色彩与语义 Token

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，英语-法语迁移学习产品，包含学生、教师、管理员工作区。当前是液态玻璃和紫色渐变基线，目标是成熟、和谐、可长时间使用的语言学习实验室。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/index.css`、`tailwind.config.js` 和真实页面，并明确写出“本轮未运行任何测试”。

请根据品牌决策建立颜色语义，而不是换一个主色值。至少覆盖：页面背景、表面层、正文、次要文字、边界、焦点、主操作、学习进展、AI 建议、成功、警告、错误、信息，以及学生/教师/管理员的辅助色。避免紫色从背景、卡片、按钮、图表到徽章全部重复。

优先调整 CSS 变量和 Tailwind 映射，保留 light/dark mode 的独立可读性。用真实中文、英文、法文和长数字静态检查对比度和层级。不要在本轮修改业务页面结构。
```

## B2：多语言字体与排版层级

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，面向英语、法语和中文文案的语言迁移学习产品。视觉目标是专业、精确、有编辑感，摆脱 AI 紫色皮肤。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查字体栈、排版 className 和真实 i18n 文案，并明确写出“本轮未运行任何测试”。

请检查 `src/index.css`、`src/lib/i18n`、页面标题、表格、表单、图表标签和结果文本，建立适合中英法三语的排版层级：display、page title、section title、body、label、metadata、numeric data。处理字重、字距、行高、数字等宽感、法文重音符号、中文 fallback、标题断行和小屏宽度。

可以参考 [TrueFocus](https://reactbits.dev/text-animations/true-focus)、[VariableProximity](https://reactbits.dev/text-animations/variable-proximity) 或 [StrokeText](https://reactbits.dev/text-animations/stroke-text) 的品牌文字理念，但不要把动态字体效果用于正文、表格、按钮或错误提示。
```

## B3：表面、边界、圆角与层级

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite 的英语-法语迁移教学产品。当前表面风格偏液态玻璃、大圆角和发光边框，本轮要建立更克制的材质层级。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态检查 `src/index.css`、共享卡片、弹窗、表格、侧栏和真实截图，并明确写出“本轮未运行任何测试”。

请重构表面语法：哪些区域是页面背景，哪些是稳定容器，哪些是可交互表面，哪些只需要边界或分隔线。建立有限的圆角层级、阴影层级、边框强度和 hover 提升方式。减少卡片套卡片、透明叠层、模糊玻璃和发光伪层。

可以参考 React Bits 的 SpotlightCard 作为单个重点表面的光线逻辑，但不要复制 GlassSurface、FluidGlass、BorderGlow 或 SpecularButton 的默认材质。要求信息密集页面优先保证文字、表格和控件的稳定对齐。
```

## B4：动效预算与 React Bits 适配层

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，已有 Framer Motion，目标是成熟、有节奏、可长时间使用的语言学习产品。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查现有 transition、Framer Motion、Tailwind animation 和 React Bits 源码，并明确写出“本轮未运行任何测试”。

请建立动效预算和适配原则：操作反馈、状态变化、页面进入、滚动揭示各自的节奏；哪些动效只出现一次；哪些动效不得持续运行；哪些动效在 `prefers-reduced-motion` 下变为静态。处理当前全局 `*` transition 过宽的问题，避免输入框、布局和弹窗迟滞。

为 React Bits 组件定义适配策略：优先复用现有 Framer Motion；GSAP/OGL/Three.js 只在品牌或诊断专属页面按需加载；每页最多一个持续 WebGL 场景；所有鼠标交互都要有键盘、触屏和静态降级。
```

## C1：桌面 App Shell

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，包含学生、教师、管理员工作区。当前 shell 使用玻璃侧栏、顶部栏和紫色 active 状态，目标是建立清晰、稳定、专业的产品骨架。

保留路由、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/components/layout/index.tsx`、`src/App.tsx`、现有截图和导航状态，并明确写出“本轮未运行任何测试”。

请只处理桌面 shell：品牌标识、工作区切换、主导航分组、active 状态、侧栏折叠、顶部标题、搜索、通知、账户区域和内容最大宽度。把 [LineSidebar](https://reactbits.dev/components/line-sidebar) 和 [PillNav](https://reactbits.dev/components/pill-nav) 当作交互参考，不要原样复制 cursor-only 行为或 GSAP 依赖。

导航要像一个可靠的工具，而不是漂浮装饰。检查长文案、折叠后图标语义、focus-visible、滚动容器和当前路由上下文。
```

## C2：移动 App Shell

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite 的多工作区学习平台。目标是让移动端仍然有品牌和秩序，而不是把桌面布局压缩成一列。

保留路由、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态检查移动侧栏、抽屉、顶部操作、底部安全区和已有移动截图，并明确写出“本轮未运行任何测试”。

请只处理 375px 左右和中等平板视口的 shell：移动导航抽屉、返回路径、标题和主操作、通知/账户入口、滚动锁定、焦点转移、触屏命中区域和横向溢出。可以参考 [StaggeredMenu](https://reactbits.dev/components/staggered-menu) 的进入节奏，但不要让菜单动画阻塞任务，也不要加入游标依赖。
```

## C3：操作控件族

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，使用 Tailwind、Lucide 和现有表单逻辑，服务英语、法语、中文真实文案。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态检查共享按钮、图标按钮、输入框、textarea、CustomSelect、RoundedSelect、checkbox、radio 和表单错误，并明确写出“本轮未运行任何测试”。

请统一操作控件的层级和完整状态：default、hover、pressed、focus-visible、disabled、loading、invalid、success。主色只表达主要行动，次要操作使用中性层级。需要参考 React Bits 时，优先借鉴 [SpecularButton](https://reactbits.dev/components/specular-button) 的边缘光线概念，但不要复制玻璃材质和持续扫光；输入和选择器始终优先可读性与键盘操作。
```

## C4：反馈、弹窗、空状态与错误状态

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，多工作区共享认证、通知、保存、恢复和错误反馈。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态追踪 `FeedbackState`、`AppErrorBoundary`、NotificationBell、弹窗和 session-runtime 的状态分支，并明确写出“本轮未运行任何测试”。

请统一 loading、skeleton、empty、error、permission denied、saving、saved、retry、success 和 destructive confirmation。每种状态都要回答“发生了什么、是否安全、下一步是什么”。不要用一个旋转图标或紫色 toast 覆盖所有语义。

可以参考 [FadeContent](https://reactbits.dev/animations/fade-content) 和 [AnimatedContent](https://reactbits.dev/animations/animated-content) 的进入方式，但状态内容必须即时可读，不能依赖动画才能理解。
```

## C5：Tabs、列表、步骤和数据展示

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，页面包含列表、表格、图表、筛选和多步骤流程。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/components/common`、ChartCard、EChart、通知、历史、导入和发布页面，并明确写出“本轮未运行任何测试”。

请建立可复用但不僵化的数据展示语法：页面级 Tabs 与局部 Tabs 的区别，列表与表格的密度，排序/筛选/分页，状态徽章，图表标题和异常解释。可以参考 [PillNav](https://reactbits.dev/components/pill-nav)、[AnimatedList](https://reactbits.dev/components/animated-list) 和 [Stepper](https://reactbits.dev/components/stepper)，但要改为语义化链接、button、list、aria-current 和受控状态。

不要把每行数据变成卡片，也不要让 stagger 动效遮挡滚动、键盘或屏幕阅读器。
```

## D1：登录、注册与账户入口

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，登录后进入英语-法语迁移诊断、训练和 AI 教学工作区。当前登录页已有渐变、玻璃和紫色倾向，比赛目标是形成品牌第一印象。

保留认证逻辑、表单校验、错误处理、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/pages/Login.tsx`、`Register.tsx`、`AccountAction.tsx` 和相关 CSS，并明确写出“本轮未运行任何测试”。

请将登录页定义为品牌入口而不是营销海报。可以组合 [Topography](https://reactbits.dev/backgrounds/topography) 的低对比度地图、[TrueFocus](https://reactbits.dev/text-animations/true-focus) 的语言焦点、[StrokeText](https://reactbits.dev/text-animations/stroke-text) 或 [MaskedHeading](https://reactbits.dev/text-animations/masked-heading) 的一次性标题揭示。

所有动态效果必须有静态降级、中文/英文/法文可读、表单焦点清晰、错误位置明确。不要把 WebGL 放进表单可读区域，不要复制 React Bits 默认紫色。
```

## D2：公开研究与品牌展示入口

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，产品包含研究测评和英语-法语迁移分析。该页面承担比赛展示和产品叙事，不是高频后台。

保留研究参与流程、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态检查 `src/pages/research`、公开入口和现有资源，并明确写出“本轮未运行任何测试”。

请打造一个具有学术编辑感的品牌展示页。可以探索 [TextLoop](https://reactbits.dev/text-animations/text-loop) 表达多语言路径，[WebThreads](https://reactbits.dev/backgrounds/web-threads) 表达词义关联，[HalftoneReveal](https://reactbits.dev/animations/halftone-reveal) 表达研究材料揭示，[MaskedHeading](https://reactbits.dev/text-animations/masked-heading) 表达成果标题。

效果只服务于“语言如何迁移”的叙事。避免常见的 AI 星空、Aurora、霓虹网格和无限滚动装饰；确保首屏、参与入口和研究说明在没有动画时仍然完整成立。
```

## E1：学生 Dashboard

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，学生工作区服务英语-法语迁移学习。目标是让学生一眼看懂当前阶段、最近进步和下一步行动。

保留查询、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/pages/dashboard/index.tsx`、学生布局、统计卡和训练计划，并明确写出“本轮未运行任何测试”。

请重构信息优先级：一个主行动、一个阶段进度、少量可信指标、最近活动和需要关注的薄弱点。可以使用 [Counter](https://reactbits.dev/components/counter)、[AnimatedList](https://reactbits.dev/components/animated-list) 和一个低强度 [SpotlightCard](https://reactbits.dev/components/spotlight-card)，但不要把所有模块做成彩色卡片，也不要让数字动画比含义更突出。

首屏要体现“语言迁移地图”的品牌方向，同时不影响学生快速开始学习。检查首次使用、无诊断、已有计划、加载和错误状态。
```

## E2：学生诊断入口与准备页

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，诊断是学生理解自身英语-法语迁移能力的关键入口。

保留诊断启动、模板选择、已有 session、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/pages/diagnosis/index.tsx`、`src/lib/diagnosis-launch.ts` 和相关启动组件，并明确写出“本轮未运行任何测试”。

请让准备页回答：这是什么、需要多久、会测什么、完成后得到什么、我现在可以做什么。可以参考 [Stepper](https://reactbits.dev/components/stepper) 表达准备阶段，也可以低强度借鉴 [Scanner](https://reactbits.dev/backgrounds/scanner) 的诊断扫描隐喻，但不要让扫描背景遮挡文案或持续占用资源。

处理首次进入、已有未完成 session、没有可用模板、网络异常和重新开始的区别。所有主要行动必须清楚、可键盘操作并有静态状态。
```

## E3：诊断、训练与测评运行时

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，诊断、训练和测评运行时包含自动保存、恢复、离开保护、提交、超时和结果生成。

保留 `src/features/session-runtime`、诊断/训练/测评业务逻辑、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态追踪状态机、保存队列、按钮条件和页面结构，并明确写出“本轮未运行任何测试”。

请统一进行中体验：题目层级、当前/总数、已保存状态、剩余时间、退出、恢复、冲突、提交确认和失败重试。可以适配 [Stepper](https://reactbits.dev/components/stepper) 作为进度语法，但必须改为受控业务状态；不要让 React Bits 自己决定提交或完成逻辑。

动效只能解释状态变化，不能延迟题目、改变控件尺寸或让用户误以为数据已保存。检查键盘、触屏、长文案和低动效模式。
```

## E4：词汇训练、错题与复习

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，学生通过词对、例句、错题和复习计划完成英语-法语迁移训练。

保留训练数据、错题逻辑、保存/恢复、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/pages/training/index.tsx`、`src/pages/student/Errors.tsx`、`src/pages/student/History.tsx` 和 session runtime，并明确写出“本轮未运行任何测试”。

请选择一种清晰的学习交互，不要同时堆多个动画：优先评估 [Stack](https://reactbits.dev/components/stack) 或 [CardSwap](https://reactbits.dev/components/card-swap) 表达词卡/例句/语义解释的层级，结合 [FadeContent](https://reactbits.dev/animations/fade-content) 做内容切换。

所有“掌握、再练一次、加入复习、跳过、查看解释”都必须有显式按钮和键盘路径。卡片滑动只是增强，不是唯一操作方式。检查空错题、未来复习、训练完成和网络恢复状态。
```

## E5：学生结果、分析与历史

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，学生结果页需要把分数转化为可执行的学习下一步。

保留 ECharts、结果数据、导出、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/pages/analytics/index.tsx`、`src/pages/student/AssessmentResult.tsx`、`History.tsx`、`Errors.tsx` 和图表组件，并明确写出“本轮未运行任何测试”。

请重排结果层级：结论、证据、薄弱点、趋势、推荐行动。可以使用 [CountUp](https://reactbits.dev/text-animations/count-up) 或 [Counter](https://reactbits.dev/components/counter) 作为有限的数字揭示，使用 [HalftoneReveal](https://reactbits.dev/animations/halftone-reveal) 作为非关键成果封面，但不要让动画取代图表轴、单位、解释文字或无障碍摘要。

检查无历史、数据为空、图表加载失败、导出失败和中英法长文案下的布局。
```

## F1：教师工作区总览与班级

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，教师工作区是专业教学工作台，需要比学生端更紧凑、更可扫描。

保留教师权限、查询、批量操作、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/features/teacher-workspace`、`src/pages/teacher/Workspace.tsx`、`Classes.tsx`、`ClassDetail.tsx` 和相关共享组件，并明确写出“本轮未运行任何测试”。

请处理总览、班级卡片/列表、筛选、最近活动、风险学生、待处理任务和主操作。可以参考 [PillNav](https://reactbits.dev/components/pill-nav) 做班级视图切换，参考 [AnimatedList](https://reactbits.dev/components/animated-list) 做待办进入，但不让每一行数据跳动或变成重型卡片。

教师要快速知道“什么需要我现在处理”，而不是被装饰吸引。检查长班级名称、零数据、权限差异和表格密度。
```

## F2：教师测评、模板与导入发布

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，教师需要创建、编辑、导入、审核和发布诊断/测评内容。

保留表单、草稿、导入预检、审核、发布、保存队列、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `AssessmentEditor.tsx`、`TemplateDraftEditor.tsx`、`LexicalImportCenter.tsx`、`TeacherAssessmentPublishDetail.tsx` 和相关 service，并明确写出“本轮未运行任何测试”。

请把复杂流程拆成可理解的阶段：输入、校验、预览、修复、发布、完成。可以适配 [Stepper](https://reactbits.dev/components/stepper) 表达流程，也可以使用 [AccordionGallery](https://reactbits.dev/components/accordion-gallery) 的“展开查看”理念来组织样例，但不要为了视觉而隐藏校验错误。

每个阶段都要有当前状态、未完成原因、回退路径、保存状态和下一步动作。导入失败、部分成功、重复项和权限拒绝必须优先可读。
```

## F3：教师学生详情与干预

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，教师通过学生详情、班级详情和干预页面进行教学判断。

保留学生数据、隐私边界、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `StudentDetail.tsx`、`ClassDetail.tsx`、`Interventions.tsx` 和相关图表/状态组件，并明确写出“本轮未运行任何测试”。

请提升“证据到行动”的链路：学生当前状态、具体薄弱点、证据、建议干预、已采取行动、后续观察。只允许用一个 [SpotlightCard](https://reactbits.dev/components/spotlight-card) 突出最重要洞察，避免所有指标都发光或动画化。

处理隐私遮蔽、无数据、学生退出班级、干预失败、批量操作和长时间页面滚动。教师需要的是可信判断，不是漂亮的统计墙。
```

## G1：管理员 Dashboard、用户和审计

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，管理员工作区优先可信、可扫描、可审计，而不是品牌展示页。

保留管理员权限、用户状态、审计日志、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/pages/admin/Dashboard.tsx`、`index.tsx`、`AuditLogs.tsx`、通知和表格组件，并明确写出“本轮未运行任何测试”。

请优化系统健康、用户列表、审计日志、筛选、排序、分页和危险操作。只使用中性层级和语义颜色；不要把 React Bits 的霓虹、玻璃、游标和 3D 组件带入管理员核心页面。必要时可借鉴 [AnimatedList](https://reactbits.dev/components/animated-list) 的渐进呈现，但表格与审计信息必须稳定。

每个高风险操作都要具体说明影响、范围和回退方式。关注 ID、时间、状态、错误、长名称和权限不足。
```

## G2：管理员配置、词库与导入中心

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，管理员需要处理配置、词库、导入、预检、冲突和部分成功。

保留加密配置、导入契约、预检结果、权限、API、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `ConfigCenter.tsx`、admin/lexical 页面、shared import 组件和服务，并明确写出“本轮未运行任何测试”。

请建立“查看当前值 -> 编辑 -> 校验 -> 预览影响 -> 保存/发布”的清晰层级。可以适配 [Stepper](https://reactbits.dev/components/stepper) 和 [PillNav](https://reactbits.dev/components/pill-nav)，但不要将配置页面做成游戏化卡片或模糊玻璃面板。

错误、冲突、部分成功、重试、未保存和权限拒绝必须在视觉和文案上可区分。优先稳定、可审计和可回退。
```

## H1：AI 助手与通知体验

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，AI 助手只是学习和教学工作流中的辅助能力，不是整个产品的视觉中心。

保留 AI 请求、引用、上下文、降级、通知、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查 `src/components/layout/index.tsx`、`NotificationBell.tsx`、AI 结果渲染、引用和动作组件，并明确写出“本轮未运行任何测试”。

请让 AI 答案、解释、证据、引用、建议动作、fallback 和失败状态形成清晰层级。可以参考 [AnimatedList](https://reactbits.dev/components/animated-list) 做通知或引用的渐进出现，但不要使用 TextType、DecryptedText、GlitchText 制造“模型正在思考”的廉价延迟。

AI 入口应使用中性品牌色或专属语义色，不能把每个页面染成紫色。回答必须在无动画时完整可读，引用必须可展开、可追溯。
```

## H2：响应式与可访问性静态加固

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，多语言、多角色、长表格、图表和运行时页面都要在真实视口成立。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；只做源码、CSS、组件语义和已有截图的静态审查，必要时做最小视口页面检查，并明确写出“本轮未运行任何测试”。

检查 375px 手机、平板、常规笔记本和宽屏：侧栏抽屉、固定头部、表格横向滚动、图表裁切、弹窗、底部安全区、长中文/英文/法文、键盘焦点、触屏命中区域、`prefers-reduced-motion`、颜色对比度、aria 语义和 WebGL 降级。

React Bits 的所有鼠标/滚动效果都必须有静态和键盘替代。不要通过缩小字体或隐藏内容解决响应式问题。
```

## H3：比赛级最终品牌验收

```text
你正在改造 EF.Transfer Platform 的真实前端：React 19 + TypeScript + Vite，目标是一个能在比赛中被记住、同时经得起真实使用的英语-法语迁移学习产品。

保留业务、API、权限、国际化和未提交改动。本任务不要运行任何测试、lint、typecheck 或 build；优先静态审查源码、路由、截图、真实文案和状态矩阵，必要时做最小页面检查，并明确写出“本轮未运行任何测试”。

请先读取 `docs/ui-quality/README.md`、`01-global-visual-evidence-audit.md`、`02-brand-direction-decision.md`、`03-react-bits-adoption-matrix.md` 和 `handoff.md`，再从以下完整路径做最终品牌审查：登录 -> 学生 Dashboard -> 诊断准备 -> 诊断运行时 -> 结果/训练；教师工作区 -> 测评发布；管理员工作区 -> 配置或导入。检查是否形成同一品牌、不同角色气质：学生有生命力，教师有判断效率，管理员有系统可信度。

重点清理：残留 AI 紫色、过度玻璃、卡片套卡片、无意义发光、全局慢过渡、重复动画、默认 React Bits 色值、游标依赖、不可解释图表和没有静态降级的 WebGL。保留真正有语义价值的品牌锚点：语言焦点、迁移地图、路径、扫描和进展。

只修复低风险且证据明确的不一致；对于信息架构或业务含义变化，列出问题和选项，不要擅自扩大范围。将最终状态、已关闭问题、遗留风险和下一步维护建议写入 `docs/ui-quality/README.md` 和 `handoff.md`，把重要变化追加到 `decision-log.md`。如果某个 React Bits 组件最终被撤回，必须同步更新 `03-react-bits-adoption-matrix.md`，避免新窗口再次引入。
```

---

## 组件取舍口诀

```text
先用层级，再用颜色；先用颜色，再用动效；最后才用 WebGL。
一个页面一个视觉锚点；一个动效一个语义；一个重型依赖一个明确场景。
```

## 交付格式

每条 Prompt 完成后，要求 AI Coding 只返回以下内容：

```text
本轮完成：
- 修改文件：
- 解决的体验问题：
- 采用或排除的 React Bits 组件：
- 静态审查范围：
- 页面检查（如有）：
- 依赖变化（如有）：
- 未验证风险：
- 本轮未运行任何测试。
```

## 许可与第三方说明

React Bits 当前仓库使用 MIT + Commons Clause。将其源码复制到 EF.Transfer 产品时，应保留版权与许可说明；可以作为产品的一部分使用，但不能把组件源码本身单独打包、转售或重新分发为组件库。
