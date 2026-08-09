param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$candidatePath = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4\tem4-candidates.jsonl'

# These records were transcribed from the rendered TEM4 pages during the GPT
# visual pass. The source PDF page and page-level checkpoint are retained in
# the candidate record; this map only repairs the previously sparse fields.
$transcriptions = @"
chagrin|n.m.|[ʃagrɛ̃]|悲伤；忧愁
code|n.m.|[kɔd]|密码；代号；法典
coffre|n.m.|[kɔfr]|箱子；保险柜；汽车后备箱
commissaire|n.m.|[kɔmisɛr]|专员；警务官
commode|a.|[kɔmɔd]|方便的；舒适的
condamner|v.t.|[kɔdane]|判处；谴责
confondre|v.t.|[kɔ̃fɔ̃dr]|混淆；使困惑
contact|n.m.|[kɔtakt]|联系；接触
cours|n.m.|[kur]|课程；流动；过程
digestif,ve|a.|[diʒɛstif, v]|助消化的
doter|v.t.|[dɔte]|装备；配备；授予
draguer|v.t.|[drage]|疏浚；搭讪
dresser|v.t.|[drɛse]|竖起；制定；训练
engager|v.t.|[ɑ̃gaʒe]|雇用；使参与；入伍
envie|n.f.|[ɑ̃vi]|欲望；愿望
exact,e|a.|[ɛgzakt]|准确的；准时的
expertise|n.f.|[ɛkspɛrtiz]|鉴定；评估
exploit|n.m.|[ɛksplwa]|功绩；壮举
exposer|v.t.|[ɛkspoze]|陈列；展示；暴露
express,e|a.|[ɛkspre,s]|特快的；快速的
fameux,se|a.|[famø]|著名的；极好的
fasciner|v.t.|[fasine]|使着迷；吸引
fixer|v.t.|[fikse]|固定；注视；确定
flamme|n.f.|[flam]|火焰；爱情；激情
formule|n.f.|[fɔrmyl]|公式；格式；措辞
futur,e|a.; n.m.|[fyt yr]|未来的；未来
garder|v.t.|[garde]|保存；看守；保留
gaz|n.m.|[gaz]|气体；煤气
gorge|n.f.|[gɔrʒ]|喉咙；峡谷
humeur|n.f.|[ymœr]|心情；脾气
ignorer|v.t.|[iɲɔre]|不知道；忽视
impressionnant,e|a.|[ɛprɛsjɔnɑ̃]|给人深刻印象的；巨大的
information|n.f.|[ɛ̃fɔrmasjɔ̃]|消息；情报；资料
inutile|a.|[inytil]|无用的；徒劳的
lecteur,trice|n.m.; n.f.|[lɛktœr]|读者；朗读者
ligne|n.f.|[liɲ]|线；路线；行
manteau|n.m.|[mɑ̃to]|大衣；外套
marché|n.m.|[marʃe]|市场；交易；行情
marquer|v.t.|[marke]|标记；留下印象
massif,ve|a.; n.m.|[masif, v]|巨大的；结实的；山块
mine|n.f.|[min]|矿井；神情；铅笔芯
mobile|a.|[mɔbil]|活动的；可移动的
office|n.m.|[ɔfis]|办事处；职务；礼拜
oignon|n.m.|[ɔɲɔ̃]|洋葱
ornement|n.m.|[ɔrnəmɑ̃]|装饰；装饰品
particulier,ère|a.; n.m.|[partikylje, ɛr]|特别的；个人的；私人
partie|n.f.|[parti]|部分；一局；一方
passer|v.i.; v.t.|[pase]|经过；通过；度过
patient,e|a.; n.m.|[pasjɑ̃, t]|耐心的；病人
pension|n.f.|[pɑ̃sjɔ̃]|膳宿；小旅馆；寄宿学校
placard|n.m.|[plakar]|壁橱；海报
poser|v.t.|[poze]|放置；提出；摆姿势
posséder|v.t.|[pɔsede]|拥有；精通
pratiquer|v.t.|[pratike]|实行；从事；练习
projeter|v.t.|[prɔʒ(ə)te]|投掷；放映；计划
provoquer|v.t.|[prɔvoke]|引起；导致；激起
pur,e|a.|[pyr]|纯的；纯粹的
quai|n.m.|[ke]|码头；站台
quart|n.m.|[kar]|四分之一；一刻钟
raisin|n.m.|[rɛzɛ̃]|葡萄；葡萄干
rallier|v.t.|[ralje]|集合；归队；靠拢
rapport|n.m.|[rapɔr]|关系；报告；比例
raquette|n.f.|[rakɛt]|球拍；雪鞋
ravi,e|a.|[ravi]|欣喜的；高兴的
recommander|v.t.|[rəkɔmɑ̃de]|推荐；叮嘱
record|n.m.|[rəkɔr]|纪录
relation|n.f.|[rəlɑsjɔ̃]|关系；叙述
relief|n.m.|[rəljɛf]|浮雕；缓解；突出
remarquer|v.t.|[rəmɑrke]|注意到；察觉
restaurer|v.t.|[rɛstore]|修复；恢复
revenu|n.m.|[rəvny]|收入
rivière|n.f.|[rivjɛr]|河流
rude|a.|[ryd]|粗糙的；严酷的；粗鲁的
sauce|n.f.|[sos]|调味汁；肉汁
sauver|v.t.|[sove]|救；保存
scandale|n.m.|[skɑ̃dal]|丑闻；令人震惊的事
secteur|n.m.|[sɛktœr]|部门；区域；行业
signaler|v.t.|[siɲale]|标示；报告；指出
ski|n.m.|[ski]|滑雪；滑雪板
solde|n.m.|[sɔld]|余额；清仓存货；军饷
solliciter|v.t.|[sɔlisite]|请求；申请；争取
supplier|v.t.|[syplije]|恳求；请求
syndicat|n.m.|[sɛ̃dika]|工会；行业协会
temple|n.m.|[tɑ̃pl]|庙宇；教堂
tendance|n.f.|[tɑ̃dɑ̃s]|趋势；倾向
terme|n.m.|[tɛrm]|术语；期限；界限
tirer|v.t.|[tire]|拉；抽取；射击
trousse|n.f.|[trus]|包；盒；工具包
type|n.m.|[tip]|类型；家伙
vapeur|n.f.|[vapœr]|蒸汽；水汽
virtuel,le|a.|[virtɥɛl]|虚拟的；可能的
"@
$transcriptions = $transcriptions -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

$byNormalized = @{}
foreach ($line in $transcriptions) {
    $parts = $line -split '\|', 4
    if ($parts.Count -ne 4) { throw "Invalid transcription: $line" }
    $normalized = $parts[0].Split(',')[0].Trim().ToLowerInvariant()
    $byNormalized[$normalized] = [ordered]@{
        part_of_speech = $parts[1]
        ipa = $parts[2]
        chinese_core_senses = @($parts[3] -split '；' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
}

$changed = 0
$rows = Get-Content -LiteralPath $candidatePath -Encoding utf8 | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object {
    $record = $_ | ConvertFrom-Json
    if ($record.source_code -eq 'TEM4_CORE_VOCABULARY' -and $byNormalized.ContainsKey([string]$record.normalized_headword)) {
        $source = $byNormalized[[string]$record.normalized_headword]
        if ([string]::IsNullOrWhiteSpace([string]$record.part_of_speech)) { $record.part_of_speech = $source.part_of_speech }
        if ([string]::IsNullOrWhiteSpace([string]$record.ipa)) { $record.ipa = $source.ipa }
        if (@($record.chinese_core_senses).Count -eq 0) { $record.chinese_core_senses = $source.chinese_core_senses }
        $record.source_type = 'visual_verified_headword'
        $record.review_state = 'VERIFIED'
        $record.missing_fields = @($record.missing_fields | Where-Object { $_ -ne 'Candidate enrichment deferred to source-specific final intersection review.' })
        $changed++
    }
    $record | ConvertTo-Json -Compress -Depth 8
}
$rows | Set-Content -LiteralPath $candidatePath -Encoding utf8
"enriched_candidate_records=$changed; visual_headwords=$($byNormalized.Count)"
