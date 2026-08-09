param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

function Resolve-PopplerCommand {
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if ($userPath) {
        $env:Path = "$userPath;$env:Path"
    }
    $command = Get-Command pdftotext -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw 'pdftotext is required for non-final candidate navigation. It must not be substituted for visual verification.'
    }
    return $command.Source
}

function Read-TextLayerPage {
    param(
        [string]$PdfPath,
        [int]$PageNumber,
        [string]$PdfToText
    )

    $temporaryText = Join-Path $env:TEMP "lexi-bridge-ff4-$([Guid]::NewGuid()).txt"
    try {
        & $PdfToText -f $PageNumber -l $PageNumber -layout -enc UTF-8 $PdfPath $temporaryText | Out-Null
        if (-not (Test-Path -LiteralPath $temporaryText)) {
            return ''
        }
        return Get-Content -LiteralPath $temporaryText -Raw -Encoding utf8
    } finally {
        if (Test-Path -LiteralPath $temporaryText) {
            Remove-Item -LiteralPath $temporaryText -Force
        }
    }
}

function Convert-ToCanonicalText {
    param([AllowNull()][string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ''
    }
    return ($Value.Normalize([Text.NormalizationForm]::FormKC) -replace '\s+', ' ').Trim()
}

function Get-FalseFriendsTextLayerCandidates {
    param(
        [string]$PdfPath,
        [int]$PageCount,
        [string]$PdfToText
    )

    $candidates = [System.Collections.Generic.List[object]]::new()
    for ($page = 1; $page -le $PageCount; $page += 1) {
        $rawPageText = Read-TextLayerPage -PdfPath $PdfPath -PageNumber $page -PdfToText $PdfToText
        $lineNumber = 0
        foreach ($rawLine in ($rawPageText -split "`r?`n")) {
            $lineNumber += 1
            $line = Convert-ToCanonicalText $rawLine
            if ($line -notmatch '^(?<head>[a-zàâçéèêëîïôöùûüÿœæ][a-zàâçéèêëîïôöùûüÿœæ''-]{1,40})(?:\s+\([mfn]\)|\s{2,}|\s+[A-Z])') {
                continue
            }
            $candidates.Add([ordered]@{
                    candidate_id = "KIRK_GREENE-P$('{0:D3}' -f $page)-L$('{0:D2}' -f $lineNumber)"
                    source_code = 'KIRK_GREENE_FALSE_FRIENDS'
                    pdf_page = $page
                    printed_page = $null
                    page_type = 'UNCLASSIFIED_PENDING_VISUAL_VERIFICATION'
                    source_type = 'text_layer_navigation_candidate'
                    raw_headword = $Matches.head
                    normalized_headword = (Convert-ToCanonicalText $Matches.head).ToLowerInvariant()
                    raw_recognition_text = $line
                    cleaned_text = $line
                    visual_verification = $false
                    review_state = 'PENDING_VISUAL_VERIFICATION'
                    note = 'Candidate only. The text layer is navigation evidence, not a reliable final source.'
                })
        }
    }
    return $candidates
}

$dataDirectory = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4'
$sourceDirectory = Join-Path $ProjectRoot 'docs\source-materials\lexi-bridge-ff4'
$falseFriendsPdf = Join-Path $sourceDirectory 'french-false-friends-kirk-greene.pdf'
$tem4Pdf = Join-Path $sourceDirectory 'french-tem4-core-vocabulary.pdf'

New-Item -ItemType Directory -Path $dataDirectory -Force | Out-Null
foreach ($path in @($sourceDirectory, $falseFriendsPdf, $tem4Pdf)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required FF4 processing path is missing: $path"
    }
}

$pdfToText = Resolve-PopplerCommand
$pageCounts = [ordered]@{
    KIRK_GREENE_FALSE_FRIENDS = 203
    TEM4_CORE_VOCABULARY = 385
}

$checkpoints = [System.Collections.Generic.List[object]]::new()
foreach ($source in $pageCounts.Keys) {
    for ($page = 1; $page -le $pageCounts[$source]; $page += 1) {
        $checkpoints.Add([ordered]@{
                source_code = $source
                pdf_page = $page
                printed_page = $null
                page_type = 'UNCLASSIFIED_PENDING_VISUAL_VERIFICATION'
                processing_status = 'PENDING_VISUAL_VERIFICATION'
                visual_verification = $false
                extracted_candidate_ids = @()
                layout_or_text_layer_doubts = @()
                correction_action = $null
                formal_record_ids = @()
            })
    }
}

$falseFriendsCandidates = Get-FalseFriendsTextLayerCandidates -PdfPath $falseFriendsPdf -PageCount $pageCounts.KIRK_GREENE_FALSE_FRIENDS -PdfToText $pdfToText

$checkpointPath = Join-Path $dataDirectory 'page-processing-checkpoints.jsonl'
$falseFriendsPath = Join-Path $dataDirectory 'false-friends-candidates.jsonl'
$tem4Path = Join-Path $dataDirectory 'tem4-candidates.jsonl'

$checkpoints | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 6 } | Set-Content -LiteralPath $checkpointPath -Encoding utf8
$falseFriendsCandidates | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 6 } | Set-Content -LiteralPath $falseFriendsPath -Encoding utf8
Set-Content -LiteralPath $tem4Path -Value '' -Encoding utf8

[ordered]@{
    data_directory = $dataDirectory
    checkpoints = $checkpoints.Count
    false_friends_text_layer_candidates = $falseFriendsCandidates.Count
    tem4_candidates = 0
    next_required_step = 'Render and visually verify every PDF page with GPT; populate independent TEM4 candidates and correct false-friends candidates from the source images.'
} | ConvertTo-Json -Depth 4
