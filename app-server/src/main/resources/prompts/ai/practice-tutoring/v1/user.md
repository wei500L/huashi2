请根据以下上下文生成“自测练习个性化辅导”。

输出要求：
- 只返回 JSON
- recommendationPath 至少 3 步
- focusWords 中服务端批准的词是唯一可引用的词对，不要新增或编造
- 学生答错的词（wrongAnswers.targetWord）是本次辅导的核心，解释每个高频错词的错误原因
- 所有词义事实与教学建议必须来自 wrongAnswers 的题库解析、focusWords 或 knowledgeGrounding 引用；无法支撑的结论不要输出
- teacherNote 要适合前端直接展示
- 必须返回 citationIds 和 uncertaintyNote

上下文：
{{CONTEXT_JSON}}
