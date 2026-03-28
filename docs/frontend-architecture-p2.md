# Frontend Architecture P2

## 本轮改造目标
- 让路由页只承担装配职责。
- 把会重复演化的业务状态和 UI 片段从超大页面里抽走。
- 先处理最容易继续膨胀的教师首页、诊断页、训练页。

## 新增模块
- `src/features/teacher-workspace/`
  - `TeacherWorkspacePage.tsx`
  - `components.tsx`
  - `copy.ts`
  - `queryKeys.ts`
- `src/features/session-runtime/`
  - `useSessionRuntime.ts`
  - `components.tsx`
  - `helpers.ts`

## 模块边界
- 路由页：
  - 只负责挂载 feature 页面。
- feature 页面：
  - 组合 query、派生文案、区块组件。
- session runtime：
  - 负责运行中页面的保存、恢复、冲突完成判断、统一提示与按钮。
- `src/lib/contracts/teacherWorkspace.ts`
  - 新增领域类型文件，并从 `src/lib/contracts.ts` 统一 re-export。
- `src/lib/services/teacherWorkspace.ts`
  - 新增领域服务文件，并从 `src/lib/services.ts` 统一 re-export。

## 首批结构治理结果
- 新教师首页不再堆在 `src/pages/teacher` 的列表页里。
- 诊断页和训练页不再各自维护一套保存进度、自动 keepalive、重试冲突和提示条逻辑。
- 共享的保存动作、反馈提示、进度头部已抽成统一组件。

## 测试策略
- Vitest 继续承担纯函数和组件级验证。
- 当前新增测试：
  - `src/features/session-runtime/helpers.test.ts`
  - `src/features/teacher-workspace/copy.test.ts`
- 下一步适合继续补的测试：
  - 教师首页 overview 派生逻辑。
  - 路由标题和默认首页映射。
  - 诊断/训练运行态 hook 的冲突完成分支。

## 迁移顺序
1. 先抽共享运行时和新首页。
2. 再拆 `TemplateDraftEditor`、`LexicalPairsWorkspace`、`ConfigCenter` 的 feature 边界。
3. 最后再考虑更细的 contracts/services 按领域完全拆分。
