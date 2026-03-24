| row_no | pair | severity | problem_type | why_problematic | revision_direction |
|---|---|---|---|---|---|
| 7 | budget / budget | minor | unnatural_example | 法语例句里 `ce mois ci` 少了连字符。小错，但生产数据连这种基础拼写都没清干净，说明复核不够硬。 | 统一清洗法语连字符和固定搭配拼写。 |
| 26 | ancient / ancien | major | pair_type_mismatch | `ancient -> ancien` 本来就是常规对应，法语 `ancien` 也能表示“古老的”。把它硬标成 `false_friend`，类型判断直接跑偏。 | 重判类型并重打分；如果坚持做高风险假朋友，这条应删除。 |
| 27 | brave / brave | major | pair_type_mismatch | 法语 `brave` 不是只能表示“善良的”，也保留“勇敢的”义。你把共享义硬删掉再打成高风险假朋友，判断失真。 | 改判为存在共享义的类型，别再伪造极低重合度。 |
| 29 | balance / balance | major | pair_type_mismatch | 英语 `balance` 本身就有“天平；秤”义。把这组词做成 `false_friend` 纯属忽略英语常见义项。 | 重新核定义项覆盖；这一行不能再按假朋友处理。 |
| 30 | confidence / confidence | major | pair_type_mismatch | 英语 `confidence` 也有“秘密吐露；私下告知”义。现在这行把真实共享义抹掉，硬凹成 `false_friend`，不可靠。 | 重判类型并重算分数，不要拿不完整词义表当依据。 |
| 32 | cry / crier | major | pair_type_mismatch | 英语 `cry` 不只表示“哭”，也常表示“大声喊叫；呼喊”。这组词不是你写的那种零重合假朋友。 | 改判为存在明显交叉义的类型，重写说明。 |
| 38 | experience / expérimenter | major | low_confidence_entry | 这不是稳定的课堂型“形似词”配对，而是把英语 `experience` 和法语动词 `expérimenter` 生拉硬拽到一起。关系不稳，像凑数。 | 直接删条，换成真正有稳定混淆证据的词对。 |
| 39 | familiar / familier | major | pair_type_mismatch | 法语 `familier` 明明保留“熟悉的；亲近的”一侧义项。现在这行把它粗暴压成 `false_friend`，误导教学判断。 | 改判为有共享义的类型，并下调风险。 |
| 40 | gentle / gentil | major | low_confidence_entry | 这组词形接近度本来就偏弱，语义上又都有“温和；友善”一带的重叠。拿它做高风险假朋友，可信度很差。 | 删条或换成更稳的相似词对，不要拿边界条目充数。 |
| 41 | habit / habit | major | pair_type_mismatch | 英语 `habit` 也能表示特定服装，尤其是宗教服。你把共享义完全抹掉再给出 `0.06 / 0.96`，分型和打分都失真。 | 重判类型并重打分；这条不能继续按纯假朋友入库。 |
| 43 | librarian / libraire | major | duplicate_or_near_duplicate | 基准库里已经有 `library / librairie`。这条只是同一误判家族的派生扩写，新增信息密度很低，近重复味太重。 | 收缩同家族派生扩写，优先换成新的混淆家族。 |
| 52 | competition / compétition | critical | pair_type_mismatch | 英语 `competition` 直接就有“比赛；竞赛”义，和法语 `compétition` 的共享义非常扎实。把它标成 `false_friend`，属于基础词义判断错误。 | 直接退回重做；这一行当前状态不配入库。 |
| 54 | manifestation / manifestation | major | pair_type_mismatch | 两语都稳定有“表现；显现”义。现在这行故意只抓法语“示威”去硬造假朋友，对学生是错误输入。 | 改判类型并重新组织义项，不要用删义法制造差异。 |
| 55 | agenda / agenda | minor | unnatural_example | 法语例句写成了 `rendez vous`，标准写法是 `rendez-vous`。这种低级格式错居然没被拦下来。 | 清洗法语固定搭配拼写，别把半成品往库里塞。 |
| 56 | conception / conception | critical | pair_type_mismatch | 英语 `conception` 和法语 `conception` 共享“构思；形成”义很稳，这行却硬标 `false_friend`，类型判断失真得很明显。 | 整行退回重判；当前类型和分数都不可信。 |
| 56 | conception / conception | critical | unnatural_example | 法语例句 `La conception date du printemps.` 根本没把词义钉死到“受孕”，照样能读成“项目构思形成于春天”。例句直接把整行教坏了。 | 先把义项判准，再重写能唯一锁定义项的例句。 |
| 60 | command / commande | major | low_confidence_entry | 备注里所谓“共享控制感”完全是空话，不是稳定义项关系。`command / commande` 被硬扔进 `partial_cognate`，证据太虚。 | 删除这类靠抽象联想成立的条目。 |
| 63 | edition / édition | major | weak_pedagogical_value | 这是接近直通的透明同源词。你为了凑 `partial_cognate`，硬说“出版色彩更强”，教学收益非常薄。 | 删掉这种强行造差异的低收益条目。 |
| 64 | emotion / émotion | major | weak_pedagogical_value | `emotion / émotion` 的共享义非常强，拿“情绪”对“激动”来硬抠差别，像在做词典细枝末节，不像在做教学词库。 | 删除或换成差异真正稳定、可教的条目。 |
| 65 | execution / exécution | major | pair_type_mismatch | 英语 `execution` 同样有“执行”和“处决”两侧义。你把共享义抹掉再打成 `partial_cognate`，分型不成立。 | 重判类型；这条不能按当前标签继续存在。 |
| 68 | instruction / instruction | minor | unnatural_example | `Read the instruction before the test.` 这句英语很别扭，正常教学语境下更自然的是 `instructions`。例句质量不达标。 | 重写英例句，不要用非母语句子充版面。 |
| 69 | legend / légende | major | pair_type_mismatch | 英语 `legend` 也有“图例；说明文字”义，不是只有“传说”。这条把真实共享义删掉后再制造差异，做法很糟。 | 重新核定义项；别再靠删共享义来伪造 `partial_cognate`。 |
| 71 | occasion / occasion | major | unnatural_example | 法语例句 `Cette voiture est une occasion intéressante.` 并没有把词义稳定锁到“二手货”，更像“不错的机会/划算的买卖”。例句支撑失败。 | 先定清楚目标法语义项，再写不歧义的例句。 |
| 72 | operation / opération | major | pair_type_mismatch | 英语 `operation` 本来就同时有“操作；运作”和“手术”义，这组词共享面很宽。拿它做 `partial_cognate` 过于牵强。 | 重判类型；这条当前标签说不通。 |
| 72 | operation / opération | major | unnatural_example | `The washing machine operation is simple.` 是明显的硬译英语，母语者不会这么说。例句又僵又假。 | 重写英语例句，别用机器味句子污染语料。 |
| 73 | passage / passage | major | pair_type_mismatch | 英语 `passage` 自己就有“通道； passageway”和“段落”两侧义。这行把共享义割裂后再标 `partial_cognate`，逻辑站不住。 | 重判类型并重算分数。 |
| 78 | sensation / sensation | major | weak_pedagogical_value | `sensation / sensation` 共享义太强，英语里也能说媒体“造成轰动”。靠边缘用法制造差别，教学价值很低。 | 删除这种差异过薄的条目。 |
| 79 | volume / volume | critical | pair_type_mismatch | 你列出的三组核心义“音量；册；体积”在英法里都直接共享。这已经不是 `partial_cognate`，而是基本同源对应。当前标注严重失真。 | 直接退回重做；这行现状不具备入库资格。 |
| 80 | relation / relation | major | weak_pedagogical_value | 这条靠英语里偏书面、偏低频的 `relation` 在硬撑，例句 `Trust is central to any relation.` 也很僵。教学收益低，输入质量也差。 | 删除这类靠生硬例句维持的条目。 |
| 81 | medicine / médecine | major | pair_type_mismatch | 英语 `medicine` 自己就有“医学”义，`study medicine` 是基础表达。现在这行把它压成低重合 `partial_cognate`，分型明显失真。 | 重判类型并重算分数，不要继续拿错误标签入库。 |

总评：

- 生产可导入标准：勉强达到。21 列结构、枚举、数值范围、固定字段、裸英文逗号、与 `semantic-lexicon-v2-import-ready-80.csv` 的精确重复，这些机器层面都过了。
- 生产可教学标准：未达到。核心问题不是格式，而是一批词对类型判错、共享义被故意删掉、例句没把目标义项钉死。
- 是否建议直接入库：不建议。至少第 26、27、29、30、52、56、79、81 行这类基础判断错误不处理，这份文件就不该进生产库。
