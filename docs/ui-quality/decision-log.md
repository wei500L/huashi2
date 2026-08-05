# UI Quality Decision Log

本文件只记录会约束后续实现的重要设计取舍，按日期追加，不回写或覆盖历史记录。

当前状态：2026-08-05 的 A3 已在 A2 品牌方向上锁定 React Bits 白名单、黑名单与依赖预算，但未锁定具体像素、色值、字体文件或页面结构；白名单仍需低保真验证。

## 2026-08-05 — A2 品牌叙事与视觉主张

- **选择 Lexical Cartography**：A1 的 BR-01/02 证明全域紫色、玻璃、发光和 Sparkles 标记把产品推向通用 AI 模板；而真实业务链路天然包含词对、诊断证据、训练路线和进度。地图隐喻能用一个可识别锚点串起这些对象，并保留三角色的业务差异。
- **选择语义色而非换一个主色**：墨色/纸张基底、来源暖矿物色、目标冷水域色、迁移亮色路线能表达起点/落点/进展；紫色退为辅助，避免复制 React Bits 默认紫色或国旗式配色。
- **选择“默认静止、状态才动”**：A1 的 PF-01/02/03 已确认全局慢过渡、无限动画、3D/磁吸和每卡 RAF 同时存在；品牌动效必须改为一次性焦点/路线反馈，并覆盖 reduced-motion。
- **React Bits 取舍**：Topography 最适合成为低对比地图底图候选；TrueFocus、VariableProximity 只适合局部焦点/探索；WebThreads 仅保留给教师关系视图。StrokeText 因双语可读性和装饰化语气排除，TextLoop 因循环口号会稀释任务与单一锚点排除为全局动效。
- **保持实现边界**：本轮只写决策文档，不引入 React Bits、不改业务/API/权限/i18n 契约、不做整站换肤；候选组件须在低保真验证、长英法文案、键盘/静态降级和性能门槛通过后再实现。

## 2026-08-05 — A3 React Bits 白名单与依赖预算

- **只采用能表达业务语义的组件隐喻**：Stepper 直接对应测评/诊断阶段，列为 adopt；Topography、TrueFocus、CountUp、AnimatedList、Radar、WebThreads 只有在删去默认装饰、改用现有 Framer Motion/CSS/DOM 并保留静态等价物后列为 adapt。
- **把不确定方案隔离在低频原型**：MaskedHeading、Stack、Scanner 只列为 evaluate，且只能用于 `/diagnosis`、`/training` 或 `/teacher/research` 的单一低频区域；不得先进入三角色高频工作台。
- **高频工作台建立硬黑名单**：StrokeText、TextLoop、CardSwap、PillNav、SpotlightCard、Masonry、ElasticSlider、MorphSlider 列为 reject。`/teacher/workspace` 与 `/admin/dashboard` 禁止 cursor-only、全屏 WebGL、玻璃拟态、默认霓虹、自动轮播和指针驱动 3D/磁吸。
- **运行时依赖预算归零**：本轮及白名单首轮实现不安装 React Bits 整库，不新增 GSAP、OGL、Three.js 或第二套动画库；复用现有 `framer-motion`，WebGL/Canvas 上下文预算为 0。未来例外必须有 bundle 证据、路由级懒加载和明确的静态降级。
- **保留现有图表与业务状态**：Radar 不替换现有按需注册的 ECharts；Stepper 不改测评保存、锁定、超时和提交队列；AnimatedList 不重排业务优先级。所有视觉变体只能读取现有状态，不能制造假进度或占位成功态。
