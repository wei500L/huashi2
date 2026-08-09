param([string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path)

$ErrorActionPreference = "Stop"
$sourceScript = Join-Path $PSScriptRoot "enrich-lexi-bridge-ff4-tem4-visual-candidates.ps1"
$candidatePath = Join-Path $ProjectRoot "docs\data\lexi-bridge-ff4\tem4-candidates.jsonl"
$sourceLines = Get-Content -LiteralPath $sourceScript -Encoding utf8
$lookup = @{}
function Set-RecordField([object]$Record, [string]$Name, [object]$Value) {
    $property = $Record.PSObject.Properties[$Name]
    if ($null -eq $property) {
        $Record | Add-Member -NotePropertyName $Name -NotePropertyValue $Value
    } else {
        $property.Value = $Value
    }
}
foreach ($line in ($sourceLines | Select-Object -Skip 11 -First 91)) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $parts = $line -split "\|", 4
    if ($parts.Count -ne 4) { throw "Invalid visual transcription: $line" }
    $key = $parts[0].Split(",")[0].Trim().ToLowerInvariant()
    $lookup[$key] = @($parts[1], $parts[2], $parts[3])
}

$output = [System.Collections.Generic.List[string]]::new()
$changed = 0
foreach ($candidateLine in (Get-Content -LiteralPath $candidatePath -Encoding utf8)) {
    if ([string]::IsNullOrWhiteSpace($candidateLine)) { continue }
    $candidate = $candidateLine | ConvertFrom-Json
    $candidateKey = [string]$candidate.normalized_headword
    $entry = $lookup[$candidateKey]
    if ($null -ne $entry) {
        if ([string]::IsNullOrWhiteSpace([string]$candidate.part_of_speech)) { Set-RecordField $candidate "part_of_speech" $entry[0] }
        if ([string]::IsNullOrWhiteSpace([string]$candidate.ipa)) { Set-RecordField $candidate "ipa" $entry[1] }
        $senseProperty = $candidate.PSObject.Properties["chinese_core_senses"]
        if ($null -eq $senseProperty -or @($candidate.chinese_core_senses | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }).Count -eq 0) {
            Set-RecordField $candidate "chinese_core_senses" @($entry[2].Split([char]0xFF1B))
        }
        Set-RecordField $candidate "source_type" "visual_verified_headword"
        Set-RecordField $candidate "review_state" "VERIFIED"
        Set-RecordField $candidate "missing_fields" @($candidate.missing_fields | Where-Object { $_ -notmatch "Candidate enrichment deferred" })
        $changed++
    }
    $output.Add(($candidate | ConvertTo-Json -Compress -Depth 8))
}
$output | Set-Content -LiteralPath $candidatePath -Encoding utf8
"enriched_candidate_records=$changed; visual_headwords=$($lookup.Count)"
