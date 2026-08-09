param(
  [string]$LedgerPath = 'docs/data/lexi-bridge-ff4/tem4-visual-headword-ledger.jsonl',
  [string]$CandidatePath = 'docs/data/lexi-bridge-ff4/tem4-candidates.jsonl'
)

$existing = [System.Collections.Generic.HashSet[string]]::new()
Get-Content $CandidatePath | ForEach-Object {
  try { $null = $existing.Add((($_ | ConvertFrom-Json).candidate_id)) } catch { throw "Invalid candidate JSONL: $_" }
}

$append = [System.Collections.Generic.List[string]]::new()
Get-Content $LedgerPath | ForEach-Object {
  $page = $_ | ConvertFrom-Json
  for ($i = 0; $i -lt $page.headwords.Count; $i++) {
    $raw = [string]$page.headwords[$i]
    $candidateId = 'TEM4-P{0}-{1:D2}' -f $page.pdf_page, ($i + 1)
    if ($existing.Contains($candidateId)) { continue }
    $standard = $raw
    if ($standard.EndsWith([char]0x00B9) -or $standard.EndsWith([char]0x00B2) -or $standard.EndsWith([char]0x00B3) -or $standard.EndsWith([char]0x2074)) {
      $standard = $standard.Substring(0, $standard.Length - 1)
    }
    $standard = $standard -replace ',(e|le|elle|ère|ve|se)$', ''
    $record = [ordered]@{
      candidate_id = $candidateId; source_code = 'TEM4_CORE_VOCABULARY'; pdf_page = $page.pdf_page
      printed_page = $page.printed_page; page_type = $page.page_type; source_type = 'visual_verified_headword'
      raw_headword = $raw; standard_french_headword = $standard; normalized_headword = $standard.ToLowerInvariant()
      part_of_speech = $null; ipa = $null; chinese_core_senses = @(); unit = $page.unit
      frequency_or_exam_markers = @(); explicitly_listed_derivatives = @(); page_evidence = 'GPT visual verification of rendered source page.'
      raw_recognition_text = $null; cleaned_text = $raw; visual_verification = $true; review_state = 'VERIFIED'
      missing_fields = @('Candidate enrichment deferred to source-specific final intersection review.')
    }
    $append.Add(($record | ConvertTo-Json -Compress -Depth 4))
  }
}
if ($append.Count) { Add-Content -Path $CandidatePath -Value $append -Encoding utf8 }
"added_candidates=$($append.Count)"
