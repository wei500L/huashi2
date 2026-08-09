# Lexi-Bridge FF4 import runbook

## Scope

The package is derived from visual checks of the Kirk-Greene false-friends source
and the TEM4 core vocabulary source. Stable lexical identity uses source code,
content version, and word ID.

## Files

- docs/data/lexi-bridge-ff4/lexical-import.csv
- docs/data/lexi-bridge-ff4/question-bank-package.json
- docs/data/lexi-bridge-ff4/review-report.json
- docs/data/lexi-bridge-ff4/reader-artifacts/lexi-bridge-ff4-review-workbook.xlsx
- docs/data/lexi-bridge-ff4/reader-artifacts/lexi-bridge-ff4-review-packet.docx

## Required local-development gate

Do not write data until effective runtime values establish a local recoverable
database. Confirm a backup or disposable development database before import.

## Lexical import workflow

1. Upload lexical-import.csv through the existing Lexical Import Center.
2. Review parser/preflight results and correct rejected rows in the staged batch.
3. Commit the batch using the existing import action.
4. Re-upload the same file to confirm idempotency.

## Question bank workflow

1. Submit question-bank-package.json to the existing question-bank preflight endpoint.
2. Resolve every preflight issue.
3. Commit only with explicit confirmation.
4. Verify questionnaire, type counts, source-page evidence, unique target words,
   and publication status through the existing admin flow.

No database write or publication is recorded by this runbook.

## Reader-artifact review gate

The workbook is filterable by source fields, generated item fields and the
manual review decision, with a Review Queue sheet that orders the 62 critical
items before the lower-priority records. The Word packet is a paginated
reading copy of the same word and question data. Both explicitly remain in
`PENDING_HUMAN_REVIEW`; they are review aids, not approval records.

Before preflight, a reviewer must resolve the decision and note fields for
each item that needs changes. Re-run the three generation/audit scripts after
any source-table correction, then regenerate the reader artifacts. Do not
interpret the structural pass as authorization to import or publish.
