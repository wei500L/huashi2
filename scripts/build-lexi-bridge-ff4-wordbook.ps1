$ff = Get-Content 'docs/data/lexi-bridge-ff4/false-friends-candidates.jsonl' | ForEach-Object { $_ | ConvertFrom-Json } | Where-Object { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' }
$tem = Get-Content 'docs/data/lexi-bridge-ff4/tem4-candidates.jsonl' | ForEach-Object { $_ | ConvertFrom-Json } | Where-Object { $_.visual_verification -eq $true -and $_.review_state -eq 'VERIFIED' }
$byWord = @{}
foreach ($f in $ff) { if (-not $byWord.ContainsKey($f.normalized_headword)) { $byWord[$f.normalized_headword] = $f } }
$seen = [System.Collections.Generic.HashSet[string]]::new()
$rows = foreach ($t in $tem) {
  $key = [string]$t.normalized_headword
  if (-not $byWord.ContainsKey($key) -or -not $seen.Add($key)) { continue }
  $f = $byWord[$key]
  [pscustomobject]@{ word_id = 'LEXI_BRIDGE_FF4-' + $seen.Count.ToString('D4'); french_word = $t.standard_french_headword; english_word = $f.english_confusable; chinese_gloss = (@($f.french_true_meanings) -join '; '); false_friend_meanings = (@($f.english_true_meanings) -join '; '); tem4_pdf_page = $t.pdf_page; false_friend_pdf_page = $f.pdf_page; unit = $t.unit; source_code = 'LEXI_BRIDGE_FF4'; content_version = 'FF4_V1'; review_state = 'AUTO_SOURCE_VERIFIED' }
}
$rows | ForEach-Object { $_ | ConvertTo-Json -Compress } | Set-Content docs/data/lexi-bridge-ff4/wordbook.jsonl -Encoding utf8
$rows | Export-Csv docs/data/lexi-bridge-ff4/wordbook.csv -NoTypeInformation -Encoding utf8
$rows | Select-Object english_word,french_word,chinese_gloss,@{n='lexical_pair_type';e={'false_friend'}},@{n='semantic_overlap_score';e={'0.20'}},@{n='false_friend_risk';e={'0.90'}},@{n='default_context_support';e={'high'}},@{n='difficulty_level';e={'4'}},@{n='notes';e={'Two visual source records'}},@{n='source';e={'Kirk-Greene and TEM4'}},source_code,content_version,word_id,@{n='active';e={'true'}},@{n='tags';e={'false-friend|tem4|lexi-bridge'}},@{n='knowledge_status';e={'ready'}},@{n='embedding_status';e={'pending'}} | Export-Csv docs/data/lexi-bridge-ff4/lexical-import.csv -NoTypeInformation -Encoding utf8
"wordbook_rows=$($rows.Count)"
