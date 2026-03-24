# 语义词库规范 V2

适用对象：EF Transfer Platform 项目的研究、内容、数据与产品实现团队。

本文档用于替代早期的聊天导出草稿，目标是把“英法形似词认知迁移词库”从概念方案整理为可研究、可导入、可诊断、可训练、可同步到 RAG 的正式规范。

## 1. 文档目标

本规范同时解决四个问题：

1. 统一研究团队对“词库条目”应包含哪些信息的理解。
2. 对齐当前仓库已经实现的真实字段、导入模板和业务链路。
3. 区分“研究标注层”和“系统入库层”，避免把所有字段都塞进主词库表。
4. 提供可直接使用的 Prompt 模板，支持候选词对挖掘、词条生成、诊断题项生成、训练内容生成和质检闭环。

## 2. 范围与边界

本项目里的“语义词库”不等于一个大而全的语言学数据库，而是围绕以下问题服务：

- 中国英法双语学习者处理英法形似词时的正迁移与负迁移机制。
- 诊断任务中的词义判断、反应时表现和语境利用。
- 训练任务中的负迁移抑制、正迁移强化、语境修复与快速识别。
- 教师工作台中的词对维护、诊断模板设计、干预建议和 RAG 检索。

本规范明确区分三类对象：

- 词库条目：词对、义项、例句及基础风险信息。
- 诊断题项：围绕某个词对生成的可测量题目结构。
- 训练内容：围绕某个词对生成的训练题、反馈和复习策略。

结论：词库条目不直接等于诊断题项，也不直接等于训练题项。

## 3. V2 设计原则

1. 先保证入库可行，再追求研究字段完整。
2. 所有核心枚举必须与代码中的真实值保持一致。
3. 模型可以辅助生成初稿，但主键、发布时间、运维状态由系统控制。
4. 一切“解释性标签”优先作为扩展层，不强行写入主词库表。
5. 所有面向模型的 Prompt 必须有明确输入契约、输出契约和失败处理策略。

## 4. 三层数据模型

### 4.1 层 A：系统主词库层

这是当前仓库已经实现、可直接导入和维护的结构。主词库负责：

- 词对展示与检索
- 诊断与训练的基础词汇来源
- 知识变更事件发布
- app-server 到 ai-gateway 的知识导出

当前导入模板字段如下。

| 字段名 | 必填 | 类型 | 允许值 / 格式 | 说明 |
| --- | --- | --- | --- | --- |
| `english_word` | 是 | string | 1-128 字符 | 英语词 |
| `french_word` | 是 | string | 1-128 字符 | 法语词 |
| `chinese_gloss` | 是 | string | 1-255 字符 | 面向教学展示的中文释义 |
| `lexical_pair_type` | 是 | enum | `cognate` / `false_friend` / `partial_cognate` / `orthographic_similar` | 词对类型 |
| `semantic_overlap_score` | 是 | decimal | 0.0-1.0 | 语义重合度 |
| `false_friend_risk` | 是 | decimal | 0.0-1.0 | 负迁移风险 |
| `default_context_support` | 是 | enum | `low` / `medium` / `high` | 默认语境支持 |
| `difficulty_level` | 是 | integer | 1-5 | 难度等级 |
| `notes` | 否 | string | 自由文本 | 教师备注 |
| `source` | 否 | string | 自由文本 | 来源、教材或生成渠道 |
| `active` | 否 | boolean | `true` / `false` | 是否启用 |
| `tags` | 否 | string | 以 `|` 分隔 | 标签集合 |
| `knowledge_status` | 否 | enum | `draft` / `ready` / `disabled` | 知识状态 |
| `embedding_status` | 否 | enum | `pending` / `embedded` / `failed` | 向量状态 |
| `sense_english_definition` | 否 | string | 自由文本 | 主义项英文释义 |
| `sense_french_definition` | 否 | string | 自由文本 | 主义项法文释义 |
| `sense_chinese_definition` | 否 | string | 自由文本 | 主义项中文释义 |
| `example_english` | 否 | string | 自由文本 | 主英文例句 |
| `example_french` | 否 | string | 自由文本 | 主法文例句 |
| `example_chinese` | 否 | string | 自由文本 | 主中文译文 |
| `example_context_support` | 否 | enum | `low` / `medium` / `high` | 主例句语境支持 |

关键限制：

1. 批量导入阶段只支持“一个主义项 + 一组主例句”。
2. 如果填写了例句，必须同时填写对应义项释义。
3. `difficulty_level` 是整数 1-5，不是 `easy/medium/hard`。
4. `entry_id` 不属于导入字段，主键由系统生成。

### 4.2 层 B：研究扩展标注层

以下字段对研究和教学分析有价值，但当前项目并未作为主词库导入字段直接实现。V2 建议将它们视为扩展层，可保存在研究数据表、JSON 扩展字段或离线标注资产中。

| 字段名 | 类型 | 用途 | 是否直接入主词库 |
| --- | --- | --- | --- |
| `transfer_prediction` | enum | `positive` / `negative` / `mixed` 的总体迁移判断 | 否 |
| `transfer_risk_reason` | string[] | 负迁移或混合迁移原因 | 否 |
| `common_error_patterns` | string[] | 学习者常见偏误模式 | 否 |
| `disambiguation_cues` | string[] | 区分策略与提示线索 | 否 |
| `learner_level_fit` | string | 推荐适配水平，如 `A2-B1` | 否 |
| `diagnostic_tags` | string[] | 诊断标签 | 否 |
| `error_type_labels` | string[] | 错误类型标签 | 否 |
| `recommended_intervention` | string[] | 干预建议 | 否 |
| `mastery_indicators` | string[] | 可观测掌握指标 | 否 |
| `teaching_suggestion` | string[] | 教学建议 | 可部分映射到 `notes`，不建议直接入主词库 |
| `exercise_tags` | string[] | 训练标签 | 可部分映射到 `tags`，但建议保留独立语义 |
| `orthographic_similarity_score` | decimal | 词形相似度 | 否 |
| `phonological_similarity_score` | decimal | 音形相似度 | 否 |
| `confidence_score` | decimal | 模型自评置信度 | 否 |

结论：

- 主词库优先存“稳定、可维护、可被系统直接消费”的字段。
- 研究标签和产品标签不应直接替代主词库基础字段。

### 4.3 层 C：衍生内容层

这是基于词库条目进一步生成的内容资产，不直接等于词库本体：

- 诊断模板 item
- 训练题项
- 教学干预建议
- RAG 回答时可引用的补充知识块

## 5. 枚举与判定标准

### 5.1 `lexical_pair_type`

必须与代码中的真实枚举一致：

| 值 | 含义 | 使用建议 |
| --- | --- | --- |
| `cognate` | 同源词，整体上更利于正迁移 | 语义高度相近，义项偏差小 |
| `false_friend` | 假朋友，形式相似但核心义项明显不同 | 负迁移风险高 |
| `partial_cognate` | 部分同源，部分义项重合、部分不重合 | 适合混合迁移研究 |
| `orthographic_similar` | 近形词，主要是书写形式相近 | 可用于干扰研究与纠错训练 |

V2 判定建议：

- 若核心义项高度重合且教学上主要目标是强化正迁移，优先标 `cognate`。
- 若形式相似但核心义项边界明显错位，优先标 `false_friend`。
- 若共享一个以上义项，但仍存在关键差异义项，优先标 `partial_cognate`。
- 若关系主要来自拼写近似、而非稳定同源关系，优先标 `orthographic_similar`。

### 5.2 `semantic_overlap_score`

建议作为 0-1 连续值，不要写成文本标签。

建议区间：

| 区间 | 含义 |
| --- | --- |
| 0.00-0.20 | 几乎无共享义项 |
| 0.21-0.49 | 有局部接近，但整体差异大 |
| 0.50-0.74 | 有一定共享义项，存在教学上值得强调的重叠 |
| 0.75-1.00 | 高度重合，适合正迁移强化 |

### 5.3 `false_friend_risk`

建议作为 0-1 连续值，用于表达负迁移风险，不等于 `lexical_pair_type`。

建议区间：

| 区间 | 含义 |
| --- | --- |
| 0.00-0.24 | 低风险 |
| 0.25-0.49 | 中低风险 |
| 0.50-0.74 | 中高风险 |
| 0.75-1.00 | 高风险 |

说明：

- `cognate` 也可能有中等风险。
- `false_friend` 通常风险较高，但仍需结合语境和学习者水平判断。

### 5.4 `default_context_support`

必须使用 `low / medium / high`。

判定建议：

| 值 | 判定标准 |
| --- | --- |
| `low` | 单句线索弱，词义可被多种解释，容易诱发表层匹配 |
| `medium` | 有部分搭配或语义线索，但仍存在误判空间 |
| `high` | 语义和搭配线索充分，足以帮助正确辨义 |

### 5.5 `difficulty_level`

当前系统必须使用 1-5 整数。

建议映射：

| 值 | 含义 |
| --- | --- |
| 1 | 初级、义项单一、线索明显 |
| 2 | 初级偏上、少量干扰 |
| 3 | 中等、适合常规教学 |
| 4 | 中高、易混淆或需结合语境 |
| 5 | 高难、义项复杂或迁移风险显著 |

不再使用 `easy / medium / hard`。

### 5.6 `transfer_prediction`

这是研究扩展层字段，不是当前主词库字段。

V2 建议：

- `positive`：共享义项稳定、风险低、可直接利用已有跨语联想。
- `negative`：形式诱导强、核心义项错位明显、误判后果稳定。
- `mixed`：部分正迁移、部分负迁移，需依赖语境或学习者水平。

建议先由模型给出初判，再人工复核，不要直接作为主词库核心事实。

## 6. 推荐的词条结构

### 6.1 研究标准结构

研究和离线标注建议使用以下结构：

```json
{
  "pair": {
    "english_word": "",
    "french_word": "",
    "chinese_gloss": "",
    "lexical_pair_type": "partial_cognate",
    "semantic_overlap_score": 0.62,
    "false_friend_risk": 0.58,
    "default_context_support": "medium",
    "difficulty_level": 3
  },
  "research_annotations": {
    "transfer_prediction": "mixed",
    "transfer_risk_reason": [],
    "common_error_patterns": [],
    "disambiguation_cues": [],
    "diagnostic_tags": [],
    "exercise_tags": [],
    "learner_level_fit": "B1-B2",
    "confidence_score": 0.84
  },
  "senses": [
    {
      "sense_english_definition": "",
      "sense_french_definition": "",
      "sense_chinese_definition": "",
      "examples": [
        {
          "example_english": "",
          "example_french": "",
          "example_chinese": "",
          "example_context_support": "medium"
        }
      ]
    }
  ]
}
```

### 6.2 当前项目可导入结构

当前项目导入时，应收敛为一行一条词对：

```json
{
  "english_word": "",
  "french_word": "",
  "chinese_gloss": "",
  "lexical_pair_type": "false_friend",
  "semantic_overlap_score": 0.10,
  "false_friend_risk": 0.92,
  "default_context_support": "high",
  "difficulty_level": 4,
  "notes": "",
  "source": "llm_v2_human_reviewed",
  "active": true,
  "tags": "false-friend|high-risk|b1-b2",
  "knowledge_status": "ready",
  "embedding_status": "pending",
  "sense_english_definition": "",
  "sense_french_definition": "",
  "sense_chinese_definition": "",
  "example_english": "",
  "example_french": "",
  "example_chinese": "",
  "example_context_support": "high"
}
```

## 7. 字段映射规则

原始草稿中的字段需要按以下方式重写：

| 草稿字段 | V2 建议归属 | 处理方式 |
| --- | --- | --- |
| `entry_id` | 系统主键 | 删除，不要求模型生成 |
| `fr_word` | 主词库层 | 改为 `french_word` |
| `en_word` | 主词库层 | 改为 `english_word` |
| `relation_type` | 主词库层 | 改为 `lexical_pair_type` |
| `semantic_overlap` | 主词库层 | 改为 `semantic_overlap_score` |
| `context_support_level` | 主词库层 | 改为 `default_context_support` 或 `example_context_support` |
| `difficulty_level = easy/medium/hard` | 主词库层 | 改为 1-5 整数 |
| `shared_meaning_zh` | 主词库层 / 研究扩展层 | 可收敛进 `chinese_gloss` 或义项层 |
| `distinct_meaning_fr_zh` | 义项层 / 研究扩展层 | 不直接入导入 CSV |
| `distinct_meaning_en_zh` | 义项层 / 研究扩展层 | 不直接入导入 CSV |
| `teaching_suggestion` | 研究扩展层 | 不直接入主词库，必要时摘要进 `notes` |
| `exercise_tags` | 研究扩展层 | 必要时映射为 `tags` 的子集 |
| `confidence_score` | 研究扩展层 | 不直接入主词库 |

## 8. 与诊断和训练链路的衔接

### 8.1 词库条目不是诊断模板

当前系统里的诊断模板 item 至少需要以下字段：

- `lexicalPairId`
- `taskType`
- `blockCode`
- `sortOrder`
- `contextSupportLevel`
- `expectedSemanticMatch`
- `stimulus`
- `options`
- `correctAnswerKey`
- `scoringProfile`

因此：

- 词库条目负责提供词对、义项、例句和基础风险。
- 诊断模板负责把词库条目组织成可测量任务。

### 8.2 训练内容来源于诊断结果

训练推荐不是直接由词库字段生成，而是结合以下来源：

- 最新诊断结果
- 高风险词对
- 错题本
- 复习计划

因此，V2 建议将训练标签保留在扩展层，用于辅助生成训练内容，但不要误以为它们能替代诊断数据。

## 9. 数据生产工作流

V2 推荐工作流如下：

1. 候选词对挖掘。
2. 研究层初标。
3. 人工复核关系类型、共享义项和风险评分。
4. 生成当前项目可导入的主词库行。
5. 导入到词对管理页并补充复杂义项和例句。
6. 基于入库后的 `lexicalPairId` 生成诊断模板 item。
7. 根据诊断结果生成训练内容和教师干预内容。
8. 做词库、题项、训练内容三层质检。
9. 验证知识同步与 RAG 检索效果。

## 10. 人工复核要求

以下字段允许模型初稿，但必须人工复核：

- `lexical_pair_type`
- `semantic_overlap_score`
- `false_friend_risk`
- `chinese_gloss`
- `sense_*_definition`
- `example_*`
- `transfer_prediction`
- `common_error_patterns`

以下字段由系统生成或系统控制：

- 词条主键
- 导入批次信息
- 创建时间、更新时间
- 是否发布到诊断模板
- 是否进入训练计划
- 向量状态与重建状态

## 11. Prompt 设计要求

所有可落地 Prompt 必须满足以下约束：

1. 指定输出格式为合法 JSON。
2. 指定字段名称必须与目标层一致。
3. 不允许模型输出解释性前后缀。
4. 不要求模型生成系统主键。
5. 对枚举字段给出固定候选值。
6. 对数值字段给出明确范围。
7. 对数组字段明确允许为空还是必须非空。

## 12. Prompt 包 V2

### 12.1 System Prompt

```text
你输出的内容将用于“英法形似词认知迁移词库”及其衍生诊断、训练和教学内容。请严格遵守以下要求：
1. 保持字段名与输入契约一致。
2. 输出必须是严格合法的 JSON。
3. 不生成系统主键、不生成批次号、不生成数据库状态字段。
4. 若证据不足，不要强行断言高度确定的语义关系，应降低 confidence 或标记待复核。
5. 解释必须面向中国英法双语学习者，强调可教学化。
6. `lexical_pair_type` 只能使用：cognate / false_friend / partial_cognate / orthographic_similar。
7. `default_context_support` 与 `example_context_support` 只能使用：low / medium / high。
8. `difficulty_level` 只能使用 1-5 整数。
9. `semantic_overlap_score` 与 `false_friend_risk` 只能使用 0-1 小数。
```

### 12.2 Prompt A：候选词对挖掘

```text
你是一名多语词汇比对助手。请从给定的法语词表和英语词表中，筛选适合进入“英法形似词认知迁移词库”的候选词对。

筛选标准：
1. 优先保留同源词、假朋友、部分同源、近形词。
2. 排除明显专名、缩写、极罕见或不适合教学的词。
3. 必须给出候选理由，不得只按拼写距离机械输出。

输出 JSON 数组，每项字段固定为：
{
  "english_word": "",
  "french_word": "",
  "orthographic_similarity_score": 0.00,
  "probable_lexical_pair_type": "cognate",
  "probable_transfer_prediction": "positive",
  "brief_reason_zh": ""
}

其中：
- probable_lexical_pair_type 只能取：cognate / false_friend / partial_cognate / orthographic_similar / uncertain
- probable_transfer_prediction 只能取：positive / negative / mixed / uncertain
- 不要输出任何额外说明文字

法语词表：
{{french_word_list}}

英语词表：
{{english_word_list}}
```

### 12.3 Prompt B：研究层标准词条生成

```text
你是一名应用语言学与教育技术交叉研究助手。请围绕中国英法双语学习者处理英法形似词时的认知迁移机制，为给定词对生成研究层标准词条。

输出 JSON 数组，每项结构固定为：
{
  "pair": {
    "english_word": "",
    "french_word": "",
    "chinese_gloss": "",
    "lexical_pair_type": "partial_cognate",
    "semantic_overlap_score": 0.00,
    "false_friend_risk": 0.00,
    "default_context_support": "medium",
    "difficulty_level": 3
  },
  "research_annotations": {
    "transfer_prediction": "mixed",
    "transfer_risk_reason": [],
    "common_error_patterns": [],
    "disambiguation_cues": [],
    "learner_level_fit": "B1-B2",
    "diagnostic_tags": [],
    "exercise_tags": [],
    "confidence_score": 0.00
  },
  "senses": [
    {
      "sense_english_definition": "",
      "sense_french_definition": "",
      "sense_chinese_definition": "",
      "examples": [
        {
          "example_english": "",
          "example_french": "",
          "example_chinese": "",
          "example_context_support": "medium"
        }
      ]
    }
  ]
}

要求：
1. 所有解释使用中文。
2. `transfer_risk_reason`、`common_error_patterns`、`disambiguation_cues` 各至少给出 2 条。
3. 若只适合单义项教学，可只输出 1 个 sense。
4. 若证据不足，不要虚构复杂义项。
5. 不要输出任何额外说明文字。

词对列表：
{{pair_list}}
```

### 12.4 Prompt C：当前项目可导入词条生成

```text
请将给定的英法词对转化为适合 EF Transfer Platform 当前项目导入的标准 JSON 数组。

输出字段固定为：
english_word,
french_word,
chinese_gloss,
lexical_pair_type,
semantic_overlap_score,
false_friend_risk,
default_context_support,
difficulty_level,
notes,
source,
active,
tags,
knowledge_status,
embedding_status,
sense_english_definition,
sense_french_definition,
sense_chinese_definition,
example_english,
example_french,
example_chinese,
example_context_support

严格约束：
1. `lexical_pair_type` 只能取：cognate / false_friend / partial_cognate / orthographic_similar。
2. `semantic_overlap_score` 和 `false_friend_risk` 必须是 0-1 小数。
3. `default_context_support` 和 `example_context_support` 只能取：low / medium / high。
4. `difficulty_level` 必须是 1-5 整数。
5. `active` 必须是 true 或 false。
6. `knowledge_status` 只能取：draft / ready / disabled。
7. `embedding_status` 只能取：pending / embedded / failed。
8. `tags` 使用单个字符串，以 `|` 分隔。
9. 如果提供了例句，必须同时提供对应义项释义。
10. `source` 固定写为 `llm_v2_human_reviewed`。
11. 不生成 `entry_id`。
12. 只输出 JSON 数组，不要输出任何额外说明文字。

词对列表：
{{pair_list}}
```

### 12.5 Prompt D：诊断题项生成

```text
你是一名教育测评设计师。请基于已入库的词对信息，为诊断模板生成题项草稿。

输出 JSON 数组，每项字段固定为：
{
  "lexicalPairId": 0,
  "taskType": "reaction_time",
  "blockCode": "B1",
  "sortOrder": 1,
  "contextSupportLevel": "low",
  "expectedSemanticMatch": true,
  "stimulus": {
    "instruction": "",
    "promptText": "",
    "contextSentence": ""
  },
  "options": [
    {
      "key": "",
      "label": "",
      "semanticMatch": true,
      "ignoreContextTrap": false
    }
  ],
  "correctAnswerKey": "",
  "scoringProfile": {
    "reactionTimeWeight": 1.0,
    "hesitationWeight": 1.0,
    "accuracyWeight": 1.0
  }
}

要求：
1. `taskType` 只能取：reaction_time / semantic_judgement。
2. `contextSupportLevel` 只能取：low / medium / high。
3. 每题必须与给定的词对风险点一致。
4. 低语境支持题优先诱发表层误判，高语境支持题优先检验修复能力。
5. 不要输出任何额外说明文字。

输入词条：
{{lexical_entries_with_ids}}
```

### 12.6 Prompt E：训练内容生成

```text
你是一名多语词汇训练内容设计师。请基于给定词条生成训练内容。

输出 JSON 数组，每项字段固定为：
{
  "lexicalPairId": 0,
  "relation_type": "false_friend",
  "training_objective": "",
  "recommended_training_mode": "false_friend_discrimination",
  "micro_explanations_zh": [],
  "question_items": [
    {
      "task_type": "meaning_discrimination",
      "prompt": "",
      "options": [],
      "answer": "",
      "feedback_correct_zh": "",
      "feedback_wrong_zh": ""
    }
  ],
  "review_strategy": "",
  "next_recommendation": ""
}

要求：
1. `recommended_training_mode` 只能取：
   - cognate_strengthening
   - false_friend_discrimination
   - context_repair
   - rapid_recognition
2. 每个词条至少生成 3 道题。
3. `feedback_wrong_zh` 必须明确指出对应的迁移偏差。
4. 不要输出任何额外说明文字。

输入词条：
{{lexical_entries_with_ids}}
```

### 12.7 Prompt F：词库与内容质检

```text
你是一名教育词库质检员。请检查以下词库条目及其衍生内容是否存在问题，并输出修订建议。

检查维度：
1. 词义准确性
2. 关系类型判定
3. 风险评分自洽性
4. 中文释义清晰度
5. 例句自然度
6. 枚举值兼容性
7. 字段缺失
8. 重复或近重复
9. 教学适配性

输出 JSON 数组，每项结构固定为：
{
  "target_type": "lexical_entry",
  "target_key": "",
  "problem_detected": true,
  "problem_type": "字段缺失",
  "why_it_is_problematic_zh": "",
  "revision_suggestion_zh": "",
  "revised_fields": {}
}

约束：
1. `problem_type` 只能取：释义错误、关系误标、迁移预测不当、例句不自然、标签不统一、字段缺失、重复条目、教学适配性不足、枚举不兼容。
2. 若没有问题，写 `problem_detected: false`，其余字段可最小化保留。
3. 不要输出任何额外说明文字。

输入数据：
{{entries_or_assets}}
```

## 13. 实施建议

若目标是尽快在当前项目跑通，推荐最短路径：

1. 用 Prompt A 挖掘候选词对。
2. 用 Prompt B 生成研究层初稿。
3. 人工复核关系类型、风险和中文释义。
4. 用 Prompt C 生成当前项目可导入 JSON 或 CSV。
5. 导入到词对管理页。
6. 在界面中补充复杂义项和例句。
7. 用 Prompt D 生成诊断题项草稿。
8. 用 Prompt E 生成训练内容草稿。
9. 用 Prompt F 做最终质检。

## 14. 当前项目中的落地入口

当前仓库里的实际操作入口如下：

1. 词对管理入口：
   - 教师：`/teacher/lexical-pairs`
   - 管理员：`/admin/lexical-pairs`
2. 导入方式：
   - 支持 `CSV` 和 `XLSX`
   - 上传后先进入导入批次和草稿确认，不会直接入库
   - 可逐行修正、跳过，再执行正式导入
3. 诊断模板入口：
   - `/teacher/diagnosis-templates`
4. AI / RAG 运维入口：
   - `/admin/config-center`

导入后的最小验证动作：

1. 在词对列表中搜索新导入词对，确认字段和例句显示正常。
2. 抽样检查若干高风险词对，确认 `lexical_pair_type`、`semantic_overlap_score`、`false_friend_risk` 是否合理。
3. 使用新词对生成或更新诊断模板，验证题项是否可发布。
4. 若目标环境的知识同步未自动完成，或需要补偿式刷新，应在管理页执行 RAG reindex 并验证检索结果。

## 15. 本版本相对旧草稿的关键修订

1. 去掉了让模型生成 `entry_id` 的要求。
2. 将 `fr_word / en_word` 统一为项目真实字段名。
3. 将 `relation_type` 统一映射为 `lexical_pair_type`。
4. 将 `difficulty_level` 从文本标签改为 1-5 整数。
5. 明确了研究层与入库层的边界。
6. 明确了词库条目、诊断题项、训练内容三层对象。
7. 明确了当前批量导入只支持一个主义项和一组主例句。
8. 明确了所有 Prompt 的输出契约和枚举约束。

## 16. 后续扩展建议

若后续项目需要进一步研究化，可增加以下能力，但不属于当前 V2 的强制范围：

- 独立的研究标注表
- 多义项批量导入
- 诊断标签与训练标签独立存储
- 模型输出与人工审核双轨工作流
- 词库版本化与发布审批
- 词条导入后的自动回归检查与 RAG 验证
