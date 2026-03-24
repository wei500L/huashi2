# 语义词库生产级 Prompt Pack

用途：这不是项目内置 Prompt，而是给当前这段 Codex / ChatGPT 对话继续生成生产级导入数据时直接复用的 Prompt。

目标：持续生成可导入、可审校、可扩容的英法形似词数据，而不是一次性生成一小批样例。

## 为什么不要只用单条 Prompt

当前数据生产要同时满足四件事：

1. 符合项目真实导入字段和后端约束。
2. 避免和已有 CSV 重复。
3. 保证教学价值，而不是只靠拼写相似机械凑词。
4. 生成后能继续做机器校验和人工复核。

结论：生产级生成最好拆成 4 段：

1. 生成新批次
2. 审校新批次
3. 修复新批次
4. 继续扩容下一批

下面给出的 Prompt 都是“当前工作区可直接执行”的版本。

## 固定上下文

无论使用哪条 Prompt，都默认模型必须先读取以下文件：

- `/mnt/d/huashi2/docs/semantic-lexicon-spec-v2.md`
- `/mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java`
- `/mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready.csv`
- `/mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-full.csv`
- `/mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-pack-2.csv`
- `/mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-high-risk-false-friends.csv`
- `/mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-80.csv`

生成时必须默认遵守这些硬约束：

1. 输出字段顺序必须严格是这 21 列：
   `english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support`
2. `lexical_pair_type` 只能取：
   `cognate / false_friend / partial_cognate / orthographic_similar`
3. `default_context_support` 和 `example_context_support` 只能取：
   `low / medium / high`
4. `difficulty_level` 只能是 `1-5` 整数。
5. `source` 固定写 `llm_v2_human_reviewed`。
6. `active` 固定写 `true`。
7. `knowledge_status` 固定写 `ready`。
8. `embedding_status` 固定写 `pending`。
9. 只允许批量导入层支持的结构：
   一个词对 + 一个主义项 + 一组主例句。
10. 如果有例句，必须同时有 `sense_*_definition`。
11. 不允许与现有 CSV 中任何 `english_word + french_word` 组合重复。
12. 优先保留高教学价值、高迁移价值、高可诊断价值条目，丢弃边界模糊或过于冷门的词。
13. 除非你会正确处理 CSV 引号，否则不要在字段里使用英文逗号，优先改写句子规避逗号。

## Prompt 1：生产级新批次生成

适用场景：生成新的导入批次，例如 `pack-3.csv`、`pack-4.csv`。

建议：每次 60-120 条，不要一次塞 300 条。

```text
你现在不是在做项目设计，也不是在写研究草稿，而是在当前工作区中继续生产 EF Transfer Platform 的英法形似词导入数据。

你的任务是：
1. 先读取以下文件并吸收真实约束：
   - /mnt/d/huashi2/docs/semantic-lexicon-spec-v2.md
   - /mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready.csv
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-full.csv
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-pack-2.csv
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-high-risk-false-friends.csv
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-80.csv
2. 在确认现有词对后，生成一个新的 CSV 文件：
   - 目标文件：{{TARGET_FILE}}
   - 目标数据条数：{{ROW_COUNT}}
3. 生成后，立即自行做机器校验：
   - 每行必须 21 列
   - 枚举值合法
   - 分数范围合法
   - 不得与已有 CSV 重复
   - 有例句时必须有 sense 定义
4. 如果发现任何不合格行，必须先修正，再交付最终文件。

你必须严格遵守这些生产原则：

一、质量优先
- 宁可少生成，也不要为了凑数加入边界模糊、教学价值低、频率极低的词对。
- 如果你对某条词对是否成立没有把握，就直接放弃，不要保留。
- 中文释义必须是教师和学生一眼就能看懂的教学型释义，不要写语言学论文式描述。

二、配比要求
- `false_friend`：40%-50%
- `partial_cognate`：20%-30%
- `cognate`：20%-25%
- `orthographic_similar`：10%-15%

三、难度要求
- `difficulty_level = 1`：10%-15%
- `difficulty_level = 2`：15%-20%
- `difficulty_level = 3`：25%-30%
- `difficulty_level = 4`：25%-30%
- `difficulty_level = 5`：10%-15%

四、生产级判定标准
- `false_friend`：核心义项明显错位，通常 `false_friend_risk` 不应低于 0.80。
- `cognate`：语义高度重合，通常 `semantic_overlap_score` 应在 0.85 以上，风险通常不高。
- `partial_cognate`：共享义项真实存在，但至少有一个高频义项发生关键分化。
- `orthographic_similar`：重点来自拼写相近，不应伪装成稳定同源词。

五、例句要求
- 英文例句和法文例句必须自然。
- 两个例句必须明确支撑你给出的义项差异或义项重合。
- 中文译文必须同时覆盖两边例句的核心教学点。
- 优先使用课堂、学校、家庭、社会、媒体、工作、日常生活等高教学价值语境。

六、标签要求
- `tags` 使用 2-4 个标签，用 `|` 分隔。
- 标签优先表达教学用途，如：
  `high-risk`、`education`、`core`、`verb`、`a1-a2`、`b1-b2`、`advanced`、`media`、`science`

七、禁止事项
- 不要生成重复词对。
- 不要生成明显专名、缩写、冷僻技术术语、过度罕见词。
- 不要生成只有拼写像但教学上几乎无价值的词对。
- 不要生成需要多义项批量导入才能成立的复杂条目。
- 不要输出解释说明，不要输出过程日志，不要输出 Markdown。

最终要求：
1. 直接把文件写到 `{{TARGET_FILE}}`
2. 然后给出极简摘要：
   - 新增条数
   - 四类词对数量
   - 难度分布
   - 校验是否通过
3. 不要向我提问，不要停在半成品。
```

## Prompt 2：高风险假朋友扩容

适用场景：你要优先扩容最适合做诊断和干预的词。

```text
继续在当前工作区中生成“高风险假朋友”专用词库。

执行要求：
1. 先读取：
   - /mnt/d/huashi2/docs/semantic-lexicon-spec-v2.md
   - /mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-high-risk-false-friends.csv
   - /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-80.csv
2. 新建文件：
   - {{TARGET_FILE}}
3. 生成 {{ROW_COUNT}} 条新数据，全部必须是 `false_friend`。

硬性约束：
- `false_friend_risk >= 0.88`
- `semantic_overlap_score <= 0.25`
- `difficulty_level` 优先 4 或 5
- `default_context_support` 优先 `high` 或 `medium`
- 词对必须能明显诱发表层误判
- 优先教育、媒体、工作、日常生活、课堂高频语境
- 必须避开现有所有 CSV 中已出现的词对

交付要求：
1. 直接写入 `{{TARGET_FILE}}`
2. 运行机器校验
3. 输出极简统计，不要输出额外解释
```

## Prompt 3：生产审校 Prompt

适用场景：你已经有一个 CSV，但怀疑它还不够生产级。

```text
你现在是英法形似词教学词库的生产审校员，不负责新增数据，只负责严格挑错。

请读取以下文件：
- /mnt/d/huashi2/docs/semantic-lexicon-spec-v2.md
- /mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java
- {{TARGET_FILE}}

你的任务：
1. 审查 `{{TARGET_FILE}}` 中的每一行是否满足生产要求。
2. 检查维度必须包括：
   - 是否满足 21 列导入结构
   - 枚举值是否合法
   - 分数和难度是否自洽
   - `lexical_pair_type` 是否和义项差异一致
   - 中文释义是否教学化且清晰
   - sense 与 example 是否匹配
   - 英文和法文例句是否自然
   - 是否存在低价值词对
   - 是否存在边界过于模糊的词对
   - 是否存在疑似重复或近重复
3. 生成一个审校报告文件：
   - {{REPORT_FILE}}

审校报告格式固定为 Markdown 表格，列为：
| row_no | pair | severity | problem_type | why_problematic | revision_direction |

严重级别只允许：
- critical
- major
- minor

`problem_type` 只允许：
- schema_error
- enum_error
- score_inconsistency
- weak_pedagogical_value
- unnatural_example
- weak_gloss
- pair_type_mismatch
- duplicate_or_near_duplicate
- low_confidence_entry

要求：
1. 不要帮我修，只输出问题。
2. 问题必须尖锐，不要客气。
3. 如果某行没有问题，不要写进报告。
4. 最后补一段总评：
   - 是否达到生产可导入标准
   - 是否达到生产可教学标准
   - 是否建议直接入库
```

## Prompt 4：基于审校报告自动修复

适用场景：你已经有一个待修复 CSV 和一份审校报告。

```text
你现在的任务不是生成新词，而是修复已有导入 CSV，使其达到可入库标准。

请读取：
- /mnt/d/huashi2/docs/semantic-lexicon-spec-v2.md
- /mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java
- {{SOURCE_CSV}}
- {{REPORT_FILE}}

执行规则：
1. 逐条读取审校报告。
2. 对 `critical` 和 `major` 问题必须修复。
3. 如果某条无法在高置信度下修复，直接删除该行，不要硬改。
4. 修复后输出到：
   - {{FIXED_CSV}}
5. 修复后再次执行机器校验：
   - 21 列
   - 枚举值
   - 分数范围
   - 是否有例句无义项
6. 最后输出修复摘要：
   - 修复了多少条
   - 删除了多少条
   - 剩余多少条
   - 校验是否通过

禁止事项：
- 不要新增本轮报告没有涉及的大量新词
- 不要改变字段顺序
- 不要输出解释性长文
```

## Prompt 5：大规模滚动扩容 Prompt

适用场景：你已经有若干批 CSV，准备继续做 `pack-3`、`pack-4`、`pack-5`。

```text
继续当前工作区里的词库生产，不要重置上下文。

你的目标是做“滚动扩容”，而不是重新从零生成。

先读取：
- /mnt/d/huashi2/docs/semantic-lexicon-spec-v2.md
- /mnt/d/huashi2/app-server/src/main/java/com/huashi/eftransfer/app/modules/lexicon/imports/support/LexicalImportTemplateSupport.java
- /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-80.csv
- /mnt/d/huashi2/docs/semantic-lexicon-v2-import-ready-high-risk-false-friends.csv
- {{EXTRA_EXISTING_FILES}}

任务：
1. 新建 `{{TARGET_FILE}}`
2. 生成 {{ROW_COUNT}} 条全新词对
3. 与所有已存在 CSV 去重
4. 维持类型和难度分布平衡
5. 提高这些维度的覆盖度：
   - 教育
   - 家庭
   - 工作
   - 法律
   - 医疗
   - 媒体
   - 行政
   - 科学
   - 日常生活
   - 抽象学术词汇

特别要求：
- 新批次必须尽量和现有批次形成互补，不要只是换一批相似结构的词。
- 优先补足“高教学价值但尚未覆盖”的高频词。
- 任何低置信条目都不要保留。

交付：
1. 直接落盘到 `{{TARGET_FILE}}`
2. 输出分布摘要
3. 输出校验通过结论
```

## 推荐执行顺序

如果你现在要继续生产数据，建议直接这样跑：

1. 用 Prompt 1 生成 `pack-3`，例如 80 条
2. 用 Prompt 3 审校 `pack-3`
3. 用 Prompt 4 修复 `pack-3`
4. 用 Prompt 2 额外生成一个高风险假朋友包
5. 用 Prompt 5 做下一批滚动扩容

## 最实用的一条

如果只想先复制一条最强可用版本，优先用 Prompt 1。

它最接近“当前对话里继续干活”的顶级生产 Prompt。
