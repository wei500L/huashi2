# 前端动效预算与适配原则

EF.Transfer 是需要长时间使用的语言学习产品。动效用于确认因果、提示状态和建立空间层级，不用于持续吸引注意力。所有新动效先对照本规范；已有业务行为、API、权限和国际化不因动效改造而改变。

## 四档预算

| 场景 | 预算 | 适用 | 约束 |
| --- | --- | --- | --- |
| 操作反馈 | 0–160ms | hover、按下、焦点、按钮/字段反馈 | 只改变颜色、边框、阴影或透明度；不平移布局 |
| 状态变化 | 160–280ms | 选中、成功/失败、展开折叠、进度更新 | 一次完成；同一控件不要叠加多个 spring |
| 页面进入 | 280–480ms | 路由内容、抽屉、弹窗进入/退出 | 只在挂载或状态切换时出现；不要阻塞可操作内容 |
| 滚动揭示 | 480–720ms | 诊断页或长列表中首次进入视口的重点内容 | 每个区块最多一次，使用 opacity/transform；不用于普通表格行 |

CSS token 位于 `src/index.css` 的 `--motion-*` 变量中。新增 CSS 优先使用 `.motion-feedback`、`.motion-state`、`.motion-enter` 或 `.motion-reveal`，避免 `transition-all`。遗留的 `transition-all` 已被全局限制为绘制反馈属性；只有确实需要宽度/位置过渡的进度条和引导框才额外使用 `.motion-layout`。

## 只出现一次与禁止持续运行

- 一次性动效：页面首次进入、首次出现的空状态/成功状态、一次性的数字计数和滚动揭示。数据刷新不能重复播放整页入场。
- 不得持续运行：背景漂浮、渐变流动、边框光束、无限 pulse、自动旋转和持续 WebGL。加载中的 spinner 是唯一例外，且必须伴随可读的 loading 状态。
- 同一视口最多一个强调性动效；学习内容、表格和输入区域优先保持稳定，避免视线疲劳。

## reduced motion

- `MotionConfig reducedMotion="user"` 在入口统一让 Framer Motion 尊重 `prefers-reduced-motion`。
- CSS 媒体查询将 transition/animation 变为静态，并关闭 spotlight、漂浮、渐变流动等非必要效果；信息、焦点环、进度和成功/错误色彩仍然可见。
- 数字计数、滚动揭示、鼠标磁吸和 3D 倾斜在 reduced motion 下直接显示最终状态。不能只把时长改成极小值后继续循环。

## React Bits 适配策略

当前前端没有 React Bits、GSAP、OGL 或 Three.js 运行时依赖；以下规则作为后续引入组件的门槛。

1. 默认复用现有 Framer Motion 和 CSS token；不要为了一个按钮或卡片引入新的动画运行时。
2. GSAP、OGL、Three.js 仅允许在品牌页或诊断专属页按需动态加载，并在离开页面时销毁实例。每页最多一个持续 WebGL 场景。
3. React Bits 的鼠标交互必须同时提供键盘（focus/Enter/Space）、触屏（tap/swipe 或静态控件）和静态降级。鼠标光标、hover 光效不能承载唯一信息。
4. WebGL、canvas 和 shader 只负责装饰或诊断辅助，核心学习内容、分数、错误解释和操作入口必须是 DOM 文本与可访问控件。
5. 无法满足 reduced motion、键盘、触屏或静态降级的组件不进入产品主流程。

## 审查清单

- 动效属于哪一档预算，是否有明确的开始/结束原因？
- 是否会因数据刷新、重新渲染或列表滚动反复播放？
- 是否触碰 layout、输入值或弹窗定位的 transition？
- reduced motion 下是否变为静态且信息不丢失？
- 鼠标之外是否可用键盘、触屏和无动画版本？
- 是否引入了新的持续运行线程或第二个 WebGL 场景？

本轮未运行任何测试、lint、typecheck 或 build。
