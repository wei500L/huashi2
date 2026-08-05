# A1 全局视觉证据审计

审计日期：2026-08-05  
阶段：evidence baseline，未进入品牌定案  
验证声明：本轮未运行任何测试、lint、typecheck 或 build，也未启动页面。

## 1. 取证边界与状态定义

- `confirmed`：当前源码或历史截图可直接证明事实；建议仍需后续设计验证。
- `proposed`：风险有源码依据，但具体表现或方案需在目标视口/语言下确认。
- `blocked`：本轮限制禁止 build/页面检查，无法获得当前运行时或 bundle 数据。
- 历史 QA 截图来自 `9e4c372`；当前 HEAD 已删除图片。引用格式 `git:9e4c372:<path>` 可复现，不把旧报告已关闭的功能问题重新打开。
- 当前 UI 契约只支持 `zh-CN`、`en-US`（`src/lib/i18n.ts:6-16`）。本审计中的“法文”指词汇、题干、例句等真实业务内容及未来排版韧性，不擅自新增 `fr-FR` 界面契约。

## 2. 覆盖证据

| 对象 | 路由 / 证据 |
| --- | --- |
| 学生真实页 | `/dashboard`；`src/pages/dashboard/index.tsx:323-964`；`git:9e4c372:qa-output/release-readiness/screenshots/prod-rerun-student-dashboard.png` |
| 教师真实页 | `/teacher/workspace`；`src/features/teacher-workspace/TeacherWorkspacePage.tsx:178-483`；`git:9e4c372:qa-output/release-readiness/screenshots/prod-rerun-teacher-workspace.png` |
| 管理员真实页 | `/admin/dashboard`；`src/pages/admin/Dashboard.tsx:259-408`；`git:9e4c372:qa-output/release-readiness/screenshots/prod-rerun-admin-dashboard-direct.png` |
| 核心流程 | `/assessments/attempts/:attemptId`；`src/pages/student/AssessmentAttempt.tsx:529-855`；`git:9e4c372:dogfood-output/answering-flow/screenshots/assessment-attempt-initial.png`、`assessment-submit-confirm.png` |
| 移动补充 | `git:9e4c372:qa-output/systematic-qa/2026-04-25/screenshots/mobile-student-dashboard.png`；`git:9e4c372:dogfood-output/answering-flow/screenshots/training-mobile-375x812.png` |

## 3. 视觉证据地图

### 品牌识别

| ID / 优先级 | 证据位置 | 观察到的事实 | 用户影响 | 建议方向 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| BR-01 / P1 | `src/index.css:8-26,34-58,189-238,392-475`；`tailwind.config.js:19-79` | primary、accent、ring、暗色 border 共用紫色；背景、玻璃、渐变字、液态按钮和发光边框继续叠加紫/粉/蓝。 | 产品首先像通用 AI 模板，英法迁移诊断的专业性和记忆点被装饰覆盖。 | A2 先定义语言迁移的图形隐喻与颜色语义，再降低紫色为有限辅助色；不要只做“换一个主色”。 | 对登录、三角色首页、答题流程做去色灰阶层级检查，再做 light/dark 对照。 | confirmed |
| BR-02 / P1 | `src/components/layout/index.tsx:238-257`；`src/index.css:407-435`；三角色 QA 截图 | 品牌标记是 Sparkles + 旋转方框；关键 CTA 使用持续流动的紫粉渐变和强阴影，截图中的发光按钮成为最强视觉物。 | 品牌锚点指向“AI/魔法”，而非词义对应、迁移路径、证据与学习进展。 | 用 EN↔FR 焦点、路径、坐标或词形差异建立单一品牌锚点；主操作保持稳定实色/低动效。 | 5 秒辨识测试：隐藏产品名后，受试者能否判断为语言迁移学习，而非 AI 助手。 | confirmed |
| BR-03 / P0 | `src/pages/admin/Dashboard.tsx:25-45,261-395`；`src/pages/student/AssessmentAttempt.tsx:529-849`；`src/pages/dashboard/index.tsx:387-424,736-745` | 管理员页未接入 `useTranslation` 且数字/日期固定 `zh-CN`；测评流程及学生页局部直接写中文。英文切换只改变 shell，页面主体保持中文。 | 英文用户得到混合语言界面；长英文/法文内容的真实断行也被中文短文案掩盖，属于发布级一致性阻断。 | 保留现有 i18n 契约，把硬编码迁移到现有 key/locale formatter；法文先作为真实内容长度基线，不在 A1 扩语言范围。 | 分别以 `zh-CN`、`en-US` 打开四条路由；注入真实长法文词、题干和说明，检查换行与日期/数字格式。 | confirmed |
| BR-04 / P2 | `src/App.css:1-184`；`src/main.tsx:10`；A4 静态引用搜索 | `App.css` 是未导入的 Vite 模板遗留；实际只加载 `index.css`，文件内还残留无业务意义的 Vite 3D transform。 | 不直接影响当前画面，但给后续设计改造制造错误入口和失效判断。 | 已删除孤立文件，保留 `index.css` 作为唯一全局样式入口。 | 静态引用搜索确认无导入；未启动页面。 | closed |

### 信息层级

| ID / 优先级 | 证据位置 | 观察到的事实 | 用户影响 | 建议方向 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| IH-01 / P1 | `src/components/layout/index.tsx:932-948`；`src/components/common/index.tsx:289-311`；学生 `:344-374`、教师 `:211-216`、管理员 `:298-317` | 同一屏依次出现 Topbar 当前页名、PageHeader 大标题、3rem Hero/概览大标题；三角色截图均显示重复标题和大面积欢迎区。 | 首屏被“告诉用户在哪里”占满，真实任务、风险与待处理项下沉；标题权重彼此竞争。 | 每页只保留一个 page title 和一个任务锚点；Hero 仅用于确有叙事价值的页面，工作台改为结论/待办先行。 | 在 1280×720 与 375×812 检查首屏是否能看到角色第一任务及其状态。 | confirmed |
| IH-02 / P1 | 学生 `src/pages/dashboard/index.tsx:344-956`；教师 `TeacherWorkspacePage.tsx:280-478`；`features/teacher-workspace/components.tsx:24-108`；管理员 `Dashboard.tsx:319-400` | section 是玻璃大卡，内部指标、列表、空态、图表再套圆角卡；学生 Dashboard 一页连续出现大量独立表面。 | 所有内容都像同等重要的模块，扫描路径变慢，页面显得“组件展厅”而非可靠工作台。 | 先用分组、留白、分隔线、表格/列表建立层级；卡片只保留给可独立操作或需明确边界的对象。 | 关闭背景、阴影和圆角后，任务顺序仍应清楚；统计卡数量和嵌套层级做清单复核。 | confirmed |
| IH-03 / P2 | `src/components/common/index.tsx:65-73,289-305`；教师 `components.tsx:12-18,27-32` | 9–10px、全大写、0.26–0.3em 字距的 eyebrow 被用于导航分组、指标和区块；中文不受 uppercase 影响，英文会显著变宽，低对比文字密集。 | 中文/英文呈现节奏不一致，法文重音和长标签容易形成稀疏断行；低视力与窄屏扫描困难。 | 建立中英法共享的 label/metadata 字级，不以超宽字距制造“高级感”；数字、标题、说明分别定级。 | 使用中文 8 字、英文 24 字符、法文含重音 28 字符的同位快照比较。 | proposed |

### 组件状态

| ID / 优先级 | 证据位置 | 观察到的事实 | 用户影响 | 建议方向 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| CS-01 / P1 | `src/components/common/ChartCard.tsx:25-122`；`FeedbackState.tsx:27-151`；学生 `Dashboard.tsx:338-341,452-455,688-731,748-756` | ChartCard/FeedbackState 已覆盖 loading、empty、permission、error、retry；真实页面仍大量使用一行文字或临时圆角 banner，状态层级和可恢复动作不一致。 | 同一错误在图表、列表、页面区块中严重程度看起来不同；用户不总能知道影响范围与下一步。 | 保留统一状态语义，但提供 inline/section/page 三种密度；不要把每个状态都做成大卡。 | 为四条路由列出 loading/empty/error/permission/stale/success 矩阵，核对影响说明与 retry。 | confirmed |
| CS-02 / P1 | `src/components/common/CustomSelect.tsx:128-165`；`LexicalPairSuggestionInput.tsx:132-166` | CustomSelect 在 `options=[]` 时打开空白菜单；词汇建议查询错误与“无建议”共用结果路径，没有 error/retry。 | 用户会把服务错误误认为没有数据，表单可能看似失效且无恢复入口。 | 增加明确的 empty/error/retry/disabled 文案和稳定高度；保持真实 Query 状态，不用 mock 填充。 | 以空数组、超时、403、500、慢请求逐项触发组件状态。 | confirmed |
| CS-03 / P2 | `src/components/layout/NotificationBell.tsx:265-305` | 通知面板只分 loading、empty、records，query error 会落入 empty；mark-all/read 失败也没有可见反馈。 | 通知服务故障被误报为“暂无通知”，教师/管理员可能漏掉待办。 | 用紧凑 inline error 区分空数据与失败，并保留重试；mutation 失败显示不阻断导航的提示。 | 断开通知接口和 WebSocket，分别验证初始加载、刷新和标记失败。 | confirmed |
| CS-04 / P1 | `src/features/onboarding/OnboardingTour.tsx:220-304`；学生/管理员 release 截图、mobile student 截图 | 首次引导使用全屏暗幕、超大 spotlight 阴影和发光边框；历史三角色截图中真实首页及 CTA 被遮挡，首屏视觉判断被引导层主导。 | 首次用户无法先建立页面心智模型，且“霓虹演示感”强化廉价印象；小屏可用空间进一步缩小。 | 改为可延后、可恢复的轻量导览；只在必要步骤聚焦，不以整屏强遮罩作为默认首屏。 | 新账号分别在桌面/375px 检查首屏、跳过、返回导览、目标不存在和 reduced-motion。 | confirmed |
| CS-05 / P2 | `src/pages/student/AssessmentAttempt.tsx:644-789,794-853`；`assessment-submit-confirm.png` | 核心流程已区分保存中/成功/失败、锁定、超时、全部作答和提交确认，是可复用的业务状态基线；但文案全部硬编码中文，玻璃确认框的次要说明在暗幕上对比偏弱。 | 状态完整性优于其他页面，但无法直接推广到英文界面；关键不可逆提示可能被视觉弱化。 | 后续只统一文案/表面/层级，不改保存队列、锁定和提交逻辑；不可逆说明应高于装饰。 | 中英切换、未答题、自动提交失败、保存冲突、键盘焦点与对比度检查。 | confirmed |

### 角色差异

| ID / 优先级 | 证据位置 | 观察到的事实 | 用户影响 | 建议方向 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| RD-01 / P1 | `src/components/layout/index.tsx:59-127,230-355,895-1006` | 角色切换主要改变导航项与学生 AI 入口；侧栏、Topbar、玻璃、圆角、标题、按钮节奏完全共享。 | 三个工作区只像同一模板换菜单，教师判断效率和管理员系统可信度没有独立气质。 | 共享品牌骨架与状态色，但按角色改变信息密度、默认组件和首要动作；不要创建三套无关皮肤。 | 同时打开三角色首页，隐藏菜单文字后仍能从密度和任务结构判断角色。 | confirmed |
| RD-02 / P1 | 学生 `Dashboard.tsx:387-427`；教师 `TeacherWorkspacePage.tsx:218-278`；管理员 `Dashboard.tsx:298-400` | 学生已把到期复习前置，教师也把 onboarding/todo 放在 quick actions 前；管理员却继续使用欢迎式大 Hero、3D StatCard 和发光 ChartCard。 | 学生/教师已有任务优先的业务线索，管理员仍像品牌展示页，运维扫描与审计判断效率受损。 | 学生强调节奏与下一步；教师强调队列、对比和批量处理；管理员强调密度、时间范围、异常和可追溯性，禁用 3D/磁吸/持续发光。 | 用“10 秒找到下一任务 / 异常 / 数据时间范围”三项角色测试。 | confirmed |

### 响应式

| ID / 优先级 | 证据位置 | 观察到的事实 | 用户影响 | 建议方向 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| RS-01 / P1 | `src/components/layout/index.tsx:1020-1026`；`src/pages/dashboard/index.tsx:344-372`；`mobile-student-dashboard.png` | 375px 下 main 约 343px，学生概览 `p-10` 后内容宽约 263px，但右侧 grid 固定 `min-w-[280px]`，存在确定的横向溢出条件；历史移动截图也标记了宽度问题。 | 首屏可能横向滚动/裁切，语言等级与反应时卡片挤压。 | 移动端降为 `p-5/6`、`min-w-0 w-full`，到合适断点再恢复双列和大内边距。 | 320/375/390/768px 检查 `scrollWidth===clientWidth`，同时使用最长英文标签。 | confirmed |
| RS-02 / P2 | `src/components/layout/index.tsx:932-1006` | Topbar 固定 `h-16`，页名无 truncate/line-clamp，右侧仍保留通知与主题按钮；长英文标题可能换行挤压。 | 小屏标题、图标和点击区可能重叠或垂直溢出。 | 移动端采用单行可截断 title + 独立 workspace label，次要控制收入口袋菜单。 | 320/375px 下遍历最长 route title 与 200% text zoom。 | proposed |
| RS-03 / P2 | `src/components/common/CustomSelect.tsx:40-51,128-165` | Portal 菜单固定出现在 trigger 下方，只水平跟随，不判断视口底部或键盘占用。 | 小屏底部表单可能出现菜单被裁切，触屏难以完成选择。 | 增加上下翻转/可用高度计算或采用成熟 listbox positioning。 | 375×667、软键盘打开、页面底部 trigger 三场景检查。 | proposed |
| RS-04 / P2 | `src/components/common/index.tsx:65-91,289-311`；`training-mobile-375x812.png`；`src/lib/i18n.ts:6-16` | 小标签/Badge 无长文案约束；历史训练移动截图证明真实法文例句会显著拉长内容，但当前没有英文长标签或法文 UI locale 的现成截图。 | 未来长词、法文重音句和英文 action 可能扩大卡片高度或挤压同行按钮。 | 按内容优先自然换行，操作区必要时纵向排列；不靠缩小字体或隐藏正文。 | 需后续用真实长英文 UI 文案 + 法文业务内容做视口快照。 | blocked |

### 性能风险

| ID / 优先级 | 证据位置 | 观察到的事实 | 用户影响 | 建议方向 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| PF-01 / P1 | `src/index.css:63-86,189-238,311-321` | `*` 为所有元素设置 0.3–0.5s 颜色/阴影过渡；fixed 纹理层使用 `mix-blend-mode` 与 z-index 9999；多处大面积 backdrop blur/阴影。 | 主题切换和复杂页面滚动增加重绘/合成成本，所有操作都带“慢半拍”的感受。 | 过渡只挂到明确交互组件；大面积表面减少 blur、blend 和多层阴影，正文/数据区使用稳定不透明表面。 | DevTools paint/composite 录制，比较主题切换、滚动和低端设备帧率。 | confirmed |
| PF-02 / P1 | `src/index.css:250-321,392-509`；`tailwind.config.js:72-103`；`OnboardingTour.tsx:169-171` | fluid、gradient button、text gradient、border beam 等无限动画并存；border beam 即使 opacity 0 仍运行。reduced-motion 仅关闭 stat value，smooth scroll 和其他动画无降级。 | 持续消耗资源，注意力被无语义运动分散，低动效用户仍被强迫观看。 | 建立全局 motion policy：默认静态；仅让状态变化动；所有无限动画默认移除或暂停，完整覆盖 `prefers-reduced-motion`。 | 开启 reduced-motion 后检查 computed animation/transition；Performance 面板记录空闲 10 秒。 | confirmed |
| PF-03 / P1 | `src/components/common/index.tsx:94-149,196-276`；学生/管理员 StatCard 网格 | 每个 StatCard 挂两组 spring、pointer move 计算、3D rotate、hover scale；每个 AnimatedNumber 再运行 1.5s RAF，且没有 RAF cleanup。 | 仪表盘多卡同时进入时产生无业务价值的主线程与合成负担，数字稳定读取被延迟。 | 管理员禁用 3D/磁吸；数字只在关键成果一次揭示，并尊重 reduced-motion、取消未完成 RAF。 | 4/8/12 卡场景比较 scripting、layers 和首次稳定时间。 | confirmed |
| PF-04 / P2 | `src/lib/echarts.ts:1-44`；`qa-output/release-readiness/report.md:131-135` | 当前 ECharts 使用 core + 按需注册，是正向基线；历史 2026-04-25 build 报告记录 vendor≈1MB、chart-engine≈412KB，但本轮禁止 build，无法确认当前体积。 | 若历史风险仍在，三角色图表页首载与低端设备解析成本偏高。 | 不替换 ECharts；先保留按需注册，后续基于当前 bundle 证据做路由级懒加载和依赖预算。 | 后续经授权运行 bundle analyzer，记录 gzip/brotli 和路由 chunk；当前不下结论。 | blocked |

## 4. 角色目标摘要

| 工作区 | 应保留的业务线索 | 当前视觉缺口 | 后续目标 |
| --- | --- | --- | --- |
| STUDENT_WORKSPACE | 到期复习、推荐训练、测评进度与保存状态已具备真实数据入口 | 首屏层级过多、卡片过密、CTA/引导发光抢占注意力 | 有生命力但稳定：下一步优先、进展清楚、错误可恢复 |
| TEACHING_WORKSPACE | onboarding/todo 已前置，班级、发布、干预和词表状态均使用真实查询 | 仍是欢迎 Hero + 卡片矩阵，判断/比较/批量处理不够突出 | 有判断效率：队列、异常、对比和批量动作优先 |
| ADMIN_CONSOLE | 用户、活跃、完成量与 AI 运维指标来源真实 | 与学生同用 3D StatCard、玻璃图表和欢迎大标题；全页中文硬编码 | 有系统可信度：中性高密度、时间范围、异常、审计线索优先 |

## 5. 明确排除项

- 不改业务逻辑、API 契约、权限、Zustand/React Query 状态和保存/提交队列。
- 不用 mock、占位成功态或静态假数据掩盖 loading/empty/error/permission。
- 不在 A1 执行整站换肤、引入 React Bits、替换 ECharts、拆路由或扩展 `fr-FR` UI 契约。
- 不把历史 QA 报告已复验关闭的乱码、路由和创建流程缺陷重新列为当前功能 bug；截图只用于视觉/布局证据。
- 不把所有内容继续卡片化，也不以缩小字体、隐藏文案解决响应式问题。
- 本轮不修源码：没有发现会阻止审计沉淀的极小代码问题；最终静态复核只看到 `docs/ui-quality/` 新文件。
