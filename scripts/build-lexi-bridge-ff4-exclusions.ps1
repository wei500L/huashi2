$ff = Get-Content 'docs/data/lexi-bridge-ff4/false-friends-candidates.jsonl' | % { $_ | ConvertFrom-Json } | ? { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' }
$tem = Get-Content 'docs/data/lexi-bridge-ff4/tem4-candidates.jsonl' | % { $_ | ConvertFrom-Json } | ? { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' }
$ffWords = [System.Collections.Generic.HashSet[string]]::new([string[]]@($ff | % normalized_headword), [System.StringComparer]::Ordinal)
$temWords = [System.Collections.Generic.HashSet[string]]::new([string[]]@($tem | % normalized_headword), [System.StringComparer]::Ordinal)
$records = [System.Collections.Generic.List[string]]::new()
foreach($item in $ff){if(-not $temWords.Contains([string]$item.normalized_headword)){$records.Add((@{candidate_id=$item.candidate_id;source_code=$item.source_code;decision='EXCLUDED';reason='No visually verified TEM4 formal-headword evidence for the normalized word form.'}|ConvertTo-Json -Compress))}}
foreach($item in $tem){if(-not $ffWords.Contains([string]$item.normalized_headword)){$records.Add((@{candidate_id=$item.candidate_id;source_code=$item.source_code;decision='EXCLUDED';reason='No visually verified false-friends formal-headword evidence for the normalized word form.'}|ConvertTo-Json -Compress))}}
Set-Content 'docs/data/lexi-bridge-ff4/excluded-candidates.jsonl' $records -Encoding utf8
"excluded_candidates=$($records.Count)"
