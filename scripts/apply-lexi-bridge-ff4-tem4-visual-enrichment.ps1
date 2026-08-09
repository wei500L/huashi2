param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'
$sourceScript = Join-Path $PSScriptRoot 'enrich-lexi-bridge-ff4-tem4-visual-candidates.ps1'
$candidatePath = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4\tem4-candidates.jsonl'

# Read the page-verified transcription rows as data, rather than executing the
# draft source script that contains them. Rows 12..102 are the visual records.
$transcriptions = Get-Content -LiteralPath $sourceScript -Encoding utf8 |
    Select-Object -Skip 11 -First 91 |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

$byNormalized = @{}
foreach ($line in $transcriptions) {
    $parts = $line -split '\|', 4
    if ($parts.Count -ne 4) { throw "Invalid visual transcription: $line" }
    $normalized = $parts[0].Split(',')[0].Trim().ToLowerInvariant()
    $byNormalized[$normalized] = [ordered]@{
        part_of_speech = $parts[1]
        ipa = $parts[2]
        chinese_core_senses = @($parts[3] -split '；' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
}

$changed = 0
$rows = Get-Content -LiteralPath $candidatePath -Encoding utf8 |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object {
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
