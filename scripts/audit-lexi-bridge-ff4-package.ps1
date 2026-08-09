$words = Get-Content 'docs/data/lexi-bridge-ff4/wordbook.jsonl' | % { $_ | ConvertFrom-Json }
$package = Get-Content 'docs/data/lexi-bridge-ff4/question-bank-package.json' -Raw | ConvertFrom-Json
$fallbackPath = 'docs/data/lexi-bridge-ff4/true-false-source-equality-fallbacks.json'
$issues = [System.Collections.Generic.List[object]]::new()
$expectedTypes = @('SINGLE_CHOICE','FILL_BLANK','TRUE_FALSE_WITH_JUSTIFICATION','SHORT_TEXT')
function Normalize-Meaning([string]$value) { return (($value -replace '[\s;,\.\(\)\uFF1B\uFF0C\u3001\u3002\uFF08\uFF09\u2014\-]', '').ToLowerInvariant()) }
function Get-MeaningTokens([string]$value) { return @($value -split ';|,|\uFF1B|\uFF0C|\u3001' | ForEach-Object { Normalize-Meaning $_ } | Where-Object { $_ }) }
function Has-Meaning-Overlap([string]$left, [string]$right) { $leftTokens=Get-MeaningTokens $left; $rightTokens=Get-MeaningTokens $right; foreach($token in $leftTokens){if($rightTokens -contains $token){return $true}}; return $false }
$byWord = @{}
foreach($w in $words){
  if([string]::IsNullOrWhiteSpace($w.word_id) -or $byWord.ContainsKey($w.word_id)){
    $issues.Add(@{severity='ERROR';code='INVALID_OR_DUPLICATE_WORD_ID';word=$w.word_id})
    continue
  }
  $byWord[$w.word_id]=$w
  foreach($field in @('french_word','english_word','chinese_gloss','false_friend_meanings','source_code','content_version','review_state')){
    if([string]::IsNullOrWhiteSpace([string]$w.$field)){$issues.Add(@{severity='ERROR';code='MISSING_WORD_FIELD';word=$w.word_id;field=$field})}
  }
  if($w.tem4_pdf_page -lt 1 -or $w.false_friend_pdf_page -lt 1){$issues.Add(@{severity='ERROR';code='INVALID_SOURCE_PAGE';word=$w.word_id})}
  if($w.source_code -ne 'LEXI_BRIDGE_FF4' -or $w.content_version -ne 'FF4_V1'){$issues.Add(@{severity='ERROR';code='UNEXPECTED_WORD_PROVENANCE';word=$w.word_id})}
}

$itemsByCode = @{}
$optionsByItem = @{}
foreach($option in $package.Options){
  if(-not $optionsByItem.ContainsKey($option.itemCode)){$optionsByItem[$option.itemCode]=[System.Collections.Generic.List[object]]::new()}
  $optionsByItem[$option.itemCode].Add($option)
}

$seen = [System.Collections.Generic.HashSet[string]]::new()
foreach($item in $package.Items){
  if([string]::IsNullOrWhiteSpace($item.itemCode) -or $itemsByCode.ContainsKey($item.itemCode)){$issues.Add(@{severity='ERROR';code='INVALID_OR_DUPLICATE_ITEM_CODE';item=$item.itemCode});continue}
  $itemsByCode[$item.itemCode]=$item
  if(-not $byWord.ContainsKey($item.targetWord)){$issues.Add(@{severity='ERROR';code='ORPHAN_WORD';item=$item.itemCode})}
  elseif(-not $seen.Add($item.targetWord)){$issues.Add(@{severity='ERROR';code='DUPLICATE_WORD';item=$item.itemCode})}
  if($expectedTypes -notcontains $item.questionType -or $item.sectionCode -ne $item.questionType){$issues.Add(@{severity='ERROR';code='INVALID_QUESTION_TYPE_OR_SECTION';item=$item.itemCode})}
  if([string]::IsNullOrWhiteSpace($item.stemText) -or $item.promptText -ne $item.stemText){$issues.Add(@{severity='ERROR';code='INVALID_PROMPT';item=$item.itemCode})}
  if(@($item.correctAnswers).Count -ne 1 -or [string]::IsNullOrWhiteSpace([string]$item.correctAnswers[0])){$issues.Add(@{severity='ERROR';code='INVALID_CORRECT_ANSWER';item=$item.itemCode})}
  if($item.requiredAnswer -ne $true -or $item.scored -ne $true -or $item.weight -ne 1){$issues.Add(@{severity='ERROR';code='INVALID_SCORING_FLAGS';item=$item.itemCode})}
  if([string]::IsNullOrWhiteSpace($item.explanationText)){$issues.Add(@{severity='ERROR';code='MISSING_EXPLANATION';item=$item.itemCode})}
  elseif($byWord.ContainsKey($item.targetWord)){
    $word=$byWord[$item.targetWord]
    $expectedEvidence="Word $($word.word_id); false-friends PDF p.$($word.false_friend_pdf_page), TEM4 PDF p.$($word.tem4_pdf_page)."
    if($item.explanationText -ne $expectedEvidence){$issues.Add(@{severity='ERROR';code='SOURCE_EVIDENCE_MISMATCH';item=$item.itemCode})}
  }
  $opts=if($optionsByItem.ContainsKey($item.itemCode)){@($optionsByItem[$item.itemCode].ToArray())}else{@()}
  $optionCodes=@($opts | ForEach-Object {$_.optionCode})
  if($item.questionType -eq 'SINGLE_CHOICE'){
    $missingOptionCodes=@(@('A','B','C','D') | Where-Object {$_ -notin $optionCodes})
    if($opts.Count -ne 4 -or @($opts|?{$_.correct}).Count -ne 1 -or @($optionCodes | Sort-Object -Unique).Count -ne 4 -or $missingOptionCodes.Count -ne 0){$issues.Add(@{severity='ERROR';code='INVALID_SINGLE_CHOICE';item=$item.itemCode})}
    elseif($item.correctAnswers[0] -ne 'A' -or -not (@($opts|?{$_.optionCode -eq 'A' -and $_.correct}).Count -eq 1) -or $opts[0].optionText -ne $byWord[$item.targetWord].chinese_gloss){$issues.Add(@{severity='ERROR';code='SINGLE_CHOICE_KEY_MISMATCH';item=$item.itemCode})}
  } elseif($item.questionType -eq 'TRUE_FALSE_WITH_JUSTIFICATION') {
    $missingOptionCodes=@(@('TRUE','FALSE') | Where-Object {$_ -notin $optionCodes})
    if($opts.Count -ne 2 -or @($opts|?{$_.correct}).Count -ne 1 -or @($optionCodes | Sort-Object -Unique).Count -ne 2 -or $missingOptionCodes.Count -ne 0){$issues.Add(@{severity='ERROR';code='INVALID_TRUE_FALSE';item=$item.itemCode})}
    elseif($item.correctAnswers[0] -ne 'FALSE' -or -not (@($opts|?{$_.optionCode -eq 'FALSE' -and $_.correct}).Count -eq 1)){$issues.Add(@{severity='ERROR';code='TRUE_FALSE_KEY_MISMATCH';item=$item.itemCode})}
    if($byWord.ContainsKey($item.targetWord) -and (Has-Meaning-Overlap $byWord[$item.targetWord].chinese_gloss $byWord[$item.targetWord].false_friend_meanings) -and $item.stemText -eq "$($byWord[$item.targetWord].french_word) means $($byWord[$item.targetWord].false_friend_meanings) in French."){$issues.Add(@{severity='ERROR';code='TRUE_FALSE_SOURCE_CLAIM_OVERLAP';item=$item.itemCode})}
  } else {
    if($opts.Count -ne 0){$issues.Add(@{severity='ERROR';code='UNEXPECTED_OPTIONS';item=$item.itemCode})}
    elseif($item.correctAnswers[0] -ne $byWord[$item.targetWord].french_word){$issues.Add(@{severity='ERROR';code='WORD_KEY_MISMATCH';item=$item.itemCode})}
  }
}
foreach($optionItemCode in $optionsByItem.Keys){if(-not $itemsByCode.ContainsKey($optionItemCode)){$issues.Add(@{severity='ERROR';code='ORPHAN_OPTION';item=$optionItemCode})}}
if($package.Items.Count -ne $words.Count){$issues.Add(@{severity='ERROR';code='QUESTION_WORD_COUNT_MISMATCH'})}
if($package.Sections.Count -ne $expectedTypes.Count -or @($package.Sections | ForEach-Object {$_.sectionCode} | Sort-Object -Unique).Count -ne $expectedTypes.Count){$issues.Add(@{severity='ERROR';code='INVALID_SECTION_SET'})}
$expectedFallbackItemCodes = @($package.Items | Where-Object { $_.questionType -eq 'TRUE_FALSE_WITH_JUSTIFICATION' -and $byWord.ContainsKey($_.targetWord) -and (Has-Meaning-Overlap $byWord[$_.targetWord].chinese_gloss $byWord[$_.targetWord].false_friend_meanings) } | ForEach-Object { $_.itemCode })
if(-not (Test-Path -LiteralPath $fallbackPath)){
  $issues.Add(@{severity='ERROR';code='MISSING_TRUE_FALSE_FALLBACK_LEDGER'})
  $fallbackItemCodes=@()
} else {
  $fallbackRaw=Get-Content -LiteralPath $fallbackPath -Raw | ConvertFrom-Json
  $fallbacks=@($fallbackRaw | ForEach-Object { $_ })
  $fallbackItemCodes=@($fallbacks | ForEach-Object { $_.itemCode })
  if(@($fallbackItemCodes | Sort-Object -Unique).Count -ne $fallbackItemCodes.Count){$issues.Add(@{severity='ERROR';code='DUPLICATE_TRUE_FALSE_FALLBACK'})}
  foreach($itemCode in $expectedFallbackItemCodes){if($fallbackItemCodes -notcontains $itemCode){$issues.Add(@{severity='ERROR';code='MISSING_TRUE_FALSE_FALLBACK';item=$itemCode})}}
  foreach($itemCode in $fallbackItemCodes){if($expectedFallbackItemCodes -notcontains $itemCode){$issues.Add(@{severity='ERROR';code='UNEXPECTED_TRUE_FALSE_FALLBACK';item=$itemCode})}}
  foreach($fallback in $fallbacks){
    if(-not $itemsByCode.ContainsKey($fallback.itemCode)){$issues.Add(@{severity='ERROR';code='ORPHAN_TRUE_FALSE_FALLBACK';item=$fallback.itemCode});continue}
    $item=$itemsByCode[$fallback.itemCode];$word=$byWord[$item.targetWord]
    if([string]::IsNullOrWhiteSpace([string]$fallback.replacementClaim) -or (Has-Meaning-Overlap $fallback.replacementClaim $word.chinese_gloss)){$issues.Add(@{severity='ERROR';code='INVALID_TRUE_FALSE_FALLBACK_CLAIM';item=$fallback.itemCode})}
    if($fallback.wordId -ne $word.word_id -or $item.stemText -ne "$($word.french_word) means $($fallback.replacementClaim) in French."){$issues.Add(@{severity='ERROR';code='TRUE_FALSE_FALLBACK_LEDGER_MISMATCH';item=$fallback.itemCode})}
  }
}
$sourceEqualityWords=@($words|Where-Object{(Normalize-Meaning $_.chinese_gloss) -eq (Normalize-Meaning $_.false_friend_meanings)}|ForEach-Object{$_.word_id})
$report=[ordered]@{wordbookCount=$words.Count;questionCount=$package.Items.Count;optionCount=$package.Options.Count;questionTypeCounts=@($package.Items|Group-Object questionType|%{@{type=$_.Name;count=$_.Count}});uniqueTargetWords=$seen.Count;sourceEqualityReviewWordIds=$sourceEqualityWords;trueFalseTemplateFallbackCount=$fallbackItemCodes.Count;issueCount=$issues.Count;issues=@($issues);auditStatus=if($issues.Count){'FAILED'}else{'STRUCTURAL_PASS'};pedagogicReview='PENDING_HUMAN_REVIEW';publicationStatus='NOT_ELIGIBLE_PENDING_REVIEW_AND_DATABASE_SAFETY_CHECK';databaseWrite='NOT_ATTEMPTED_UNSAFE_BOUNDARY'}
$report|ConvertTo-Json -Depth 6|Set-Content docs/data/lexi-bridge-ff4/review-report.json -Encoding utf8
$lines=@('# Lexi-Bridge FF4 package review',"","- Wordbook: $($report.wordbookCount)","- Questions: $($report.questionCount)","- Options: $($report.optionCount)","- Unique word targets: $($report.uniqueTargetWords)","- True/false source-overlap fallbacks: $($report.trueFalseTemplateFallbackCount)","- Structural issues: $($report.issueCount)","- Pedagogic review: PENDING_HUMAN_REVIEW","- Publication: NOT_ELIGIBLE_PENDING_REVIEW_AND_DATABASE_SAFETY_CHECK","- Database write: NOT ATTEMPTED_UNSAFE_BOUNDARY")
Set-Content docs/data/lexi-bridge-ff4/review-report.md $lines -Encoding utf8
"audit=$($report.auditStatus) issues=$($issues.Count)"
