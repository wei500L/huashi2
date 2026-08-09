$ff = Get-Content 'docs/data/lexi-bridge-ff4/false-friends-candidates.jsonl' | ForEach-Object { $_ | ConvertFrom-Json } | Where-Object { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' }
$tem = Get-Content 'docs/data/lexi-bridge-ff4/tem4-candidates.jsonl' | ForEach-Object { $_ | ConvertFrom-Json } | Where-Object { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' }
$ffWords = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
foreach ($entry in $ff) { $null = $ffWords.Add([string]$entry.normalized_headword) }
$out = [System.Collections.Generic.List[string]]::new()
foreach ($item in $tem) {
  $key = [string]$item.normalized_headword
  if (-not $ffWords.Contains($key)) { continue }
  $matches = @($ff | Where-Object { [string]$_.normalized_headword -eq $key })
  $decision = [ordered]@{
    decision_id = "INTERSECTION-$($item.candidate_id)"; normalized_headword = $item.normalized_headword
    tem4_candidate_id = $item.candidate_id; false_friend_candidate_ids = @($matches | ForEach-Object candidate_id)
    decision = 'CANDIDATE_REVIEW'; rationale = 'Both sources have visually verified formal headwords; retain for sense and part-of-speech reconciliation before publication.'
    tem4_evidence = @{ pdf_page=$item.pdf_page; printed_page=$item.printed_page; evidence=$item.page_evidence }
    false_friend_evidence = @($matches | ForEach-Object { @{candidate_id=$_.candidate_id;pdf_page=$_.pdf_page;printed_page=$_.printed_page; evidence=$_.page_evidence} })
  }
  $out.Add(($decision | ConvertTo-Json -Compress -Depth 6))
}
Set-Content -Path 'docs/data/lexi-bridge-ff4/intersection-decisions.jsonl' -Value $out -Encoding utf8
"intersection_decisions=$($out.Count)"
