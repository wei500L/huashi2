param(
    [Parameter(Mandatory)]
    [ValidateSet('KIRK_GREENE_FALSE_FRIENDS', 'TEM4_CORE_VOCABULARY')]
    [string]$SourceCode,
    [Parameter(Mandatory)]
    [ValidateRange(1, 9999)]
    [int]$PdfPage,
    [Parameter(Mandatory)]
    [ValidateSet('PROCESSED_EXTRACTED', 'PROCESSED_NO_VALID_ENTRY', 'NON_BODY', 'UNREADABLE')]
    [string]$ProcessingStatus,
    [Parameter(Mandatory)]
    [string]$PageType,
    [Nullable[int]]$PrintedPage,
    [string[]]$Candidates = @(),
    [string[]]$Doubts = @(),
    [string]$CorrectionAction = 'GPT multimodal visual verification against rendered source page.',
    [string[]]$FormalRecordIds = @(),
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$checkpointPath = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4\page-processing-checkpoints.jsonl'
$auditPath = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4\content-review-audit.jsonl'
if (-not (Test-Path -LiteralPath $checkpointPath)) {
    throw "Checkpoint file not found: $checkpointPath"
}

$candidatePath = if ($SourceCode -eq 'KIRK_GREENE_FALSE_FRIENDS') {
    Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4\false-friends-candidates.jsonl'
} else {
    Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4\tem4-candidates.jsonl'
}
if (Test-Path -LiteralPath $candidatePath) {
    $seenCandidateIds = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $uniqueCandidateLines = [System.Collections.Generic.List[string]]::new()
    foreach ($candidateLine in (Get-Content -LiteralPath $candidatePath -Encoding utf8 | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        $candidate = $candidateLine | ConvertFrom-Json
        if ($seenCandidateIds.Add([string]$candidate.candidate_id)) {
            $uniqueCandidateLines.Add($candidateLine)
        }
    }
    $uniqueCandidateLines | Set-Content -LiteralPath $candidatePath -Encoding utf8
}

$rows = @(Get-Content -LiteralPath $checkpointPath -Encoding utf8 | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_ | ConvertFrom-Json } |
        Group-Object source_code, pdf_page |
        ForEach-Object {
            $_.Group |
                Sort-Object @{ Expression = { if ($_.visual_verification) { 0 } else { 1 } } }, @{ Expression = { if ($_.processing_status -ne 'PENDING_VISUAL_VERIFICATION') { 0 } else { 1 } } } |
                Select-Object -First 1
        })
$row = $rows | Where-Object { $_.source_code -eq $SourceCode -and [int]$_.pdf_page -eq $PdfPage } | Select-Object -First 1
if ($null -eq $row) {
    throw "No checkpoint found for $SourceCode page $PdfPage"
}

$row.printed_page = $PrintedPage
$row.page_type = $PageType
$row.processing_status = $ProcessingStatus
$row.visual_verification = $true
$row.extracted_candidate_ids = @($Candidates)
$row.layout_or_text_layer_doubts = @($Doubts)
$row.correction_action = $CorrectionAction
$row.formal_record_ids = @($FormalRecordIds)

$rows | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 8 } | Set-Content -LiteralPath $checkpointPath -Encoding utf8

$auditRecord = [ordered]@{
    timestamp = (Get-Date).ToUniversalTime().ToString('o')
    source_code = $SourceCode
    pdf_page = $PdfPage
    printed_page = $PrintedPage
    page_type = $PageType
    processing_status = $ProcessingStatus
    visual_verification = $true
    extracted_candidates = @($Candidates)
    layout_or_text_layer_doubts = @($Doubts)
    correction_action = $CorrectionAction
    audit_note = 'GPT multimodal visual verification completed from a Poppler-rendered source page; no OCR engine was used.'
}
($auditRecord | ConvertTo-Json -Compress -Depth 8) | Add-Content -LiteralPath $auditPath -Encoding utf8

$auditRecord | ConvertTo-Json -Depth 8
