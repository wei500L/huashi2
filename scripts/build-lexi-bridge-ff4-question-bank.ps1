$words = Get-Content 'docs/data/lexi-bridge-ff4/wordbook.jsonl' | ForEach-Object { $_ | ConvertFrom-Json }
$items = [System.Collections.Generic.List[object]]::new(); $options = [System.Collections.Generic.List[object]]::new(); $fallbacks = [System.Collections.Generic.List[object]]::new(); $types=@('SINGLE_CHOICE','FILL_BLANK','TRUE_FALSE_WITH_JUSTIFICATION','SHORT_TEXT')
function Normalize-Meaning([string]$value) { return (($value -replace '[\s;,\.\(\)\uFF1B\uFF0C\u3001\u3002\uFF08\uFF09\u2014\-]', '').ToLowerInvariant()) }
function Get-MeaningTokens([string]$value) { return @($value -split ';|,|\uFF1B|\uFF0C|\u3001' | ForEach-Object { Normalize-Meaning $_ } | Where-Object { $_ }) }
function Has-Meaning-Overlap([string]$left, [string]$right) { $leftTokens=Get-MeaningTokens $left; $rightTokens=Get-MeaningTokens $right; foreach($token in $leftTokens){if($rightTokens -contains $token){return $true}}; return $false }
for($i=0;$i -lt $words.Count;$i++){
  $w=$words[$i]; $type=$types[$i % 4]; $code=('FF4Q-{0:D4}' -f ($i+1)); $correct=@()
  switch($type){
    'SINGLE_CHOICE' { $correct=@('A'); $stem="Which French meaning matches $($w.french_word)?"; $d1=$words[($i+1)%$words.Count].chinese_gloss;$d2=$words[($i+2)%$words.Count].chinese_gloss;$d3=$words[($i+3)%$words.Count].chinese_gloss; $options.Add([pscustomobject]@{itemCode=$code;optionCode='A';optionText=$w.chinese_gloss;correct=$true;explanation='Source-verified French meaning.'}); $options.Add([pscustomobject]@{itemCode=$code;optionCode='B';optionText=$d1;correct=$false;explanation='Meaning of a different verified wordbook entry.'}); $options.Add([pscustomobject]@{itemCode=$code;optionCode='C';optionText=$d2;correct=$false;explanation='Meaning of a different verified wordbook entry.'}); $options.Add([pscustomobject]@{itemCode=$code;optionCode='D';optionText=$d3;correct=$false;explanation='Meaning of a different verified wordbook entry.'}) }
    'FILL_BLANK' { $correct=@($w.french_word); $stem="Complete the French word for the source-verified meaning: $($w.chinese_gloss)." }
    'TRUE_FALSE_WITH_JUSTIFICATION' {
      $correct=@('FALSE'); $claim=$w.false_friend_meanings
      if(Has-Meaning-Overlap $claim $w.chinese_gloss){
        $replacement=$null
        for($offset=1;$offset -lt $words.Count;$offset++){
          $candidate=$words[($i+$offset)%$words.Count]
          if(-not (Has-Meaning-Overlap $candidate.chinese_gloss $w.chinese_gloss)){ $replacement=$candidate; break }
        }
        if($null -eq $replacement){throw "Unable to build a non-equivalent false claim for $($w.word_id)"}
        $claim=$replacement.chinese_gloss
        $fallbacks.Add([ordered]@{itemCode=$code;wordId=$w.word_id;frenchWord=$w.french_word;originalClaim=$w.false_friend_meanings;replacementClaim=$claim;reason='SOURCE_CLAIM_OVERLAPS_TARGET_MEANING';falseFriendsPdfPage=$w.false_friend_pdf_page;tem4PdfPage=$w.tem4_pdf_page})
      }
      $stem="$($w.french_word) means $claim in French."
      $options.Add([pscustomobject]@{itemCode=$code;optionCode='TRUE';optionText='True';correct=$false;explanation='The statement does not match the French source meaning.'}); $options.Add([pscustomobject]@{itemCode=$code;optionCode='FALSE';optionText='False';correct=$true;explanation="French source meaning: $($w.chinese_gloss)."})
    }
    default { $correct=@($w.french_word); $stem="Spell the French false-friend headword for: $($w.chinese_gloss)." }
  }
  $items.Add([pscustomobject]@{itemCode=$code;sectionCode=($type);questionType=$type;stemText=$stem;promptText=$stem;correctAnswers=$correct;explanationText="Word $($w.word_id); false-friends PDF p.$($w.false_friend_pdf_page), TEM4 PDF p.$($w.tem4_pdf_page).";requiredAnswer=$true;scored=$true;weight=1;transferCategory='FALSE_FRIEND';contextLevel='HIGH';constructCode='LEXI_BRIDGE_FF4';targetWord=$w.word_id;displayConditionJson=$null})
}
$sections=@();for($i=0;$i -lt $types.Count;$i++){$sections+=@{sectionCode=$types[$i];title=$types[$i];description='Lexi-Bridge FF4';sharedMaterial=$null;sortOrder=$i+1;formalSection=$true}}
$package=[ordered]@{Questionnaire=@{code='LEXI_BRIDGE_FF4_V1';title='French TEM4 False Friends';description='Generated from two visual-verified sources.';durationMinutes=90;scoringVersion='FF4_V1';aiPromptVersion='FF4_V1'};Sections=$sections;Items=$items;Options=$options}
$package|ConvertTo-Json -Depth 8|Set-Content docs/data/lexi-bridge-ff4/question-bank-package.json -Encoding utf8
@($fallbacks)|ConvertTo-Json -Depth 6|Set-Content docs/data/lexi-bridge-ff4/true-false-source-equality-fallbacks.json -Encoding utf8
"questions=$($items.Count) options=$($options.Count) true_false_fallbacks=$($fallbacks.Count)"
