# 语义词库生产级 Prompt Pack V2

> 用途：给 AI 对话（Codex / ChatGPT / Claude）继续生成生产级英法形似词导入数据时直接复用的 Prompt。
> 不是项目内置 Prompt，不是一次性样例。

## 快速上手

1. 复制任意一条 Prompt 到你的 AI 对话
2. 替换所有 `{{...}}` 占位符
3. 确保模型能访问项目文件（上传或工作区挂载）
4. 推荐执行顺序：生成(P1) → 审校(P3) → 修复(P4) → 高风险扩容(P2) → 滚动扩容(P5)

## 占位符速查

| 占位符 | 含义 | 示例值 |
|---|---|---|
| `{{PROJECT_ROOT}}` | 项目根路径 | `/mnt/d/huashi2` 或 `D:/huashi2` |
| `{{TARGET_FILE}}` | 输出 CSV 路径 | `{{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-pack-3.csv` |
| `{{ROW_COUNT}}` | 目标生成条数 | `80`（建议 60-120，不超过 150） |
| `{{SOURCE_CSV}}` | 待修复的 CSV | 同 TARGET_FILE 的某个已有文件 |
| `{{REPORT_FILE}}` | 审校报告输出路径 | `{{PROJECT_ROOT}}/docs/review-report-pack-3.md` |
| `{{FIXED_CSV}}` | 修复后的 CSV | `{{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-pack-3-fixed.csv` |

## 当前库存（使用前务必更新）

截至 2026-03-24，5 个 CSV 文件共 **80 个去重词对**。文件之间高度重叠：

| 文件 | 行数（含 header） | 说明 |
|---|---|---|
| `import-ready.csv` | 16 | 初始小批次 |
| `import-ready-full.csv` | 41 | 初始完整批次 |
| `import-ready-pack-2.csv` | 41 | 第二批 |
| `import-ready-high-risk-false-friends.csv` | 24 | 高风险假朋友子集 |
| `import-ready-80.csv` | 81 | 80 条合并去重版（最全） |

> 去重基准文件：`import-ready-80.csv` 包含所有已有词对。新批次只需与此文件去重即可。

## 固定上下文（所有 Prompt 共享）

无论使用哪条 Prompt，模型必须先读取以下文件：

**约束文件（必读）：**
- `{{PROJECT_ROOT}}/docs/semantic-lexicon-spec-v2.md` — 词库规范
- `{{PROJECT_ROOT}}/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java` — 后端导入模板约束

**去重基准（必读）：**
- `{{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-80.csv` — 当前最全的 80 条词对

**补充参考（可选，按需读取）：**
- `{{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-pack-2.csv` — 参考 pack-2 的风格和质量

## 硬约束（13 条）

1. **21 列固定顺序**，每个 CSV 必须含 header 行，编码 UTF-8 无 BOM：
   ```
   english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support
   ```
2. `lexical_pair_type` 枚举：`cognate` | `false_friend` | `partial_cognate` | `orthographic_similar`
3. `default_context_support` / `example_context_support` 枚举：`low` | `medium` | `high`
4. `difficulty_level`：整数 `1`-`5`
5. `semantic_overlap_score`：小数 `0.00`-`1.00`
6. `false_friend_risk`：小数 `0.00`-`1.00`
7. `source` 固定：`llm_v2_human_reviewed`
8. `active` 固定：`true`
9. `knowledge_status` 固定：`ready`
10. `embedding_status` 固定：`pending`
11. 每行只含：一个词对 + 一个主义项 + 一组主例句（批量导入层限制）
12. 有例句（`example_*`）时，必须同时有 `sense_*_definition`
13. 字段内禁用英文逗号——改写句子规避，不要依赖 CSV 引号转义

## 金标准示例行

```csv
fabric,fabrique,布料；工厂,false_friend,0.07,0.94,high,4,英语布料义常误导法语工厂义,llm_v2_human_reviewed,true,false-friend|high-risk|industry,ready,pending,cloth material,factory,布料；工厂,This fabric feels soft.,Cette fabrique produit du verre.,这种布料很柔软；这家工厂生产玻璃。,high
```

## 机器校验清单

每次生成/修复后，必须逐行检查：

| # | 检查项 | 判定 |
|---|---|---|
| V1 | 每行恰好 21 个字段（以逗号分割） | 不通过 = schema_error |
| V2 | `lexical_pair_type` 在 4 个枚举值内 | 不通过 = enum_error |
| V3 | `default_context_support` 和 `example_context_support` 在 3 个枚举值内 | 不通过 = enum_error |
| V4 | `difficulty_level` 为 1-5 整数 | 不通过 = enum_error |
| V5 | `semantic_overlap_score` 为 0.00-1.00 小数 | 不通过 = schema_error |
| V6 | `false_friend_risk` 为 0.00-1.00 小数 | 不通过 = schema_error |
| V7 | `source` = `llm_v2_human_reviewed` | 不通过 = enum_error |
| V8 | `active` = `true`，`knowledge_status` = `ready`，`embedding_status` = `pending` | 不通过 = enum_error |
| V9 | 有 `example_english` 时必须有 `sense_english_definition`（三语同理） | 不通过 = schema_error |
| V10 | `english_word + french_word` 不与基准 CSV 重复 | 不通过 = duplicate |
| V11 | 字段内无裸英文逗号 | 不通过 = schema_error |

---

## Prompt 1：生产级新批次生成

**场景**：生成 `pack-3.csv`、`pack-4.csv` 等新批次。建议每次 60-120 条。

```text
你的角色是 EF Transfer Platform 英法形似词词库的生产工程师。你不在做研究、不在写草稿——你在生产可直接导入系统的 CSV 数据。

## 步骤

1. 读取约束文件：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-spec-v2.md
   - {{PROJECT_ROOT}}/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java

2. 读取去重基准：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-80.csv

3. 提取所有已存在的 english_word + french_word 组合，建立去重集合。

4. 生成新 CSV：
   - 目标文件：{{TARGET_FILE}}
   - 目标条数：{{ROW_COUNT}}
   - 必须包含 header 行
   - 编码 UTF-8 无 BOM

5. 对生成结果执行机器校验（见下方清单），不合格行必须修正后再交付。

## 硬约束

21 列固定顺序：
english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support

字段约束：
- lexical_pair_type：cognate / false_friend / partial_cognate / orthographic_similar
- default_context_support / example_context_support：low / medium / high
- difficulty_level：1-5 整数
- semantic_overlap_score / false_friend_risk：0.00-1.00 小数
- source 固定 llm_v2_human_reviewed
- active 固定 true
- knowledge_status 固定 ready
- embedding_status 固定 pending
- 有例句时必须有 sense 定义
- 字段内禁用英文逗号

## 类型配比

- false_friend：40%-50%
- partial_cognate：20%-30%
- cognate：20%-25%
- orthographic_similar：10%-15%

## 难度配比

- level 1：10%-15%
- level 2：15%-20%
- level 3：25%-30%
- level 4：25%-30%
- level 5：10%-15%

## 类型判定标准

- false_friend：核心义项明显错位，false_friend_risk 通常 >= 0.80，semantic_overlap_score 通常 <= 0.25
- cognate：语义高度重合，semantic_overlap_score 通常 >= 0.85，false_friend_risk 通常 <= 0.20
- partial_cognate：共享义项真实存在但至少一个高频义项发生关键分化，两个分数通常在 0.30-0.70 区间
- orthographic_similar：拼写相近为主要关系，不应伪装成稳定同源词

## 质量红线

- 宁缺毋滥——对词对是否成立没有把握就放弃
- 中文释义必须是教学型释义（教师和学生一眼看懂）
- 英法例句必须自然且明确支撑义项差异/重合
- 中文译文必须覆盖两边例句的核心教学点
- 优先课堂、学校、家庭、社会、媒体、工作、日常生活等高频语境
- tags 使用 2-4 个标签（`|` 分隔），优先教学用途标签

## 禁止

- 重复词对
- 专名、缩写、冷僻术语、极罕见词
- 拼写相似但教学无价值的词对
- 需要多义项才能成立的复杂条目

## 机器校验清单

生成完毕后逐行检查：
- [ ] 每行 21 个字段
- [ ] 枚举值合法
- [ ] 分数范围 0.00-1.00
- [ ] difficulty_level 为 1-5 整数
- [ ] 有例句时有 sense 定义
- [ ] 无裸英文逗号
- [ ] 无与基准 CSV 重复的词对

## 交付

1. 直接写入 {{TARGET_FILE}}
2. 输出极简摘要（不要输出过程、解释、Markdown 正文）：
   - 新增条数
   - 四类词对数量
   - 五级难度分布
   - 校验是否全部通过
3. 不要提问，不要停在半成品。
```

---

## Prompt 2：高风险假朋友扩容

**场景**：优先扩容最适合诊断和干预的高风险假朋友词对。

```text
你的角色是 EF Transfer Platform 高风险假朋友词库的专项生产工程师。

## 步骤

1. 读取约束文件：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-spec-v2.md
   - {{PROJECT_ROOT}}/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java

2. 读取去重基准（必须读取全部已有数据）：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-80.csv
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-high-risk-false-friends.csv

3. 提取所有已存在的 english_word + french_word 组合，建立去重集合。

4. 生成 {{ROW_COUNT}} 条全新数据到 {{TARGET_FILE}}。

## 硬约束

（同 Prompt 1 的 21 列结构和字段约束）

额外约束——本批次全部为 false_friend：
- false_friend_risk >= 0.88
- semantic_overlap_score <= 0.25
- difficulty_level 优先 4 或 5（至少 70%）
- default_context_support 优先 high 或 medium
- 词对必须能明显诱发英语背景学习者的表层误判
- 优先教育、媒体、工作、日常生活、课堂高频语境

## 质量红线

（同 Prompt 1）

## 机器校验清单

（同 Prompt 1）

## 交付

1. 直接写入 {{TARGET_FILE}}
2. 极简统计：条数 + 难度分布 + 校验结果
3. 不要输出额外解释。
```

---

## Prompt 3：生产审校

**场景**：对已有 CSV 做严格质量审查，只挑错不修复。

```text
你的角色是英法形似词教学词库的生产审校员。你不负责新增数据，只负责严格挑错。

## 步骤

1. 读取约束文件：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-spec-v2.md
   - {{PROJECT_ROOT}}/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java

2. 读取去重基准（用于检查跨文件重复）：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-80.csv

3. 读取待审文件：
   - {{TARGET_FILE}}

4. 逐行审查并生成报告到 {{REPORT_FILE}}。

## 审查维度（缺一不可）

1. 21 列结构完整性
2. 枚举值合法性
3. 分数（0.00-1.00）和难度（1-5）范围
4. 分数与 lexical_pair_type 的一致性（如 false_friend 的 risk 不应过低）
5. 中文释义是否教学化且清晰
6. sense 定义与 example 例句的匹配度
7. 英法例句的自然度
8. 教学价值——是否存在低价值或边界模糊词对
9. 跨文件重复或近重复检测
10. 字段内是否有裸英文逗号

## 报告格式

文件：{{REPORT_FILE}}，固定 Markdown 表格：

| row_no | pair | severity | problem_type | why_problematic | revision_direction |
|---|---|---|---|---|---|

severity 只允许：critical / major / minor

problem_type 只允许：
- schema_error — 列数/格式错误
- enum_error — 枚举值非法
- score_inconsistency — 分数与类型不自洽
- weak_pedagogical_value — 教学价值低
- unnatural_example — 例句不自然
- weak_gloss — 中文释义不清晰
- pair_type_mismatch — 类型标注与实际义项差异不符
- duplicate_or_near_duplicate — 与已有数据重复或近重复
- low_confidence_entry — 词对关系不可靠

## 规则

1. 只输出问题行，没问题的行不写进报告
2. 措辞尖锐直接，不要客气
3. 不要帮修，只诊断
4. 报告末尾附总评：
   - 是否达到生产可导入标准
   - 是否达到生产可教学标准
   - 是否建议直接入库
```

---

## Prompt 4：基于审校报告自动修复

**场景**：拿到审校报告后，修复 CSV 使其达到入库标准。

```text
你的任务是修复已有导入 CSV，不是生成新词。

## 步骤

1. 读取约束文件：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-spec-v2.md
   - {{PROJECT_ROOT}}/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java

2. 读取待修复文件和审校报告：
   - {{SOURCE_CSV}}
   - {{REPORT_FILE}}

3. 逐条处理审校报告中的问题：
   - critical 和 major 必须修复
   - 无法在高置信度下修复的行直接删除，不硬改
   - minor 尽量修，不确定可跳过

4. 输出修复后文件到 {{FIXED_CSV}}

5. 对修复后文件执行完整机器校验。

## 机器校验清单

- [ ] 每行 21 个字段
- [ ] 枚举值合法
- [ ] 分数范围 0.00-1.00
- [ ] difficulty_level 为 1-5 整数
- [ ] 有例句时有 sense 定义
- [ ] 无裸英文逗号
- [ ] header 行完整

## 禁止

- 不要新增报告未涉及的新词
- 不要改变字段顺序
- 不要输出长篇解释

## 交付

修复摘要（极简）：
- 修复条数
- 删除条数
- 剩余条数
- 校验是否通过
```

---

## Prompt 5：滚动扩容

**场景**：已有若干批 CSV，继续做 pack-3、pack-4、pack-5，追求互补覆盖。

```text
你的角色是 EF Transfer Platform 词库的滚动扩容工程师。目标是做增量互补，不是从零重建。

## 步骤

1. 读取约束文件：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-spec-v2.md
   - {{PROJECT_ROOT}}/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java

2. 读取全量去重基准：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-80.csv

3. 如果有新近生产的 pack 文件也一并读取（用于去重和互补分析）：
   - {{PROJECT_ROOT}}/docs/semantic-lexicon-v2-import-ready-pack-2.csv
   （有更多 pack 文件时在此追加）

4. 分析现有词对的覆盖分布：类型、难度、领域标签。

5. 生成 {{ROW_COUNT}} 条全新词对到 {{TARGET_FILE}}，优先补足覆盖薄弱区域。

## 硬约束

21 列固定顺序：
english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support

字段约束：
- lexical_pair_type：cognate / false_friend / partial_cognate / orthographic_similar
- default_context_support / example_context_support：low / medium / high
- difficulty_level：1-5 整数
- semantic_overlap_score / false_friend_risk：0.00-1.00 小数
- source 固定 llm_v2_human_reviewed
- active 固定 true
- knowledge_status 固定 ready
- embedding_status 固定 pending
- 有例句时必须有 sense 定义
- 字段内禁用英文逗号

## 互补覆盖要求

新批次必须优先提高以下领域的覆盖度（按优先级排列）：
- 教育、课堂
- 日常生活、家庭
- 工作、行政
- 媒体、文化
- 法律、医疗
- 科学、学术

新批次必须与现有批次形成内容互补——不是换一批相似结构的词。
优先补足"高教学价值但尚未覆盖"的高频词。

## 质量红线

- 宁缺毋滥——对词对是否成立没有把握就放弃
- 中文释义必须是教学型释义（教师和学生一眼看懂）
- 英法例句必须自然且明确支撑义项差异/重合
- 中文译文必须覆盖两边例句的核心教学点
- 优先课堂、学校、家庭、社会、媒体、工作、日常生活等高频语境
- tags 使用 2-4 个标签（`|` 分隔），优先教学用途标签

## 机器校验清单

生成完毕后逐行检查：
- [ ] 每行 21 个字段
- [ ] 枚举值合法
- [ ] 分数范围 0.00-1.00
- [ ] difficulty_level 为 1-5 整数
- [ ] 有例句时有 sense 定义
- [ ] 无裸英文逗号
- [ ] 无与基准 CSV 重复的词对

## 交付

1. 直接写入 {{TARGET_FILE}}
2. 极简摘要：
   - 新增条数
   - 类型分布
   - 难度分布
   - 新覆盖的领域标签
   - 校验结果
```

---

## 推荐执行顺序

```
P1 生成 pack-3 (80条) → P3 审校 pack-3 → P4 修复 pack-3
                                                ↓
                              P2 高风险假朋友扩容 (30条)
                                                ↓
                              P5 滚动扩容 pack-4 (80条)
                                                ↓
                              P3 审校 pack-4 → P4 修复 pack-4
```

## 最快开始

如果只用一条 Prompt，用 **Prompt 1**。它是最完整的单次生产 Prompt。

替换占位符示例：
- `{{PROJECT_ROOT}}` → `/mnt/d/huashi2`
- `{{TARGET_FILE}}` → `/mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-pack-3.csv`
- `{{ROW_COUNT}}` → `80`
