# 数据导入与使用指南

这份指南只解决一个问题：

导入的数据到底从哪里进、导入后会落到哪里、以及怎样继续接到诊断、训练和 RAG 链路里。

如果你要看当前线上环境到底已经导入了哪一份词库，请先看：

- [词库导入生产状态（2026-04-25）](/mnt/d/huashi2/docs/lexical-import-production-status-2026-04-25.md)

## 1. 先理解项目里的“导入”和“使用”

本项目里的批量导入，导入的是“词对知识库”本身，不是直接给学生做题的数据包。

真实关系是：

1. 先把词对、义项、例句导入到主词库。
2. 再由教师或管理员在诊断模板、词表等业务配置里引用这些 `lexicalPairId`。
3. 学生完成诊断后，系统才会基于这些词对生成训练计划、错题本和复习计划。
4. AI / RAG 检索会消费词对、义项、例句这套知识数据。

结论：

- 导入成功后，数据会先出现在“词对管理 / 语料库管理”里。
- 它不会自动出现在学生端题目里。
- 如果要进入学生主链路，还需要继续做模板或词表配置。

## 2. 先把本地环境启动到可登录状态

如果你只是想先跑通导入流程，最省事的方式是打开 demo 账号。

### 2.1 打开 demo 账号

编辑 `deploy/.env`：

```env
APP_DEMO_DATA_ENABLED=true
```

只有这个开关为 `true` 时，默认测试账号才会存在：

- 管理员：`admin` / `Admin@123456`
- 教师：`teacher.zhang` / `Teacher@123456`
- 学生：`student.li` / `Student@123456`
- 学生：`student.wang` / `Student@123456`

### 2.2 启动依赖

```bash
cd deploy
docker compose --env-file .env up -d mysql redis rabbitmq postgres
```

### 2.3 启动后端

仓库要求：

- JDK `25`
- Node.js `20+`

启动命令：

```bash
./mvnw -pl ai-gateway -am spring-boot:run
./mvnw -pl app-server -am spring-boot:run
```

### 2.4 启动前端

```bash
npm install
npm run dev
```

默认访问地址：

- 前端：`http://localhost:3000`
- 后端 API：`http://localhost:8080`

## 3. 从哪里进入导入页面

导入入口不在独立菜单里，而是在词对工作区内部。

- 教师账号进入：`/teacher/lexical-pairs`
- 管理员账号进入：`/admin/lexical-pairs`

页面里会同时看到：

- 下载模板
- 词对列表
- 批量导入中心
- 词对编辑器

## 4. 导入文件怎么准备

### 4.1 支持格式

- `CSV`
- `XLSX`

限制：

- 单文件上限 `50MB`
- `XLSX` 只读取第一个 sheet
- 第一行必须是表头

### 4.2 最小必填列

下面 8 列是必须的：

- `english_word`
- `french_word`
- `chinese_gloss`
- `lexical_pair_type`
- `semantic_overlap_score`
- `false_friend_risk`
- `default_context_support`
- `difficulty_level`

### 4.3 推荐值格式

为了最稳妥，文件里直接使用模板示例里的小写值：

- `lexical_pair_type`: `cognate` / `false_friend` / `partial_cognate` / `orthographic_similar`
- `default_context_support`: `low` / `medium` / `high`
- `knowledge_status`: `draft` / `ready` / `disabled`
- `embedding_status`: `pending` / `embedded` / `failed`
- `active`: `true` / `false`

### 4.4 一份最小可用 CSV

```csv
english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level
coin,coin,硬币；角落,false_friend,0.10,0.92,high,4
```

### 4.5 例句和义项的限制

批量导入阶段只支持“一行一个词对 + 一个主义项 + 一组主例句”。

如果你填了下面任意例句列：

- `example_english`
- `example_french`
- `example_chinese`

那就必须同时填写对应义项列：

- `sense_english_definition`
- `sense_french_definition`
- `sense_chinese_definition`

## 5. 真正的导入步骤

在“词对管理 / 语料库管理”页面里按这个顺序走：

1. 点击“下载模板”。
2. 按模板准备 `CSV` 或 `XLSX`。
3. 在“批量导入中心”里选择文件。
4. 点击“创建导入批次”。
5. 等待后台解析完成。
6. 在“导入历史”里选中该批次。
7. 检查统计信息：
   - `可导入`
   - `需修正`
   - `已跳过`
   - `已导入`
8. 如果有无效行，进入右侧逐行修正或勾选“跳过该行”。
9. 点击“正式导入可用行”。
10. 导入完成后回到词对列表抽样检查。
11. 再看批次详情里的“知识同步概览”：
   - `待嵌入` 说明主库已写入，但知识嵌入还在后台执行
   - `已嵌入` 说明这批词条已经进入检索知识库
   - `嵌入失败` 说明需要回到词对总览或重建任务里继续排查
   - `最近成功嵌入` 可用来确认这批词条最近一次真正进入知识库的时间
12. 如果你是管理员，且这批词条仍处于 `待嵌入 / 嵌入失败`，可直接点击 `重建本批索引`
   - 页面会显示最近一次定向任务的 `jobId`
   - 任务完成后，批次详情会自动刷新知识同步摘要

状态含义：

- `PARSING`：后台解析文件中
- `DRAFT`：已生成草稿，允许人工修正
- `IMPORTING`：正在正式导入
- `COMPLETED`：批次处理完成
- `FAILED`：批次失败，需要看错误信息

## 6. 导入后怎样真正“用起来”

这是最容易误解的地方。

### 6.1 导入完成后，先会进入哪里

它会先进入词对主库。

你可以马上做的事：

- 在词对列表里搜索、筛选、编辑
- 补充义项、例句、标签、备注
- 导出 CSV

### 6.2 怎样进入诊断链路

去“诊断模板”页面，把新词对的 `lexicalPairId` 配进模板 item。

页面路径：

- `/teacher/diagnosis-templates`

当前实现里，模板项直接引用 `lexicalPairId`，不会自动从“刚导入的词”里生成题。

### 6.3 怎样进入词表链路

去“词表管理”页面，把这些 `lexicalPairId` 加进词表。

页面路径：

- `/teacher/lexical-lists`

当前页面仍然是通过输入 `lexicalPairId` 的方式加词，不是图形化点选。

### 6.4 怎样进入训练、错题本、复习计划

这些不是手工直接导入的，而是学生完成诊断、训练之后，系统基于词对结果继续生成：

- training plan
- wrong book
- review schedule

所以顺序应该理解成：

`导入词对 -> 配诊断模板 / 词表 -> 学生作答 -> 训练与复习链路继续生成`

### 6.5 怎样进入 RAG / AI 检索

词对、义项、例句会进入知识同步链路，供 AI / RAG 检索使用。

但本地联调时仍建议这样判断：

- 如果导入后 RAG 已能检索到新词条，说明知识同步正常。
- 如果导入后 RAG 还检索不到，去管理员配置中心手动执行一次 `RAG Reindex`。

管理员页面：

- `/admin/config-center`

## 7. 常见问题

### 7.1 为什么导入成功了，学生端还是看不到

因为导入只是进入词库，不会自动变成诊断题或训练题。

你还需要：

- 配诊断模板
- 或把词对加入词表

### 7.2 为什么词表页面让我填 `lexicalPairId`

因为当前实现就是通过 `lexicalPairId` 建立引用关系。

建议做法：

- 先在词对管理里确认该词已经导入成功
- 记下对应的 `Pair #id`
- 再去词表或模板页面引用

### 7.3 为什么导入后 RAG 还搜不到

优先排查：

1. `app-server`、`ai-gateway`、`rabbitmq` 是否都已启动
2. `PLATFORM_INTERNAL_API_TOKEN` 是否一致
3. 管理员配置中心里健康检查是否正常
4. 需要时手动做一次 `INCREMENTAL` reindex

### 7.4 哪些字段最容易填错

- 列名和模板首行不完全一致
- `semantic_overlap_score`、`false_friend_risk` 不在 `0-1`
- `difficulty_level` 不是 `1-5`
- 枚举值写成中文或自由文本
- 填了例句但没填义项
- 同一个文件里出现重复词对

## 8. 一条最短上手路径

如果你只想先跑通一次，按这个最短路径做：

1. 把 `APP_DEMO_DATA_ENABLED=true`
2. 启动 `mysql redis rabbitmq postgres`
3. 启动 `ai-gateway` 和 `app-server`
4. 启动前端并访问 `http://localhost:3000`
5. 用 `teacher.zhang / Teacher@123456` 登录
6. 进入“词对管理”
7. 下载模板并导入一条最小 CSV
8. 在词对列表里确认导入成功
9. 去“诊断模板”页面，用该词对的 `lexicalPairId` 建一个最简单模板
10. 再切学生账号走诊断/训练链路
