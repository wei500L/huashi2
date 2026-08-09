# Lexi-Bridge FF4 reader-artifact handoff

## Current verified inputs

- `wordbook.csv` and `wordbook.jsonl`: 445 dual-source word records.
- `question-bank-package.json`: 445 items and 670 options.
- `review-report.json`: structural pass only; pedagogic review remains pending
  and publication is not eligible.
- `pedagogic-review-queue.json`: 445 ordered review records, retaining risk
  reasons, source-page evidence notes, and any original/replacement true-false
  claims without assigning approval.

Run the following before producing a reader artifact so it is synchronized
with the source candidate tables:

```powershell
./scripts/build-lexi-bridge-ff4-wordbook.ps1
./scripts/build-lexi-bridge-ff4-question-bank.ps1
./scripts/audit-lexi-bridge-ff4-package.ps1
./scripts/build-lexi-bridge-ff4-pedagogic-review-queue.ps1
./scripts/build-lexi-bridge-ff4-reader-artifacts.ps1
```

## Created deliverables

1. `reader-artifacts/lexi-bridge-ff4-review-workbook.xlsx`: a formatted
   workbook with a summary sheet plus filterable
   word, question and option sheets. It includes manual-review decision and
   note columns without changing the generated source fields.
2. `reader-artifacts/lexi-bridge-ff4-review-packet.docx`: a paginated review
   packet that identifies the questionnaire,
   provenance, structural counts, release blockers, and a paginated word and
   question appendix. A matching PDF export was produced for render review.

Do not mark either artifact as pedagogically approved, published, or imported.
The existing `wordbook.csv` is a source table that Excel can open, not the
requested formatted workbook.

## Verification

- Excel reopened the workbook with the expected sheets and data ranges:
  Review Queue `A1:P447`, Vocabulary `A1:L447`, Questions `A1:K447`, Options
  `A1:G672`.
- Word reopened the packet as 41 pages with three tables, and the PDF export
  was written successfully.
- Visual inspection covered the workbook overview and vocabulary sheets plus
  the source-stable review-packet layout. No database access was attempted.
