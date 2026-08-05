# UI Quality Handoff

更新时间：2026-08-05  
当前阶段：A1 证据审计完成，下一步是 A2 品牌方向决策。

## 新窗口最短接手路径

1. 读 [README](./README.md)。
2. 读 [A1 完整证据地图](./01-global-visual-evidence-audit.md)，优先看 BR-03、IH-01/02、CS-04、RD-01/02、RS-01、PF-01/02/03。
3. 执行 `git status --short`，保留新窗口开始时的任何未提交修改；本轮只新增 `docs/ui-quality/`。
4. 进入 A2，只产出 `02-brand-direction-decision.md` 及同步沉淀，不做整站换肤。

## 本轮结论

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

执行 A2“比赛级品牌叙事与视觉主张”：优先评估 Lexical Cartography / 语言迁移地图，以一个品牌锚点替代全域紫色装饰；定义共享品牌骨架和学生/教师/管理员三种信息密度。把候选、确认和排除分开，不锁死像素与色值，不改业务代码。

本轮未运行任何测试、lint、typecheck 或 build。
