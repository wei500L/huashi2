# UI Quality Handoff

更新时间：2026-08-05  
当前阶段：A2 品牌方向决策完成，下一步是低保真视觉验证。

## 新窗口最短接手路径

1. 读 [README](./README.md)。
2. 读 [A1 完整证据地图](./01-global-visual-evidence-audit.md)，优先看 BR-03、IH-01/02、CS-04、RD-01/02、RS-01、PF-01/02/03。
3. 读 [A2 品牌方向决策](./02-brand-direction-decision.md)，先区分已确认、候选和明确排除。
4. 执行 `git status --short`，保留新窗口开始时的任何未提交修改；视觉原型阶段仍不改业务代码。
5. 进入低保真验证，只验证关键路径和候选组件边界，不做整站换肤。

## 本轮结论

- 品牌方向：确认 **Lexical Cartography / 语言迁移地图**。EF.Transfer 是连接英语起点、迁移证据和法语目标的地图；AI 是测绘工具，不是主角。
- 品牌锚点：EN↔FR 迁移焦点 + 单条可读路线 + 证据图钉。颜色以墨色/纸张为基底，来源暖矿物色、目标冷水域色、进展亮色路线；紫色降为辅助。
- React Bits：Topography 是最接近锚点的底图候选；TrueFocus、VariableProximity 是局部焦点/探索候选；WebThreads 仅限教师关系视图；StrokeText 与 TextLoop 明确排除为全局品牌锚点。
- 三角色：学生是路线行者，教师是地图编辑者，管理员是测绘控制台；共享地图语法但分别强调下一步、队列/对比、密度/审计。

- P0：`/admin/dashboard` 与 `/assessments/attempts/:attemptId` 大量硬编码中文，`/dashboard` 也有局部中文；英文 shell 下必然混排。当前 UI locale 仅中/英，法文按业务内容和排版韧性处理。
- P1 主线：同一套紫色、玻璃、3rem 圆角、渐变 CTA、发光边框、磁吸/3D 被用于所有层级和角色，形成通用 AI 皮肤。
- 信息层级：Topbar title + PageHeader + Hero 重复，卡片套卡片把任务、数据和状态拉成同一权重。
- 角色差异：业务路由与数据不同，但视觉语法基本相同；管理员尤其不应继续使用 3D StatCard 和持续发光图表。
- 状态：AssessmentAttempt 的保存/锁定/超时/提交确认值得保留；ChartCard/FeedbackState 是统一状态的基础，但页面、通知、Select/建议输入仍有缺口。
- 响应式：学生概览在 375px 存在 `p-10` 后容器小于 `min-w-[280px]` 的确定溢出条件。
- 性能：全局 `*` 慢过渡、fixed blend 纹理、blur/阴影、无限动画和每卡 spring/RAF 同时存在；reduced-motion 覆盖不足。

## QA 证据说明

- 历史截图已由 commit `62a4e32` 从工作树删除，本轮从 `9e4c372` 只读提取查看，没有恢复到仓库。
- 关键路径：三角色 `qa-output/release-readiness/screenshots/prod-rerun-*.png`；核心流程 `dogfood-output/answering-flow/screenshots/assessment-*.png`；移动补充 `mobile-student-dashboard.png`、`training-mobile-375x812.png`。
- 旧 QA 报告中的功能缺陷多已注明复验关闭；不要把截图中的历史状态直接当成当前功能回归。

## 未验证风险

- 未启动当前页面，无法确认当前字体实际渲染、精确对比度、focus、软键盘和横向 scrollWidth。
- 没有当前英文长文案/法文业务内容截图；RS-04 保持 blocked。
- 未生成当前 bundle；历史 vendor/chart-engine 体积只作为待复核风险，PF-04 保持 blocked。
- 未检查所有页面，结论是全局样式 + 四条代表路径的证据基线，不是最终验收。

## 下一步

执行低保真视觉验证：用现有 CSS/DOM 对三角色首页与学生诊断路径做静态对照，验证 5 秒品牌辨识、首屏任务可见性、真实长英法文案、reduced-motion 与 375px 无横向溢出；再决定候选组件是否进入实现。

本轮未运行任何测试、lint、typecheck 或 build。
