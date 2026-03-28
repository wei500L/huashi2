# 前后端契约与 Mock 遗留排查报告

排查日期：2026-03-28

结论说明：
- 本报告以后端源码 `app-server/`、`ai-gateway/`、`shared-kernel/` 为真实契约来源。
- 下列问题均可直接从当前仓库代码证明；本次未出现必须标记为“需要后端确认”的问题。
- 严重程度优先级按“页面显示错误 / 状态判断错误 / 提交流程异常 / 会掩盖联调问题”排序。

## 严重程度：严重

### 1. 诊断结果页 `chartPayload` 契约已漂移，雷达图当前直接消费了后端不存在的字段

- 问题描述
  前端 `DiagnosisChartPayload` 相关类型仍按旧字段建模，`DiagnosisRadarMetric.max`、`DiagnosisRadarMetric.key`、`DiagnosisContextPerformance.contextSupportLevel`、`DiagnosisResponseTimelinePoint.hesitationTimeMs` 等字段与后端当前返回结构不一致。其中雷达图页面已经直接消费了 `metric.max`，会在运行时拿到 `undefined`。
- 涉及文件
  `src/lib/contracts.ts`
  `src/pages/diagnosis/index.tsx`
  `src/lib/format.ts`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisRadarMetric.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisContextPerformance.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisLexicalTypePerformance.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisResponseTimelinePoint.java`
- 具体代码位置
  `src/lib/contracts.ts:396-426`
  `src/pages/diagnosis/index.tsx:377-383`
  `src/lib/format.ts:168-176`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisRadarMetric.java:3-7`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisContextPerformance.java:3-7`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisLexicalTypePerformance.java:3-7`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/diagnosis/support/DiagnosisResponseTimelinePoint.java:3-10`
- 前端当前实现
  前端类型声明为：
  - `DiagnosisRadarMetric { key, label, value, max }`
  - `DiagnosisContextPerformance { contextSupportLevel, accuracy, avgReactionTimeMs, attemptCount }`
  - `DiagnosisLexicalTypePerformance { lexicalPairType, accuracy, avgReactionTimeMs, attemptCount }`
  - `DiagnosisResponseTimelinePoint { order, reactionTimeMs, hesitationTimeMs, correct }`

  诊断结果页又把 `metric.max` 直接传给 `buildRadarOption`，而 `buildRadarOption` 会把它作为 ECharts radar indicator 的 `max`。
- 后端预期或真实契约
  后端当前 record 为：
  - `DiagnosisRadarMetric(code, label, value)`
  - `DiagnosisContextPerformance(level, accuracy, avgReactionTime, totalCount)`
  - `DiagnosisLexicalTypePerformance(lexicalPairType, accuracy, avgReactionTime, totalCount)`
  - `DiagnosisResponseTimelinePoint(presentationOrder, itemResultId, taskType, lexicalPairType, reactionTime, correct, errorType)`

  后端没有返回 `max`，也没有返回 `hesitationTimeMs`。
- 风险影响
  会导致诊断结果页雷达图使用不存在的 `max` 字段，图表上限配置与真实契约脱节；同时 `chartPayload` 其余子结构的前端类型也已失真，后续一旦在页面或图表里继续消费 `contextPerformance`、`lexicalTypePerformance`、`responseTimeline`，会直接出现字段读取错误或静默展示错误。
- 修复建议
  1. 先把 `src/lib/contracts.ts` 中这四个类型改成与后端同名字段一致。
  2. 如果前端渲染确实需要雷达图 `max`，不要再假设后端返回，改为在前端按指标 code 本地派生，例如统一设为 `1` 或建立 `code -> max` 映射。
  3. 如页面希望继续保留现有字段名，应该在 service 层增加显式适配，而不是让页面直接消费错误类型。
- 是否属于 mock 遗留问题
  否

## 严重程度：高

### 2. 词表批量加词响应类型与后端不一致，提交成功提示会漏掉“跳过重复词对”信息

- 问题描述
  前端把 `AddLexicalListItemsResultVO` 定义成了 `lexicalListId + skippedCount + duplicatedPairIds`，但后端真实返回只有 `addedCount + skippedPairIds`。页面提交成功后读取 `result.skippedCount`，实际会得到 `undefined`，从而丢失后端返回的重复跳过信息。
- 涉及文件
  `src/lib/contracts.ts`
  `src/pages/teacher/LexicalLists.tsx`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/vo/AddLexicalListItemsResultVO.java`
- 具体代码位置
  `src/lib/contracts.ts:1112-1116`
  `src/pages/teacher/LexicalLists.tsx:100-115`
  `src/pages/teacher/LexicalLists.tsx:67-70`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/vo/AddLexicalListItemsResultVO.java:5-7`
- 前端当前实现
  前端类型声明：
  - `lexicalListId: number`
  - `addedCount: number`
  - `skippedCount: number`
  - `duplicatedPairIds: number[]`

  页面成功提示依赖 `result.skippedCount`。同时页面还本地基于 `existingPairIdSet` 预先拼装了一个 `skippedPairIds`，只用于提交前提示。
- 后端预期或真实契约
  后端真实返回：
  - `addedCount`
  - `skippedPairIds`

  没有 `lexicalListId`，也没有 `skippedCount` 或 `duplicatedPairIds`。
- 风险影响
  用户实际提交时如果有重复词对被后端跳过，页面成功提示会错误地显示为“只添加了 N 个词对”，而不会提示跳过数量。若前端本地预判与后端最终判定不一致，真实跳过结果也不会被展示出来。
- 修复建议
  1. 把前端类型改成 `addedCount: number; skippedPairIds: number[]`。
  2. 页面提示改为基于 `result.skippedPairIds.length` 计算跳过数量。
  3. 如果要展示重复词对详情，应优先使用后端 `skippedPairIds`，不要把本地预判结果当成真实结果。
- 是否属于 mock 遗留问题
  否

### 3. 管理员配置页的“兼容旧响应”兜底逻辑会掩盖真实联调问题，并可能把空白默认值回写到后端

- 问题描述
  `normalizeAdminAiConfigView` 和 `normalizeAiOpsConfigPayload` 会把缺失的配置块统一补成空字符串、`0`、`false`、`null`，并继续允许页面进入编辑和保存流程。当前后端源码已经保证 `AdminAiConfigViewVO` 一定返回 `config`、`runtime`、`stored`，因此前端这套兜底会把真实契约回归隐藏掉。
- 涉及文件
  `src/pages/admin/ConfigCenter.tsx`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/dto/AdminAiConfigViewVO.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/dto/AdminAiRuntimeStateVO.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/dto/AdminAiStoredStateVO.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/service/AiOpsAdminService.java`
- 具体代码位置
  `src/pages/admin/ConfigCenter.tsx:164-203`
  `src/pages/admin/ConfigCenter.tsx:212-245`
  `src/pages/admin/ConfigCenter.tsx:273-290`
  `src/pages/admin/ConfigCenter.tsx:436-437`
  `src/pages/admin/ConfigCenter.tsx:557-585`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/dto/AdminAiConfigViewVO.java:8-15`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/dto/AdminAiRuntimeStateVO.java:5-10`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/dto/AdminAiStoredStateVO.java:5-8`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/opsconfig/service/AiOpsAdminService.java:289-318`
- 前端当前实现
  前端注释明确写了“Older app-server responses may omit nested admin status blocks; normalize first so the page stays renderable.”  
  实际行为是：
  - 缺失 `config.provider` 时补空 provider 和空 providers map
  - 缺失数字字段时补 `0`
  - 缺失 `runtime/stored` 时补 `available: false`、`present: false`
  - `buildSavePayload` 直接把归一化后的 `config` 重新提交
  - `currentConfigVersion` 在版本缺失时返回 `null`
- 后端预期或真实契约
  当前后端 `AiOpsAdminService.toView(...)` 总是构造完整的 `AdminAiConfigViewVO`，并明确返回 `runtime` 与 `stored` 两个嵌套对象。也就是说，当前源码并不支持“把这些对象省略掉”这种响应形态。
- 风险影响
  1. 会把后端真实契约缺失、字段回归、网关响应异常伪装成“空配置但还能编辑”的正常页面。
  2. 管理员一旦保存，可能把前端补出来的空字符串和 `0` 默认值回写到后端。
  3. 如果版本号被补成 `null`，保存时会弱化并发保护，增加误覆盖风险。
- 修复建议
  1. 对 `config`、`runtime`、`stored` 改为严格校验；缺任何一个都应直接报“契约异常”，并阻止保存。
  2. 仅对真正允许可空的展示字段做窄范围 fallback，不要对整份配置对象做可保存的默认值补齐。
  3. 如果确实要兼容旧后端版本，应加显式版本判断，并在 UI 上标注“只读兼容模式”，禁止保存。
- 是否属于 mock 遗留问题
  否（属于联调兜底/兼容遗留）

## 严重程度：中

### 4. 班级完成率 `byMode` 类型定义落后于后端，后续一旦启用模式拆分展示会直接读错字段

- 问题描述
  前端 `ClassCompletionByModeVO` 仍假定后端返回 `label` 和 `completedCount`，而后端真实字段是 `completedStudentCount`，没有 `label`。当前页面尚未消费 `byMode`，所以问题暂时未显性暴露，但类型已经失真。
- 涉及文件
  `src/lib/contracts.ts`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/analytics/vo/ClassCompletionByModeVO.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/analytics/vo/ClassCompletionRateVO.java`
- 具体代码位置
  `src/lib/contracts.ts:822-833`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/analytics/vo/ClassCompletionByModeVO.java:3-7`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/analytics/vo/ClassCompletionRateVO.java:5-10`
- 前端当前实现
  前端定义为：
  - `mode`
  - `label`
  - `completionRate`
  - `completedCount`
  - `studentCount`

  但当前页面只使用了 `trend`，还没有消费 `byMode`。
- 后端预期或真实契约
  后端真实返回：
  - `mode`
  - `completionRate`
  - `completedStudentCount`
  - `studentCount`

  不返回 `label`。
- 风险影响
  现在属于潜伏问题；一旦前端开始展示按模式拆分的完成率卡片或表格，会直接得到 `undefined label` 和 `undefined completedCount`。
- 修复建议
  1. 把前端类型改为与后端一致。
  2. 如需展示友好文案，前端按 `mode` 本地映射 label，不要把 label 当成接口字段。
- 是否属于 mock 遗留问题
  否

## 严重程度：低

### 5. CSV 导入模板与失败项类型被前端放宽到了后端并不存在的字段，当前页面虽有 fallback，但会制造错误预期

- 问题描述
  前端 `CsvImportTemplateFieldVO` 和 `CsvImportFailureVO` 保留了后端当前并不返回的字段，例如 `key`、`label`、`lineNo`、`message`。当前页面通过 fallback 勉强可用，但类型层已经暗示了不存在的契约。
- 涉及文件
  `src/lib/contracts.ts`
  `src/pages/shared/LexicalPairsWorkspace.tsx`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/vo/CsvImportTemplateFieldVO.java`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/vo/CsvImportFailureVO.java`
- 具体代码位置
  `src/lib/contracts.ts:991-1018`
  `src/pages/shared/LexicalPairsWorkspace.tsx:974-984`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/vo/CsvImportTemplateFieldVO.java:3-7`
  `app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/vo/CsvImportFailureVO.java:3-7`
- 前端当前实现
  - `CsvImportTemplateFieldVO` 允许 `fieldName/key/label`
  - `CsvImportFailureVO` 允许 `rowNumber/lineNo/reason/message`
  - 页面通过 `field.fieldName || field.key`、`field.description || field.label` 做兜底渲染
- 后端预期或真实契约
  - 模板字段只返回 `fieldName/required/description/example`
  - 失败项只返回 `rowNumber/englishWord/frenchWord/reason`
- 风险影响
  当前页面不会立刻报错，但后续开发如果按前端类型直接使用 `key`、`label`、`lineNo`、`message`，会拿到空值并误判为“接口偶发没带字段”。
- 修复建议
  1. 删除前端类型里当前后端不存在的字段。
  2. 如页面确实需要 `label` 或 `key`，应在前端基于 `fieldName` 显式派生，而不是放进接口类型里冒充后端字段。
- 是否属于 mock 遗留问题
  否

## Mock / 写死数据 / 兜底逻辑结论

### 未发现仍在生产请求链路中生效的 mock 开关或拦截器

- 排查结论
  未发现 `msw`、`mockjs`、`axios-mock-adapter`、`vite-plugin-mock`、`VITE_*MOCK`、请求级 mock interceptor 等仍在生产代码路径中生效。
- 证据
  `src/lib/api.ts:24-54` 只基于 `VITE_API_URL` 和真实 axios/fetch 发请求，没有 mock 分支。
  `src/lib/compat-axios.ts:1-10` 只是对真实 API 方法的兼容封装，不是 mock 层。
  全仓可见的 `vi.mock(...)` 仅出现在测试文件，例如 `src/App.test.tsx:10-22`、`src/pages/shared/LexicalImportCenter.test.tsx:15-18`。
- 需要注意的非 mock 行为
  `src/pages/admin/ConfigCenter.tsx:212-245` 的兜底归一化会掩盖真实契约异常。
  `src/pages/teacher/LexicalLists.tsx:67-70` 存在本地拼装 `skippedPairIds` 的 UI 预判，但它只用于提示，不是请求层 mock。

### 未发现真实“假成功 / 假失败”逻辑

- 排查结论
  未发现生产代码中通过本地 `Promise.resolve`、写死成功响应、写死失败响应替代真实接口结果的实现。
- 备注
  本次发现的问题主要集中在“契约漂移”和“兜底吞错”，不是 mock server 遗留。
