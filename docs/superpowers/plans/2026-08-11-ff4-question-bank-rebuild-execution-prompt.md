# 法语专四假朋友题库重建与研究问卷优化：执行提示词

> ⚠️ 本文为 2026-08-11 的历史执行计划，部分内容已被后续交付取代：
> - 生产 V1 答题时限已于 2026-08-12 从 40 分钟调整为 60 分钟（本文"40 分钟"表述失效）。
> - 实际交付的 V3 为 **239 道计分题、120 分钟**（四类：词义 27 / 句选 16 / 判断 22 / 拼写 174），并非本文规划的"60 道计分题、四类各 15 题"。
> - V3 已以 `LEXIBRIDGE_RESEARCH_V3` 种子落地（DRAFT/APPROVED 状态见种子文件），不再是"待定方案"。

下面“主执行提示词”可直接交给具备本地文件、终端、浏览器和服务器访问能力的模型。不要把 SSH 私钥写入文件、提交到 Git 或显示在日志中；凭据由用户在运行环境中另行提供。

## 已核实的现状

1. 用户最新规则来自：
   - `/Users/oo/Library/Containers/com.tencent.xinWeChat/Data/Documents/xwechat_files/wxid_ilz4sgwyo3hc22_68a9/temp/drag/0811 题库修改意见(1).docx`
2. 用户所说的 `法语专四假朋友题库(1).xlsx` 与代码库内文件逐字节相同：
   - `/Users/oo/Library/Containers/com.tencent.xinWeChat/Data/Documents/xwechat_files/wxid_ilz4sgwyo3hc22_68a9/msg/file/2026-08/法语专四假朋友题库(1).xlsx`
   - `/Users/oo/project/huashi2/docs/data/lexi-bridge-ff4/student-deliverables/法语专四假朋友题库.xlsx`
3. 旧题库共 445 题：单选 112、选词填空 111、判断 111、拼写 111；四个工作表之间没有重复词，但这是按词表顺序轮转分配，不是按新选词规则筛选。
4. 旧题库的可复现问题：
   - 单选题仅 1/112 的错误选项含易混英文词的中文义；新规则要求每题必须有这一项。
   - 句子选词题有 52/111 使用 `Pour exprimer...` 释义模板，14/111 出现词头/词性残片式题干，并非专四词书中的完整例句。
   - 判断题答案被人为平衡为 V=56、F=55；仅 26/111 直接使用了易混英文词的首个中文义，其他题可能使用法语正确义或随机替换义。
   - 拼写题仅 54/111 满足“法英拼写编辑距离 1–4 且不是简单 -er 形态差异”的初步机械规则。
5. 445 个交集词的现有关系标签为：
   - A 核心义不对应/原书另词表达：94
   - B 部分对应/语义范围变化：346
   - C 语义范围/用法边界：4
   - D 同形同义借词边界：1
6. 只做中文义项精确字符串初筛时，法语前 2–3 个义项与英文义无直接重合的词约 247 个；严格采用现有 A 类标签仅 94 个。前三种题型若继续各做约 111 题并要求全局不重复，至少需要 333 个语义完全不重合词，因此旧题量目标在当前 445 词池内不可实现。不得为了凑数放宽规则。
7. 拼写距离初筛可得约 211 个候选，但仍须人工排除规则性词尾、派生构词或不会造成真实混淆的词。
8. 线上发布 `RES-AFC02D0823F2` 当前是生产 V1：60 道正式题、40 分钟、状态 PUBLISHED，二维码免码入口关闭，发布时间为 2026-08-08。生产库中没有 `LEXI_BRIDGE_FF4` 题目版本；现有 445 题 Excel 尚未导入生产题库。
9. 本地代码已有 `LEXIBRIDGE_RESEARCH_V2.json`，但线上仍运行 V1。必须把 V1 当作不可变历史版本，不得原地覆盖已发布试卷。
10. 当前公开问卷界面能填写短文本，但没有“答错一次后立即显示首字母”的逐题判错/提示机制。若采用拼写题，必须补充交互、接口状态、审计字段和测试，不能只生成 Excel。

---

## 主执行提示词

你是一名兼具法语词汇教研、测量问卷设计、数据工程和全栈开发能力的高级工程师。请在 `/Users/oo/project/huashi2` 中完成“法语专四假朋友题库 V2 重建 + 研究问卷 V3 内容方案与本地实现”。

你的第一原则是证据可追溯和研究有效性。不要沿用旧题库的题目文本，不要为了保持 445 题或每类 111 题而降低筛选标准。用户明确表示旧题库需要全部推翻重来，但目前尚未逐词审定所有法语/英语释义是否正确，因此所有新内容在人工词汇审定前必须标记为 `LEXICAL_REVIEW_PENDING`，不得声称“内容已审定正确”。

### 一、必须先读的材料

完整读取并交叉核对：

- 最新修改意见：`/Users/oo/Library/Containers/com.tencent.xinWeChat/Data/Documents/xwechat_files/wxid_ilz4sgwyo3hc22_68a9/temp/drag/0811 题库修改意见(1).docx`
- 被推翻的旧题库：`/Users/oo/Library/Containers/com.tencent.xinWeChat/Data/Documents/xwechat_files/wxid_ilz4sgwyo3hc22_68a9/msg/file/2026-08/法语专四假朋友题库(1).xlsx`
- 代码库词汇总表：`docs/data/lexi-bridge-ff4/student-deliverables/法语专四假朋友词汇总表.xlsx`
- 现有词表与生成数据：
  - `docs/data/lexi-bridge-ff4/wordbook.jsonl`
  - `docs/data/lexi-bridge-ff4/tem4-candidates.jsonl`
  - `docs/data/lexi-bridge-ff4/intersection-decisions.jsonl`
  - `docs/data/lexi-bridge-ff4/source-ocr-cache/false-friends-pages.jsonl`
- 两个原始 PDF：
  - `docs/source-materials/lexi-bridge-ff4/french-tem4-core-vocabulary.pdf`
  - `docs/source-materials/lexi-bridge-ff4/french-false-friends-kirk-greene.pdf`
- 旧生成器与审计逻辑：
  - `scripts/build-lexi-bridge-ff4-question-bank.ps1`
  - `scripts/build-lexi-bridge-ff4-student-deliverables.py`
  - `scripts/audit-lexi-bridge-ff4-package.ps1`
- 当前研究问卷：
  - `app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V1.json`
  - `app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V2.json`
  - `src/pages/research/index.tsx`
  - `app-server/src/main/java/com/huashi/eftransfer/app/modules/assessment/service/PublicAssessmentService.java`

先输出一份简短的“证据与缺口确认”，然后直接继续执行；只有出现会实质改变研究设计且无法从材料判断的阻塞项时才询问用户。

### 二、四类题的硬性选词规则

#### 题型 1：词义单选

候选词必须同时满足：

1. 法语假朋友词典与法语专四词书交集；
2. 只取专四词书中排在最前的 2–3 个法语中文义；
3. 这些核心义与易混英文词中文义在概念上完全不重合；不能仅靠字符串不相同判定；
4. 部分对应、上下位包含、常见语境可互译或共享核心义的词全部排除，例如 `classe/class`；
5. 每个入选结论都必须保存双源页码、原文证据、判定理由、审查状态与置信度。

题目格式：法语目标词 + 四个中文选项。四个选项必须是：

- 1 个法语词正确中文义；
- 1 个易混英文词中文义，作为核心迁移干扰项；
- 2 个同词性、相近难度、互不包含且与正确答案无同义关系的干扰项。

答案位置应平衡，但不得以牺牲选项质量换取机械平衡。

#### 题型 2：句子选词

候选词采用与题型 1 相同的严格语义不重合规则，并且不得与任何其他题型重复目标词。

题干必须使用专四词书原始、完整、可定位的例句，并在界面中将目标词加粗和下划线。不得把释义模板、词组、词头残片或模型自造句冒充原书例句。若原始页面没有完整例句，标记 `SOURCE_EXAMPLE_MISSING` 并换词，不得编造。

四个法语选项必须是：

- 1 个在该句具体语境中可以替换目标词的法语近义表达；
- 1 个表达易混英文词中文义的法语词，作为迁移干扰项；
- 2 个词性和句法位置匹配、但语义不成立的法语干扰项。

必须分别验证语义、词性、配价、冠词/性数、时态及放入原句后的语法正确性。仅“词典上近义”但在句中不能替换的答案不合格。

#### 题型 3：判断正误

候选词采用与题型 1 相同的严格语义不重合规则，且与其他三类题全局不重复。

题干必须严格为：`法语目标词 = 易混英文词的中文义`。不得再使用随机其他词义替代英文义，不得为了平衡 V/F 偷换模板。

注意：在“语义完全不重合 + 题干固定使用英文义”这两个条件同时成立时，所有题的正确答案逻辑上都会是 F。先生成一份 `TYPE3_DESIGN_DECISION.md`，明确说明该设计会产生全 F 的反应定势风险，并给出两个方案：

- A：完全忠于用户规则，题型 3 全部为 F；
- B：另设不计入假朋友候选池的真命题控制题，用于平衡反应，但清楚标记为 control，不能伪装成同一选词规则下的题。

未经用户明确批准，不得自行把题型 3 改成混合真假；默认保留 A，并把问卷状态保持为 `REVIEW_REQUIRED`，不得发布。

#### 题型 4：单词拼写

候选词必须来自同一交集词池，但只考虑真实拼写混淆：

1. 法语词与易混英文词的编辑距离为 1–4；同时记录原始字符距离与去重音符号后的距离；
2. 距离 0 或大于 4 的词排除；
3. 排除只因规则性法语词尾、明显派生后缀或语法形态造成的表面接近，例如 `classer/class`；
4. 每个排除项必须保存 `MORPHOLOGY_ONLY`、`DISTANCE_OUT_OF_RANGE` 等理由；
5. 中文题干只采用专四词书前 2–3 个义项；若该中文义可能对应多个常见法语词，必须配置 `acceptableAnswers` 或换成唯一可判定的义项。

题干格式：`中文释义 ______（填写对应法语单词）`。

平台必须实现：第一次错误尝试后显示目标词首字母，但不泄露完整答案；记录错误尝试次数、提示是否显示和最终答案。提示出现前后的反应时需可区分。不得在用户尚未提交本题时把正确答案发送到前端可直接读取的字段中。

### 三、题量和分配原则

1. 不保留旧的 112/111/111/111 配额；先筛选，再决定题量。
2. 四种题型的目标词必须全局唯一，任何词不得跨题型复用。
3. 先生成完整合格候选池，再生成研究问卷抽样集。
4. 默认的问卷 V3 方案为 60 道计分题、四类各 15 题，另保留基本信息与知情说明。若人工审查后的合格池不足，减少题量并报告，不得降标补足。
5. V3 应作为新版本/新试卷生成；不得原地修改已发布 V1，也不得把本地 V2 当作生产已上线版本。
6. 先给出“用四类词汇题替换现有 60 道正式题”对原研究构念、计分维度和与 V1 可比性的影响说明。V3 与 V1 的结果不得直接混合分析，除非建立版本变量和可比性说明。

### 四、必须建立的中间数据模型

为每个交集词生成一条可审计记录，至少包含：

- `wordId`
- `frenchWord`
- `englishConfusable`
- `tem4TopSenses`（最多 3 个）
- `englishChineseSenses`
- `semanticOverlapDecision`: `NONE / PARTIAL / CORE / UNCERTAIN`
- `semanticDecisionReason`
- `tem4PdfPage`
- `falseFriendsPdfPage`
- `tem4SourceQuote`
- `falseFriendsSourceQuote`
- `tem4ExampleSentence`
- `exampleSentenceStatus`
- `rawEditDistance`
- `accentFoldedEditDistance`
- `morphologyOnly`
- `eligibleTypes`
- `assignedType`
- `lexicalReviewStatus`
- `pedagogicReviewStatus`
- `reviewNotes`

语义判定不得由大模型一句话直接覆盖。先用确定性规则做候选过滤，再对 `UNCERTAIN` 和临界项生成审查队列；所有最终入题项必须保留可复核理由。

### 五、实现步骤

1. 新建分支，建议 `codex/ff4-question-bank-v2`。不要覆盖或删除旧文件，把旧题库作为历史基线保存。
2. 重写生成流程，优先使用 Python/Node 的跨平台脚本，避免继续依赖只在 PowerShell 上方便运行的核心流程。
3. 生成新的候选审定文件、题库 Excel、机器可读 JSON 包和审计报告。
4. 生成 `LEXIBRIDGE_RESEARCH_V3.json`，包含 4 个正式题型 section，每类默认 15 题；基本信息题不计入 60 题。
5. 扩展导入预检：检查来源页码、全局目标词唯一、题型资格、选项角色、答案一致性、例句证据、拼写距离、形态排除和人工审查状态。
6. 实现拼写题的逐题尝试/首字母提示机制，包含后端状态、并发与恢复、前端交互、计时和隐私边界。
7. 更新计分和分析维度，使四类题分别可统计；把提示后答对与首次答对区分，不得同分处理而不记录。
8. 为生成器、预检、计分、公开问卷恢复、错误一次显示提示、刷新后提示状态恢复、移动端交互添加测试。
9. 完成本地 lint、typecheck、前端测试、后端测试、构建和种子预检。
10. 只生成生产发布方案，不直接写生产库或替换线上 release。生产操作必须等用户单独明确批准；批准后也必须先备份数据库和源文件，并创建新版本/新 release，保留 V1 回滚路径。

### 六、输出文件

建议输出到新的目录，避免与 V1 混淆：

- `docs/data/lexi-bridge-ff4-v2/candidate-adjudication.xlsx`
- `docs/data/lexi-bridge-ff4-v2/candidate-adjudication.jsonl`
- `docs/data/lexi-bridge-ff4-v2/semantic-review-queue.xlsx`
- `docs/data/lexi-bridge-ff4-v2/TYPE3_DESIGN_DECISION.md`
- `docs/data/lexi-bridge-ff4-v2/法语专四假朋友题库_V2.xlsx`
- `docs/data/lexi-bridge-ff4-v2/question-bank-package-v2.json`
- `docs/data/lexi-bridge-ff4-v2/audit-report.md`
- `app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json`
- `docs/superpowers/plans/lexibridge-research-v3-release-runbook.md`

### 七、验收标准

以下条件必须全部通过；任何一项失败都不得标记为可发布：

- 四个题型之间目标词重复数为 0。
- 题型 1 每题恰有一个正确义、一个英文迁移义、两个合格随机干扰义。
- 题型 2 每题有可定位的专四原句；释义模板数为 0，词头/词性残片数为 0，自造原句数为 0。
- 题型 2 正确近义词在原句中可替换，四个选项的词性和句法位置一致。
- 题型 3 的命题文本与英文中文义一致，不存在随机替换义；设计决策已记录。
- 题型 4 的距离全部为 1–4，形态型伪混淆为 0，答案唯一或明确列出可接受答案。
- 每题均有双源页码、来源摘录、判定理由、词汇审查状态和教研审查状态。
- 未完成词汇人工审查的题不得标记为 `APPROVED`。
- 生成器重复运行结果稳定；固定输入下题目与答案哈希一致。
- 问卷 60 道计分题的 section、题型、答案、计分维度和展示顺序通过自动审计。
- 首次错误后才显示首字母；刷新/恢复后状态一致；前端在提示前无法读取完整正确答案。
- 已发布 V1 数据、release 和既有答卷不被修改。
- 没有私钥、密码、数据库环境变量值或参与者敏感数据进入 Git、日志或交付文件。

### 八、最终汇报格式

最终只汇报可核验结果：

1. 合格候选池总数及各题型可用数；
2. 被排除的主要原因及数量；
3. 实际生成的各题型题量；
4. 人工审查未决数量；
5. 代码和数据文件清单；
6. 测试命令与结果；
7. V1/V2/V3 的版本关系；
8. 是否具备发布条件；若不具备，明确列出阻塞项；
9. 生产发布所需的单独批准事项。

不要使用“结构校验通过”代替“词汇和题目内容正确”。不要在没有原书证据时补写例句、释义、同义词或英文迁移义。

