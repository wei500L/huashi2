# A3 React Bits 采用矩阵与依赖预算

决策日期：2026-08-05  
阶段：组件边界决策已复核；撤回项已同步，页面继续采用静态降级
验证声明：本轮只做源码、依赖和页面语义的静态审查，未启动页面，也未运行任何测试、lint、typecheck 或 build。

## 1. 适用边界

本矩阵把 React Bits 当作选定组件的源码参考，不把它当作需要安装的组件库。业务逻辑、API、权限、i18n 契约、React Query/Zustand 状态、保存与提交队列均保持不变；后续实现只能在现有业务组件的边界内替换表现层。

当前直接依赖基线来自 `package.json`：React 19、TypeScript、Tailwind 3.4、`framer-motion@^11.15.0`、ECharts 5.6 及现有表单/查询库。当前没有直接依赖 GSAP、OGL、Three.js，也没有为本轮安装任何依赖。

表中“实现变体与动效依赖”列统一解释为：写明 **Framer Motion** 即复用现有 motion；写明“不需要 motion”即静态 CSS/SVG/DOM；提到 GSAP/OGL/Three.js 只是说明 React Bits 原实现可能带来的风险，不能视为本项目许可。

### 状态含义

- **adopt**：语义与页面任务高度匹配；可在低保真验证通过后按现有 API 形态实现。
- **adapt**：可采用核心隐喻，但必须删去默认装饰、无限动效或重写为现有 Framer Motion/CSS/DOM 变体。
- **evaluate**：只允许在单一低频场景做原型；需要真实长文案、键盘、reduced-motion 和性能证据后再决定。
- **reject**：进入黑名单；不得作为默认实现或品牌锚点。若业务需要同名语义，使用现有静态组件重做。

## 2. 组件白名单、条件名单与黑名单

### 品牌：Focus / Route / Terrain

| 组件与来源 | 语义用途 | 目标路由 / 页面位置 | 实现变体与动效依赖 | 性能 / 可访问性风险 | 静态降级 | 当前状态 |
| --- | --- | --- | --- | --- | --- | --- |
| [Topography](https://reactbits.dev/backgrounds/topography) | 等高线/地形底图，表达迁移地形而非 AI 魔法 | 学生 `/diagnosis` 诊断结果或路线区；不要铺满 `/dashboard` | 优先 CSS/SVG 静态稀疏线；可用现有 Framer Motion 做一次性路线显现；不需要 GSAP/OGL/Three | 大面积纹理、blend、对比度会干扰正文；SVG 节点过多会增加绘制成本 | 移除纹理，仅保留 1 条路线、节点和文本图例 | adapt |
| [TrueFocus](https://reactbits.dev/text-animations/true-focus) | 突出一个 EN↔FR 词对、错误片段或下一步焦点 | 学生 `/diagnosis` 的单个诊断卡或 `/assessments/attempts/:attemptId` 当前题提示 | 只保留一次 focus/opacity 变化，改用现有 Framer Motion；不需要 GSAP/OGL/Three | 自动聚焦可能打断阅读，长法文可能被裁切；必须保留真实 heading/文本语义和键盘焦点 | 常态显示同一词对，增加颜色、下划线和“当前焦点”文本 | adapt |
| [StrokeText](https://reactbits.dev/text-animations/stroke-text) | 轮廓字/海报装饰 | 无目标业务位置；不用于 logo、CTA、数据或双语标题 | CSS `text-stroke` 也不建议；不需要 motion/GSAP/OGL/Three | 低对比、长英法文案和小屏可读性差，容易回到霓虹海报语气 | 使用普通实心标题和语义层级 | reject |
| [MaskedHeading](https://reactbits.dev/text-animations/masked-heading) | 通过裁切/遮罩表达路线揭示或章节转场 | 仅评估学生 `/diagnosis` 路线页的短章节标题；不放工作台首屏 | CSS `clip-path`/mask + 一次性 Framer Motion；不引入 GSAP/OGL/Three | mask/clip 在高对比、字体回退和 200% zoom 下可能截断字形；对长中文/法文不稳 | 直接渲染普通标题，保留 eyebrow、路线和副标题 | evaluate |
| [TextLoop](https://reactbits.dev/text-animations/text-loop) | 循环口号或自动轮播文案 | 无；不得作为全局品牌锚点或导航标题 | 即使可用 Framer Motion 也不采用；不需要 GSAP/OGL/Three | 替换任务信息、增加认知等待，与 reduced-motion 和读屏顺序冲突 | 固定一句与当前任务相关的说明 | reject |

### 学习：Progress / Evidence / Next step

| 组件与来源 | 语义用途 | 目标路由 / 页面位置 | 实现变体与动效依赖 | 性能 / 可访问性风险 | 静态降级 | 当前状态 |
| --- | --- | --- | --- | --- | --- | --- |
| [Stack](https://reactbits.dev/components/stack) | 将有限数量的训练阶段、证据或推荐卡叠成“下一站” | 学生 `/training` 推荐训练区；不作为管理员统计卡 | 静态 CSS 堆叠优先，点击后展开单张；若需进入/退出只用 Framer Motion；不需要 GSAP/OGL/Three | z-index/transform 会改变视觉顺序，堆叠内容可能被读屏重复；多层阴影和 3D 会放大合成成本 | 普通纵向列表或 stepper，所有项目同时可见；当前训练层已回退为单表面 + 可见按钮/键盘等价物，不采用堆叠/拖拽视觉 | reject |
| [CardSwap](https://reactbits.dev/components/card-swap) | 通过换卡浏览内容 | 无默认目标；不放 `/dashboard`、`/teacher/workspace` 或 `/admin/dashboard` 高频区 | 原实现常见连续位移、拖拽或 GSAP/scroll 驱动；本项目不引入 GSAP，不保留交互换卡 | 焦点顺序、触屏拖拽、历史回退和 reduced-motion 容易失真；隐藏卡片造成信息不可见 | 分页/列表 + 明确的上一张/下一张按钮 | reject |
| [Stepper](https://reactbits.dev/components/stepper) | 表达测评题目、诊断阶段或训练路线的顺序与完成状态 | 学生 `/assessments/attempts/:attemptId` 左侧题目导航，或 `/diagnosis` 路线阶段 | 语义化 `<ol>`/按钮为主，Framer Motion 只做一次状态强调；不需要 GSAP/OGL/Three | 必须支持 `aria-current="step"`、键盘跳转、未答/锁定/超时状态；不能只靠颜色 | 现有题号网格/进度文字继续作为完整等价物 | adopt |
| [CountUp](https://reactbits.dev/components/count-up) | 关键成果数字的一次性揭示 | 学生 `/dashboard` 进度成果；管理员 `/admin/dashboard` 指标仅在明确刷新后 | 不复制无 cleanup 的 RAF；优先静态数字，必要时用 Framer Motion 数值过渡且只触发一次；不需要 GSAP/OGL/Three | 多卡同时 RAF 会拖慢首屏，数字变化可能被读屏重复播报；必须尊重 reduced-motion | 直接显示最终格式化值，保留单位、时间范围和来源 | adapt |
| [AnimatedList](https://reactbits.dev/components/animated-list) | 新增/完成/待处理项目的顺序变化 | 教师 `/teacher/workspace` 今日待办和队列；学生 `/dashboard` 到期复习列表 | 使用现有 Framer Motion `layout`/短暂 enter；禁止无限循环和指针驱动；不需要 GSAP/OGL/Three | 列表重排会造成焦点漂移和读屏重复；必须稳定 key、保留加载/空态/错误态 | 普通列表按业务排序渲染，变化以文本状态或 `aria-live` 提示 | adapt |

### 工作台：Queue / Compare / Audit

| 组件与来源 | 语义用途 | 目标路由 / 页面位置 | 实现变体与动效依赖 | 性能 / 可访问性风险 | 静态降级 | 当前状态 |
| --- | --- | --- | --- | --- | --- | --- |
| [PillNav](https://reactbits.dev/components/pill-nav) | 胶囊式页面导航或 tab 切换 | 无默认目标；工作台主导航仍使用现有 shell 导航 | 不采用 cursor/磁吸/滑块追踪；不引入 GSAP/OGL/Three | 低宽屏易溢出，活动指示器会遮蔽焦点，胶囊化会抬高装饰权重 | 原生链接/`aria-current` 导航或标准 tabs | reject |
| [SpotlightCard](https://reactbits.dev/components/spotlight-card) | 指针聚光强调卡片 | 无默认目标；禁止放高频 `/teacher/workspace`、`/admin/dashboard` | 任何 pointer tracking、光晕、blend 均禁用；不需要 GSAP/OGL/Three | cursor-only 对触屏、键盘和 reduced-motion 无等价；每卡监听 pointer 会增加主线程工作 | 普通可聚焦边框/选中态，使用 `:focus-visible` 和状态色 | reject |
| [Masonry](https://reactbits.dev/components/masonry) | 不规则瀑布流展示资源 | 无默认目标；工作台的队列、指标、异常和审计不用瀑布流 | 若未来用于非关键资源画廊，仅 CSS columns；不需要 motion/GSAP/OGL/Three | 阅读顺序不稳定、列高差导致扫描和键盘顺序不一致，窄屏布局跳动 | 分组列表、表格或固定网格 | reject |

### 研究：Scan / Relation / Range

| 组件与来源 | 语义用途 | 目标路由 / 页面位置 | 实现变体与动效依赖 | 性能 / 可访问性风险 | 静态降级 | 当前状态 |
| --- | --- | --- | --- | --- | --- | --- |
| [Scanner](https://reactbits.dev/components/scanner) | 表达一次诊断/研究扫描的范围与结果 | 教师 `/teacher/research` 研究结果摘要，或学生 `/diagnosis` 加载完成后的证据区；不作为等待动画 | 仅评估 CSS/SVG 的一次性扫描线；若源码需要 Canvas/OGL/Three 则不引入；Framer Motion 可选 | 持续扫描会制造假进度、耗电并遮蔽结果；扫描线对低视力和 reduced-motion 不友好 | 静态“扫描范围/已完成”图例 + 结果计数和时间戳 | evaluate |
| [Radar](https://reactbits.dev/components/radar) | 多维能力/风险轮廓，帮助比较而非装饰 | 学生 `/analytics` 能力概览；管理员 `/admin/dashboard` 不替换现有 ECharts | 优先复用现有按需注册 ECharts；若需轻量 SVG，仅一次性绘制，Framer Motion 可选；GSAP/OGL/Three 为 0 | 轴标签拥挤、颜色区分不足和动态重绘影响读数；必须提供表格/文本摘要 | 维度列表 + 数值/变化，或当前 ECharts 图表 | adapt |
| [WebThreads](https://reactbits.dev/backgrounds/web-threads) | 稀疏关系线，表达词汇/班级迁移关联 | 教师 `/teacher/research` 词汇关系或班级共同障碍视图；不做全局背景、管理员控制台背景 | 源码若为 OGL/Canvas/Three 版本一律改为低密度 SVG/DOM；不引入新渲染库，Framer Motion 只做选中线强调 | WebGL 上下文、动画线条、对比度和读屏关系均有风险；节点/边必须有列表等价物 | 静态 SVG 关系图 + 可排序关系表，键盘可逐节点查看 | adapt |
| [ElasticSlider](https://reactbits.dev/components/elastic-slider) | 在连续范围中选择时间窗、强度或阈值 | 仅评估管理员 `/admin/dashboard` 时间范围筛选，不能替换现有表单控件 | 用原生 `<input type="range">` + Framer Motion 非必要；不引入 GSAP/OGL/Three | 弹性拖拽会丢失精确值、键盘步进和触屏可控性；不能只用视觉刻度 | 原生 range + 数字输入 + 明确 min/max/step | reject |
| [MorphSlider](https://reactbits.dev/components/morph-slider) | 形态变化的内容轮播/对比 | 无默认目标；研究和审计页面不以轮播隐藏证据 | 常见 SVG/GSAP/连续 morph 方案不采用；不引入 GSAP/OGL/Three | 轮播隐藏内容、焦点与读屏顺序不稳定，持续动画会遮蔽异常和证据 | 固定分段 tabs、并列对比或分页列表 | reject |

## 3. 采用门槛与依赖预算

### 白名单（可进入低保真实现）

- **adopt**：Stepper。
- **adapt**：Topography、TrueFocus、CountUp、AnimatedList、Radar、WebThreads。

这些组件仍需通过真实 `zh-CN`/`en-US` 文案、法文业务内容长度、键盘焦点、`prefers-reduced-motion`、375px 无横向溢出和 loading/empty/error/permission 状态复核后才能合入页面。

### 条件名单（只做原型，不承诺实现）

MaskedHeading、Scanner。原型必须有普通 DOM 等价物，且不得把装饰动画当作状态来源或进度来源。

### 黑名单

Stack、StrokeText、TextLoop、CardSwap、PillNav、SpotlightCard、Masonry、ElasticSlider、MorphSlider。黑名单尤其适用于 `/teacher/workspace`、`/admin/dashboard` 等高频工作台：不得使用 cursor-only、全屏 WebGL、玻璃拟态或默认霓虹样式。训练页当前保留 `LearningCardStack` 的单表面静态降级，不再引入 React Bits Stack。

### 运行时预算

1. 本轮新增运行时依赖预算为 **0**；不安装 React Bits 整库，不新增 GSAP、OGL、Three.js 或第二套动画库。
2. 后续若有例外，必须先提交 bundle 证据和路由级懒加载方案；单个新包建议控制在 gzip 20KB 以内，且每条路由最多一个新运行时包。无法证明收益时回退 CSS/DOM。
3. 动效统一复用现有 Framer Motion。单个视口默认最多一个持续中的动效表面；管理员控制台默认为 0 个持续动效，教师工作台只允许短暂状态变化。禁止无限 RAF、持续流动边框、自动轮播和指针驱动 3D/磁吸。
4. WebGL/Canvas 上下文预算为 **0**。需要关系或地形时使用静态 SVG/CSS；所有图、线和节点都必须有文本/列表等价物。
5. 所有候选必须有静态降级、真实长文案、键盘可达焦点、读屏顺序、`aria-live`（如状态确需播报）和 reduced-motion 分支；动效不能改变业务状态、路由或 API 时序。

## 4. 后续实现顺序

1. 先在学生 `/diagnosis` 与 `/assessments/attempts/:attemptId` 做 Topography、TrueFocus、Stepper 的 CSS/DOM 低保真对照。
2. 再在教师 `/teacher/workspace` 与 `/teacher/research` 验证 AnimatedList、WebThreads 的静态关系/队列变体；不把其变成全屏背景。
3. 最后在学生 `/analytics` 评估 Radar 与管理员 `/admin/dashboard` 的静态指标替代，保留现有 ECharts 按需注册，不在本轮替换图表引擎。
4. 每一项通过门槛后才允许复制 React Bits 的局部源码；未通过即使用静态降级并在本矩阵更新状态。
