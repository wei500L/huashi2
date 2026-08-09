param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

$dataDirectory = Join-Path $ProjectRoot 'docs\data\lexi-bridge-ff4'
$outputDirectory = Join-Path $dataDirectory 'reader-artifacts'
$wordbookPath = Join-Path $dataDirectory 'wordbook.jsonl'
$packagePath = Join-Path $dataDirectory 'question-bank-package.json'
$reportPath = Join-Path $dataDirectory 'review-report.json'
$queuePath = Join-Path $dataDirectory 'pedagogic-review-queue.json'
$workbookPath = Join-Path $outputDirectory 'lexi-bridge-ff4-review-workbook.xlsx'
$documentPath = Join-Path $outputDirectory 'lexi-bridge-ff4-review-packet.docx'
$pdfPath = Join-Path $outputDirectory 'lexi-bridge-ff4-review-packet.pdf'
$htmlPath = Join-Path (Join-Path $ProjectRoot 'tmp') 'lexi-bridge-ff4-review-packet.source.html'

foreach ($requiredPath in @($wordbookPath, $packagePath, $reportPath, $queuePath)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Missing required generated input: $requiredPath"
    }
}

$report = Get-Content -LiteralPath $reportPath -Raw -Encoding utf8 | ConvertFrom-Json
if ($report.auditStatus -ne 'STRUCTURAL_PASS' -or $report.issueCount -ne 0) {
    throw 'Reader artifacts require a zero-issue structural audit.'
}

$words = @(Get-Content -LiteralPath $wordbookPath -Encoding utf8 | Where-Object { $_ } | ForEach-Object { $_ | ConvertFrom-Json })
$package = Get-Content -LiteralPath $packagePath -Raw -Encoding utf8 | ConvertFrom-Json
$queueRaw = Get-Content -LiteralPath $queuePath -Raw -Encoding utf8 | ConvertFrom-Json
$reviewQueue = @($queueRaw | ForEach-Object { $_ })
$items = @($package.Items)
$options = @($package.Options)
if ($words.Count -ne $report.wordbookCount -or $items.Count -ne $report.questionCount -or $options.Count -ne $report.optionCount) {
    throw 'Generated input counts do not match the structural audit.'
}
if ($reviewQueue.Count -ne $items.Count) { throw 'Pedagogic review queue count does not match the generated item count.' }

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

function New-Matrix {
    param([object[]]$Rows, [int]$ColumnCount)
    $matrix = New-Object 'object[,]' $Rows.Count, $ColumnCount
    for ($rowIndex = 0; $rowIndex -lt $Rows.Count; $rowIndex++) {
        for ($columnIndex = 0; $columnIndex -lt $ColumnCount; $columnIndex++) {
            $matrix[$rowIndex, $columnIndex] = $Rows[$rowIndex][$columnIndex]
        }
    }
    return ,$matrix
}

function Set-TabularSheet {
    param(
        [object]$Workbook,
        [string]$Name,
        [string]$Title,
        [string[]]$Headers,
        [object[]]$Rows,
        [int[]]$ColumnWidths
    )

    $sheet = $Workbook.Worksheets.Add()
    $sheet.Name = $Name
    $sheet.DisplayPageBreaks = $false
    $sheet.Cells.Font.Name = 'Aptos'
    $sheet.Cells.Font.Size = 10
    $sheet.Rows.VerticalAlignment = -4108
    $sheet.Range($sheet.Cells.Item(1, 1), $sheet.Cells.Item(1, $Headers.Count)).Merge()
    $sheet.Cells.Item(1, 1).Value2 = $Title
    $sheet.Cells.Item(1, 1).Font.Bold = $true
    $sheet.Cells.Item(1, 1).Font.Size = 16
    $sheet.Cells.Item(1, 1).Font.Color = 16777215
    $sheet.Cells.Item(1, 1).Interior.Color = 5983516
    $sheet.Cells.Item(1, 1).HorizontalAlignment = -4131
    $sheet.Cells.Item(1, 1).RowHeight = 28

    $headerRange = $sheet.Range($sheet.Cells.Item(2, 1), $sheet.Cells.Item(2, $Headers.Count))
    $headerValues = New-Object 'object[,]' 1, $Headers.Count
    for ($columnIndex = 0; $columnIndex -lt $Headers.Count; $columnIndex++) {
        $headerValues[0, $columnIndex] = $Headers[$columnIndex]
    }
    $headerRange.Value2 = $headerValues
    $headerRange.Font.Bold = $true
    $headerRange.Font.Color = 16777215
    $headerRange.Interior.Color = 7693618
    $headerRange.WrapText = $true
    $headerRange.RowHeight = 30
    $headerRange.HorizontalAlignment = -4108
    $headerRange.Borders.LineStyle = 1
    $headerRange.Borders.Color = 14341837

    if ($Rows.Count -gt 0) {
        $bodyRange = $sheet.Range($sheet.Cells.Item(3, 1), $sheet.Cells.Item($Rows.Count + 2, $Headers.Count))
        $bodyRange.Value2 = New-Matrix -Rows $Rows -ColumnCount $Headers.Count
        $bodyRange.WrapText = $true
        $bodyRange.VerticalAlignment = -4160
        $bodyRange.Borders.LineStyle = 1
        $bodyRange.Borders.Color = 15132390
        $bodyRange.RowHeight = 34
        $sheet.Range($sheet.Cells.Item(2, 1), $sheet.Cells.Item($Rows.Count + 2, $Headers.Count)).AutoFilter() | Out-Null
        $decisionColumn = [array]::IndexOf($Headers, 'Pedagogic decision') + 1
        if ($decisionColumn -gt 0) {
            $decisionRange = $sheet.Range($sheet.Cells.Item(3, $decisionColumn), $sheet.Cells.Item($Rows.Count + 2, $decisionColumn))
            $decisionRange.Validation.Delete()
            $decisionRange.Validation.Add(3, 1, 1, 'PENDING_HUMAN_REVIEW,APPROVED,REVISE,REJECT')
            $decisionRange.Validation.InCellDropdown = $true
        }
    }

    for ($columnIndex = 0; $columnIndex -lt $ColumnWidths.Count; $columnIndex++) {
        $sheet.Columns.Item($columnIndex + 1).ColumnWidth = $ColumnWidths[$columnIndex]
    }
    $sheet.Range('A3').Select() | Out-Null
    $sheet.Application.ActiveWindow.FreezePanes = $true
    return $sheet
}

function HtmlEncode([object]$Value) {
    if ($null -eq $Value) { return '' }
    return [System.Net.WebUtility]::HtmlEncode([string]$Value)
}

# Create the formatted review workbook without changing the generated records.
$excel = $null
$workbook = $null
try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    $workbook = $excel.Workbooks.Add()
    $overview = $workbook.Worksheets.Item(1)
    $overview.Name = 'Overview'
    $overview.Cells.Font.Name = 'Aptos'
    $overview.Cells.Font.Size = 10
    $overview.Range('A1:F1').Merge()
    $overview.Range('A1').Value2 = 'Lexi-Bridge FF4 | manual review workbook'
    $overview.Range('A1').Font.Bold = $true
    $overview.Range('A1').Font.Size = 18
    $overview.Range('A1').Font.Color = 16777215
    $overview.Range('A1').Interior.Color = 5983516
    $overview.Range('A1').HorizontalAlignment = -4108
    $overview.Range('A1').RowHeight = 34
    $criticalReviewCount = @($reviewQueue | Where-Object { $_.priority -eq 'CRITICAL' }).Count
    $overviewSummaryRows = [System.Collections.Generic.List[object]]::new()
    $overviewSummaryRows.Add([object[]]@('Questionnaire code', $package.Questionnaire.code))
    $overviewSummaryRows.Add([object[]]@('Structural audit', $report.auditStatus))
    $overviewSummaryRows.Add([object[]]@('Word records', $words.Count))
    $overviewSummaryRows.Add([object[]]@('Question records', $items.Count))
    $overviewSummaryRows.Add([object[]]@('Option records', $options.Count))
    $overviewSummaryRows.Add([object[]]@('Pedagogic review', $report.pedagogicReview))
    $overviewSummaryRows.Add([object[]]@('Publication', $report.publicationStatus))
    $overviewSummaryRows.Add([object[]]@('Critical review items', $criticalReviewCount))
    $overview.Range('A3:B10').Value2 = New-Matrix -Rows $overviewSummaryRows -ColumnCount 2
    $overview.Range('A3:A10').Font.Bold = $true
    $overview.Range('A3:B10').Borders.LineStyle = 1
    $overview.Range('A3:B10').Borders.Color = 15132390
    $overview.Range('A3:A10').Interior.Color = 15789798
    $overview.Range('A11:F11').Merge()
    $overview.Range('A11').Value2 = 'Use the filterable sheets for an item-by-item pedagogic review. Do not treat a pending decision as approval or publication authority.'
    $overview.Range('A11').WrapText = $true
    $overview.Range('A11').Interior.Color = 14086655
    $overview.Range('A11').Font.Color = 2122368
    $overview.Range('A11').RowHeight = 42
    $overviewBoundaryRows = @(
        @('Review decision values', 'PENDING_HUMAN_REVIEW | APPROVED | REVISE | REJECT'),
        @('Source inputs', 'wordbook.jsonl; question-bank-package.json; review-report.json'),
        @('Database action', 'NOT ATTEMPTED'),
        @('Publication action', 'NOT ELIGIBLE until human review and database safety check')
    )
    $overview.Range('A13:B16').Value2 = New-Matrix -Rows $overviewBoundaryRows -ColumnCount 2
    $overview.Range('A13:A16').Font.Bold = $true
    $overview.Range('A13:B16').WrapText = $true
    $overview.Range('A13:B16').Borders.LineStyle = 1
    $overview.Range('A13:B16').Borders.Color = 15132390
    $overview.Columns.Item(1).ColumnWidth = 26
    $overview.Columns.Item(2).ColumnWidth = 72
    $overview.Columns.Item(3).ColumnWidth = 15
    $overview.Columns.Item(4).ColumnWidth = 15
    $overview.Columns.Item(5).ColumnWidth = 15
    $overview.Columns.Item(6).ColumnWidth = 15
    $overview.Range('A3').Select() | Out-Null
    $excel.ActiveWindow.DisplayGridlines = $false

    $queueRows = [System.Collections.Generic.List[object]]::new()
    foreach ($queueEntry in $reviewQueue) {
        $queueRows.Add([object[]]@($queueEntry.priority, $queueEntry.risk_score, $queueEntry.item_code, $queueEntry.question_type, $queueEntry.word_id, $queueEntry.french_word, $queueEntry.chinese_gloss, (@($queueEntry.source_flags) -join ' | '), $queueEntry.false_friends_page_evidence, $queueEntry.original_source_claim, $queueEntry.applied_false_claim, (@($queueEntry.review_rationale) -join ' '), $queueEntry.false_friends_pdf_page, $queueEntry.tem4_pdf_page, $queueEntry.pedagogic_decision, $queueEntry.reviewer_notes))
    }
    Set-TabularSheet -Workbook $workbook -Name 'Review Queue' -Title 'Pedagogic review queue | priority is not approval' -Headers @('Priority','Risk score','Item code','Question type','Word ID','French','Chinese gloss','Source flags','Source evidence note','Original source claim','Applied false claim','Review rationale','False-friends page','TEM4 page','Pedagogic decision','Reviewer notes') -Rows $queueRows -ColumnWidths @(13,11,18,32,23,18,34,35,60,31,31,72,17,12,27,36) | Out-Null

    $wordRows = [System.Collections.Generic.List[object]]::new()
    foreach ($entry in $words) {
        $wordRows.Add([object[]]@($entry.word_id, $entry.french_word, $entry.english_word, $entry.chinese_gloss, $entry.false_friend_meanings, $entry.tem4_pdf_page, $entry.false_friend_pdf_page, $entry.unit, $entry.source_code, $entry.review_state, 'PENDING_HUMAN_REVIEW', ''))
    }
    Set-TabularSheet -Workbook $workbook -Name 'Vocabulary' -Title 'Vocabulary records | source-backed, pending pedagogic review' -Headers @('Word ID','French','English confusable','Chinese gloss','English-source meaning','TEM4 page','False-friends page','Unit','Source code','Source state','Pedagogic decision','Reviewer notes') -Rows $wordRows -ColumnWidths @(23,18,20,34,34,11,15,14,19,22,27,36) | Out-Null

    $questionRows = [System.Collections.Generic.List[object]]::new()
    foreach ($item in $items) {
        $questionRows.Add([object[]]@($item.itemCode, $item.questionType, $item.targetWord, $item.stemText, (@($item.correctAnswers) -join ' | '), $item.explanationText, $item.requiredAnswer, $item.weight, $item.transferCategory, 'PENDING_HUMAN_REVIEW', ''))
    }
    Set-TabularSheet -Workbook $workbook -Name 'Questions' -Title 'Question records | source-backed, pending pedagogic review' -Headers @('Item code','Question type','Target word','Prompt','Correct answer','Source evidence','Required','Weight','Transfer category','Pedagogic decision','Reviewer notes') -Rows $questionRows -ColumnWidths @(18,32,23,64,22,46,11,10,19,27,36) | Out-Null

    $optionRows = [System.Collections.Generic.List[object]]::new()
    foreach ($option in $options) {
        $optionRows.Add([object[]]@($option.itemCode, $option.optionCode, $option.optionText, $option.correct, $option.explanation, 'PENDING_HUMAN_REVIEW', ''))
    }
    Set-TabularSheet -Workbook $workbook -Name 'Options' -Title 'Option records | source-backed, pending pedagogic review' -Headers @('Item code','Option','Option text','Correct','Generated explanation','Pedagogic decision','Reviewer notes') -Rows $optionRows -ColumnWidths @(18,10,58,10,52,27,36) | Out-Null

    $workbook.Worksheets.Item('Overview').Activate() | Out-Null
    $workbook.SaveAs($workbookPath, 51)
}
finally {
    if ($null -ne $workbook) { try { $workbook.Close($true) } catch {} }
    if ($null -ne $excel) { try { $excel.Quit() } catch {} }
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}

# Build a Word packet from one source HTML document to keep its long appendices
# stable and paginated when Word converts it to DOCX.
$html = [System.Text.StringBuilder]::new()
[void]$html.AppendLine('<!DOCTYPE html><html><head><meta charset="utf-8"><style>')
[void]$html.AppendLine('@page { size: A4 landscape; margin: 1.25cm; } body { font-family: Aptos, Arial, sans-serif; color: #24313b; font-size: 9pt; } h1 { color: #1c4d5b; font-size: 24pt; margin: 0 0 6pt; } h2 { color: #1c4d5b; font-size: 15pt; margin: 18pt 0 8pt; page-break-after: avoid; } p { line-height: 1.35; } .meta { color: #5a6770; margin: 0 0 14pt; } .notice { background: #fff1d6; border-left: 4px solid #ce8a25; padding: 8pt; margin: 12pt 0; } table { width: 100%; border-collapse: collapse; margin: 8pt 0 16pt; } th { background: #326575; color: #fff; font-weight: bold; text-align: left; } th, td { border: 1px solid #cdd6da; padding: 4pt; vertical-align: top; word-wrap: break-word; } tr { page-break-inside: avoid; } .small { font-size: 8pt; } .break { page-break-before: always; }</style></head><body>')
[void]$html.AppendLine('<h1>Lexi-Bridge FF4 review packet</h1>')
[void]$html.AppendLine('<p class="meta">Internal reader for manual pedagogic review | Questionnaire: ' + (HtmlEncode $package.Questionnaire.code) + ' | Generated from the current zero-issue structural package</p>')
[void]$html.AppendLine('<div class="notice"><strong>Release boundary.</strong> Structural audit: ' + (HtmlEncode $report.auditStatus) + '. Pedagogic review: ' + (HtmlEncode $report.pedagogicReview) + '. Publication: ' + (HtmlEncode $report.publicationStatus) + '. No database import, commit, or publication is authorized by this packet.</div>')
[void]$html.AppendLine('<h2>Package summary</h2><table><tr><th>Words</th><th>Questions</th><th>Options</th><th>Question mix</th><th>Source evidence</th></tr>')
[void]$html.AppendLine('<tr><td>' + $words.Count + '</td><td>' + $items.Count + '</td><td>' + $options.Count + '</td><td>112 single-choice; 111 each fill-blank, true/false and short-text</td><td>Each record carries false-friends and TEM4 PDF page references.</td></tr></table>')
[void]$html.AppendLine('<p><strong>Review ordering:</strong> ' + $criticalReviewCount + ' critical records are listed in the workbook review queue. Priority is a routing aid, not a correctness verdict.</p>')
[void]$html.AppendLine('<h2>Reviewer instructions</h2><ol><li>Verify the French headword, Chinese gloss and false-friend distinction against the cited source pages.</li><li>Check prompt language, distractor plausibility and answer key; record a decision in the workbook.</li><li>Resolve every revise/reject item before any preflight or database action.</li></ol>')
[void]$html.AppendLine('<h2 class="break">Vocabulary appendix</h2><table class="small"><tr><th>Word ID</th><th>French</th><th>English confusable</th><th>Chinese gloss</th><th>English-source meaning</th><th>TEM4</th><th>False-friends</th><th>Decision</th></tr>')
foreach ($word in $words) {
    [void]$html.AppendLine('<tr><td>' + (HtmlEncode $word.word_id) + '</td><td>' + (HtmlEncode $word.french_word) + '</td><td>' + (HtmlEncode $word.english_word) + '</td><td>' + (HtmlEncode $word.chinese_gloss) + '</td><td>' + (HtmlEncode $word.false_friend_meanings) + '</td><td>p.' + (HtmlEncode $word.tem4_pdf_page) + '</td><td>p.' + (HtmlEncode $word.false_friend_pdf_page) + '</td><td>PENDING</td></tr>')
}
[void]$html.AppendLine('</table><h2 class="break">Question appendix</h2><table class="small"><tr><th>Item</th><th>Type</th><th>Target word</th><th>Prompt</th><th>Correct answer</th><th>Source evidence</th><th>Decision</th></tr>')
foreach ($item in $items) {
    [void]$html.AppendLine('<tr><td>' + (HtmlEncode $item.itemCode) + '</td><td>' + (HtmlEncode $item.questionType) + '</td><td>' + (HtmlEncode $item.targetWord) + '</td><td>' + (HtmlEncode $item.stemText) + '</td><td>' + (HtmlEncode ((@($item.correctAnswers)) -join ' | ')) + '</td><td>' + (HtmlEncode $item.explanationText) + '</td><td>PENDING</td></tr>')
}
[void]$html.AppendLine('</table></body></html>')
[System.IO.File]::WriteAllText($htmlPath, $html.ToString(), [System.Text.UTF8Encoding]::new($false))

$word = $null
$document = $null
try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $document = $word.Documents.Open($htmlPath, $false, $true)
    $document.PageSetup.Orientation = 1
    $document.PageSetup.TopMargin = 35
    $document.PageSetup.BottomMargin = 35
    $document.PageSetup.LeftMargin = 35
    $document.PageSetup.RightMargin = 35
    $document.Sections.Item(1).Headers.Item(1).Range.Text = 'Lexi-Bridge FF4 | Internal manual review'
    $document.Sections.Item(1).Footers.Item(1).Range.Text = 'Pending human pedagogic review — no database import or publication'
    $document.SaveAs2($documentPath, 16)
    $document.ExportAsFixedFormat($pdfPath, 17)
}
finally {
    if ($null -ne $document) { try { $document.Close($false) } catch {} }
    if ($null -ne $word) { try { $word.Quit() } catch {} }
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}

if (-not (Test-Path -LiteralPath $workbookPath) -or -not (Test-Path -LiteralPath $documentPath) -or -not (Test-Path -LiteralPath $pdfPath)) {
    throw 'One or more reader artifacts were not written.'
}

"reader_artifacts=created; xlsx=$workbookPath; docx=$documentPath; pdf=$pdfPath"
