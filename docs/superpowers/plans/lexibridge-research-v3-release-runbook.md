# Lexi-Bridge 研究问卷 V3 发布 Runbook（仅本地生成，未发布）

> **已废弃（2026-08-12）**：V3 题库不再作为研究问卷发布。`LEXIBRIDGE_FF4_V2` 题库改由 `LexiBridgePracticeBankSeedInitializer` 只种题库，
> 供学生自测练习模块（`/practice`）使用；研究问卷保持 V1 唯一。以下内容仅作为历史记录保留。

> 文档编号：LEXIBRIDGE_RESEARCH_V3_RELEASE_RUNBOOK
> 版本：2026-08-12（分支 `codex/ff4-question-bank-v2`）
> 状态：**CONTENT_READY / NOT_DEPLOYED** —— 题库内容已按 0811 文档规则生成并通过本地审计；生产发布仍须完成备份、完整环境验证、代码评审和单独发布批准。

---

## 1. 版本关系与可比性说明（V1 / V2 / V3）

| 维度 | V1（已发布） | V2（本地实现，已回滚，非生产） | V3（本次生成，APPROVED 内容） |
|------|--------------|-------------------------------|--------------------------------|
| 题库来源 | DOCX 问卷 + 分析文档 | V1 的包装改动 | FF4 V2 四类题题库（双书证据驱动生成） |
| 正式题结构 | Partie 1A/1B/2/3/4 共 60 题，按语境层级（WORD/PHRASE/SENTENCE/CLOZE/READING） | 同 V1，仅拆 P1B 与加粗强调 | 4 个 section：词义单选 27 / 句子选词 16 / 判断正误 22 / 拼写 174（共 239 计分题） |
| 构念维度 | LEXICAL_TRANSFER / CONTEXT_REPAIR 等 + 语境层级 | 同 V1 | FF4_WORD_MEANING / FF4_SENTENCE_SYNONYM / FF4_TRUE_FALSE_TRANSFER / FF4_SPELLING |
| 计分版本 | SCORING_V1 | SCORING_V1 | SCORING_V3 |
| 状态 | 生产已发布 release，**不可修改** | 已 revert，仅 target/classes 残留构建产物 | 内容 APPROVED，尚未创建 release |

### V1 与 V3 结果不可直接混合分析

1. **构念不同**：V1 的 60 题按"词汇→短语→句子→完型→阅读"的语境梯度组织；V3 按"词义判断→语境同义→真假迁移→拼写"四种任务组织。两者测的不是同一套维度，直接合并会混淆"任务类型"与"能力"。
2. **计分脚本不同**：SCORING_V1 与 SCORING_V3 的维度聚合口径不同（见 `AssessmentScoringV1` 与 `AssessmentScoringV3`），虽然都输出百分比，但分母、分维度不可直接相减。
3. **材料不同**：V1 题目来自教师编写 DOCX；V3 来自双书 OCR、确定性筛选与概念级生产规则复核。即便题目表面相似，来源和审定口径也不同。
4. **正确做法**：若研究需要纵向比较，必须建立**版本变量（questionnaireVersion）**，在模型中加入版本固定效应，并分别报告 V1/V3 的信度与构念效度，先做 measurement invariance 检验再谈合并；在此之前，V3 与 V1 结果**只可并列展示，不可混合计算**。

---

## 2. 当前状态快照（生成时）

- 合格候选池以 `production_semantic_rules.py` 的概念级批准词表为唯一成品来源；题型 2 固定使用证据完备的 `remarquer`（原句、同义词 `repérer`、英语迁移义对应词 `dire` 均可定位）。
- 题量：T1=27，T2=16，T3=22，T4=174（共 239 计分题），V3 直接使用完整生产批准题库，不再抽样缩减。
- 全部 239 题：`LEXICAL_REVIEW_STATUS=APPROVED` + `PEDAGOGIC_REVIEW_STATUS=APPROVED`。
- 覆盖状态：`MAXIMUM_RULE_COMPLIANT_COVERAGE`；生产审计强制校验“语义批准池 ∪ 拼写合格池 ∪ 同源控制池”全部进入题库且目标词不重复，任何漏词均使审计失败。
- 题型 2 按证据分级扩充为 5 道 TEM4 完整原句 + 11 道 TEM4 原搭配最小语境题；题型 3 为 12 道假朋友 F + 10 道同形同义 V，控制题独立标记为 `COGNATE` 并穿插展示。

---

## 3. 发布前置条件（全部满足才可进入发布流程）

- [x] **内容规则收口**：成品题库只使用 `production_semantic_rules.py` 中通过概念级复核的词；原始候选表中的 `PENDING` 仅保留为筛选过程证据，不传播到成品。
- [x] **题目结构审计**：选项角色、答案唯一性、跨题型目标词去重、拼写距离与形态排除均由生成器和审计脚本强制校验。
- [x] **题型三设计决策**：执行客户最新反馈，在假朋友 F 题中加入 10 道同源词 Vrai 控制题，降低全 F 反应定势。
- [x] **预检全绿**：`python3 scripts/lexi-bridge-ff4-v2/audit_package.py` 与 `preflight_v3_seed.py` 均输出 issueCount=0；
- [x] **交付固化**：`production-release-manifest.json` 已记录规则版本、题量、状态及所有生产交付文件 SHA-256；
- [ ] **测试环境可用**：Docker（Testcontainers）、Redis、RabbitMQ、ai-gateway(8090) 可用，`./mvnw -pl app-server -am test` 全绿；
- [ ] **数据库与源文件备份**：备份生产 MySQL 相关表与 `app-server/src/main/resources/assessment-seeds/` 全部文件；
- [ ] **代码审查**：本分支 PR 评审通过。

---

## 4. 发布步骤（生产操作，需用户单独批准后执行）

1. **备份**（必须先做，任何一步之前）：
   ```bash
   # 数据库：至少备份 assessment_question_bank / assessment_question_bank_import /
   # assessment_questionnaire* / assessment_paper / assessment_question / assessment_question_version
   mysqldump -u<user> -p <db> assessment_question_bank assessment_question_bank_import \
     assessment_questionnaire assessment_questionnaire_version assessment_questionnaire_section \
     assessment_questionnaire_item assessment_paper assessment_question assessment_question_version \
     assessment_content_review_issue > /backup/lexibridge-v1-before-v3-$(date +%Y%m%d%H%M%S).sql
   ```
2. **交付文件校验**：运行 `python3 scripts/lexi-bridge-ff4-v2/verify_release_manifest.py`，确认所有 XLSX、JSON、种子与审计报告 SHA-256 匹配且 `status=PASS`。
3. **Schema 变更**：部署包含 `assessment_attempt_answer` 新增两列（`spelling_wrong_attempt_count`、`spelling_hint_shown_at`）的版本。开发期按 CLAUDE.md 约定直接更新 `schema.sql`（本次已更新；生产库需在停机窗口执行等价 ALTER）。
4. **后端部署**：部署 shared-kernel + app-server 新版本（含 SPELLING 题型、V3 种子、SCORING_V3、拼写提示接口）。
5. **V3 种子加载**：生产环境设置 `APP_ASSESSMENT_LEXIBRIDGE_V3_SEED_ENABLED=true` 启动一次，生成内容状态为 APPROVED、试卷状态为 DRAFT 的 `LEXIBRIDGE_RESEARCH_V3` 问卷。
6. **导入核验**：确认导入记录无 OPEN/REJECTED review issue，questionnaire/version 保持种子中的 APPROVED；试卷仍为 DRAFT，不自动发布。
7. **发布**：创建新 release（`LEXIBRIDGE_RESEARCH_V3`），生成参与码，**保留 V1 release 不触碰**。
8. **回滚路径**：V1 release 及其数据、种子文件、代码路径均未改动；若 V3 出问题，直接停用 V3 release、恢复备份即可回到 V1 现状。

---

## 5. 禁止事项

- 不得绕过 `production_semantic_rules.py` 将原始候选表中的待定词直接写入 APPROVED 成品。
- 不得添加未在 `TRUE_COGNATE_CONTROLS` 中登记、无双书页码或未标记 `COGNATE` 的 Vrai 控制题。
- 不得修改已发布 V1 的种子文件、release、既有答卷。
- 不得把本地 V2 种子当作生产版本。
- 不得把私钥、密码、数据库环境变量值、参与者敏感数据写入 Git、日志或交付文件。
