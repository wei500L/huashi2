param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$dataDirectory = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4'
$wordbookPath = Join-Path $dataDirectory 'wordbook.jsonl'
$packagePath = Join-Path $dataDirectory 'question-bank-package.json'
$reportPath = Join-Path $dataDirectory 'review-report.json'
$fallbackPath = Join-Path $dataDirectory 'true-false-source-equality-fallbacks.json'
$falseFriendsPath = Join-Path $dataDirectory 'false-friends-candidates.jsonl'
$jsonPath = Join-Path $dataDirectory 'pedagogic-review-queue.json'
$csvPath = Join-Path $dataDirectory 'pedagogic-review-queue.csv'
$markdownPath = Join-Path $dataDirectory 'pedagogic-review-queue.md'

foreach ($requiredPath in @($wordbookPath, $packagePath, $reportPath, $fallbackPath, $falseFriendsPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) { throw "Missing required review input: $requiredPath" }
}

$report = Get-Content -LiteralPath $reportPath -Raw -Encoding utf8 | ConvertFrom-Json
if ($report.auditStatus -ne 'STRUCTURAL_PASS' -or $report.issueCount -ne 0) {
    throw 'A pedagogic review queue requires a zero-issue structural package audit.'
}

function Normalize-Meaning([string]$value) { return (($value -replace '[\s;,\.\(\)\uFF1B\uFF0C\u3001\u3002\uFF08\uFF09\u2014\-]', '').ToLowerInvariant()) }

$words = @(Get-Content -LiteralPath $wordbookPath -Encoding utf8 | Where-Object { $_ } | ForEach-Object { $_ | ConvertFrom-Json })
$package = Get-Content -LiteralPath $packagePath -Raw -Encoding utf8 | ConvertFrom-Json
$fallbacksRaw = Get-Content -LiteralPath $fallbackPath -Raw -Encoding utf8 | ConvertFrom-Json
$fallbacks = @($fallbacksRaw | ForEach-Object { $_ })
$falseFriendCandidates = @(Get-Content -LiteralPath $falseFriendsPath -Encoding utf8 | Where-Object { $_ } | ForEach-Object { $_ | ConvertFrom-Json } | Where-Object { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' })
$wordById = @{}
foreach ($word in $words) { $wordById[$word.word_id] = $word }
$fallbackByItemCode = @{}
foreach ($fallback in $fallbacks) { $fallbackByItemCode[$fallback.itemCode] = $fallback }

function Get-FalseFriendEvidence([object]$Word) {
    $matches = @($falseFriendCandidates | Where-Object { $_.normalized_headword -eq $Word.french_word.ToLowerInvariant() -and $_.pdf_page -eq $Word.false_friend_pdf_page })
    if ($matches.Count -eq 0) { return '' }
    return [string]$matches[0].page_evidence
}

$queue = [System.Collections.Generic.List[object]]::new()
foreach ($item in @($package.Items)) {
    $word = $wordById[$item.targetWord]
    if ($null -eq $word) { throw "Queue source word missing for item: $($item.itemCode)" }
    $flags = [System.Collections.Generic.List[string]]::new()
    $reasons = [System.Collections.Generic.List[string]]::new()
    $score = 0
    $originalSourceClaim = ''
    $appliedFalseClaim = ''
    $sourceEvidenceNote = ''

    if ($fallbackByItemCode.ContainsKey($item.itemCode)) {
        $fallback = $fallbackByItemCode[$item.itemCode]
        $originalSourceClaim = [string]$fallback.originalClaim
        $appliedFalseClaim = [string]$fallback.replacementClaim
        $flags.Add('TRUE_FALSE_TEMPLATE_FALLBACK')
        $reasons.Add('The source confusable claim shared a verified French meaning, so the generated false claim was replaced. Confirm the item still measures the intended construct.')
        $score += 4
    }
    if ((Normalize-Meaning $word.chinese_gloss) -eq (Normalize-Meaning $word.false_friend_meanings)) {
        $flags.Add('SOURCE_MEANINGS_EQUAL')
        $reasons.Add('The two source-derived meaning fields are identical after punctuation normalization. Confirm whether this record belongs in a false-friends instrument.')
        $score += 4
    }
    $glossSenseCount = @($word.chinese_gloss -split ';|,|\uFF1B|\uFF0C|\u3001' | Where-Object { $_.Trim() }).Count
    if ($glossSenseCount -ge 5) {
        $flags.Add('HIGH_POLYSEMY')
        $reasons.Add("The French gloss lists $glossSenseCount senses; verify that the question and distractors test a bounded intended sense.")
        $score += 2
    }
    if (($item.questionType -eq 'FILL_BLANK' -or $item.questionType -eq 'SHORT_TEXT') -and $word.french_word.Length -ge 10) {
        $flags.Add('LONG_EXACT_INPUT')
        $reasons.Add('The generated response requires an exact long French spelling; verify acceptance and normalization policy before release.')
        $score += 1
    }

    if ($flags.Count -gt 0) { $sourceEvidenceNote = Get-FalseFriendEvidence $word }

    $priority = if ($score -ge 4) { 'CRITICAL' } elseif ($score -ge 2) { 'HIGH' } elseif ($score -ge 1) { 'MEDIUM' } else { 'NORMAL' }
    if ($reasons.Count -eq 0) { $reasons.Add('Baseline source-backed manual pedagogic review is still required.') }
    $queue.Add([pscustomobject]@{
        priority = $priority
        risk_score = $score
        item_code = $item.itemCode
        question_type = $item.questionType
        word_id = $word.word_id
        french_word = $word.french_word
        chinese_gloss = $word.chinese_gloss
        source_flags = @($flags)
        false_friends_page_evidence = $sourceEvidenceNote
        original_source_claim = $originalSourceClaim
        applied_false_claim = $appliedFalseClaim
        review_rationale = @($reasons)
        false_friends_pdf_page = $word.false_friend_pdf_page
        tem4_pdf_page = $word.tem4_pdf_page
        pedagogic_decision = 'PENDING_HUMAN_REVIEW'
        reviewer_notes = ''
    })
}

$priorityOrder = @{ CRITICAL = 0; HIGH = 1; MEDIUM = 2; NORMAL = 3 }
$orderedQueue = @($queue | Sort-Object @{ Expression = { $priorityOrder[$_.priority] } }, @{ Expression = { -$_.risk_score } }, item_code)
$orderedQueue | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding utf8
$orderedQueue | Select-Object priority,risk_score,item_code,question_type,word_id,french_word,chinese_gloss,@{Name='source_flags';Expression={$_.source_flags -join '|'}},false_friends_page_evidence,original_source_claim,applied_false_claim,@{Name='review_rationale';Expression={$_.review_rationale -join ' '}},false_friends_pdf_page,tem4_pdf_page,pedagogic_decision,reviewer_notes | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding utf8

$counts = @($orderedQueue | Group-Object priority)
$criticalCount = @($orderedQueue | Where-Object { $_.priority -eq 'CRITICAL' }).Count
$highCount = @($orderedQueue | Where-Object { $_.priority -eq 'HIGH' }).Count
$mediumCount = @($orderedQueue | Where-Object { $_.priority -eq 'MEDIUM' }).Count
$normalCount = @($orderedQueue | Where-Object { $_.priority -eq 'NORMAL' }).Count
$lines = @(
    '# Lexi-Bridge FF4 pedagogic review queue',
    '',
    "- Total items: $($orderedQueue.Count)",
    "- Critical: $criticalCount",
    "- High: $highCount",
    "- Medium: $mediumCount",
    "- Normal: $normalCount",
    "- True/false template fallbacks: $($fallbacks.Count)",
    '',
    'Priority is a review-order aid, not a correctness verdict or publication approval.',
    'All items remain `PENDING_HUMAN_REVIEW`; source pages and reasons are retained in the JSON/CSV queue.'
)
Set-Content -LiteralPath $markdownPath -Value $lines -Encoding utf8
"pedagogic_review_queue=$($orderedQueue.Count); critical=$criticalCount; high=$highCount; medium=$mediumCount; normal=$normalCount"
