# Lexi-Bridge FF4 content-review findings

## Verified structural evidence

- All 203 false-friends PDF pages and all 385 TEM4 PDF pages have an explicit
  processing status.
- The candidate tables contain visual-headword records and preserve source-page
  evidence.
- The generated wordbook contains 445 normalized, de-duplicated dual-source
  records.
- The question package has 445 distinct target words and balanced type counts
  (112 single-choice; 111 each fill-blank, true/false and short-text).
- The reproducible structural audit now also checks unique codes and targets,
  required word fields and source pages, exact source-page explanations,
  question/section alignment, answer keys, option cardinality and codes, and
  orphaned options. The current rebuilt package passes with zero structural
  issues.
- Sixty generated true/false assertions overlapped a verified French meaning
  while being keyed `FALSE`. Their source-claim wording was replaced with a
  non-overlapping in-package claim and each replacement is retained in
  `true-false-source-equality-fallbacks.json`; the audit verifies the ledger
  covers every such template case. This is a generation correction, not a
  pedagogic approval.
- `pedagogic-review-queue.json` and `.csv` rank every item for manual review:
  62 critical, 39 high, 15 medium and 329 normal. Priority is only a review
  routing signal; every record remains pending.

## Publication blockers

1. Automated package construction has not replaced the required word-by-word
   pedagogic review of Chinese glosses, original examples, distractors and
   explanations. Existing records retain source evidence but not a completed
   authoring review.
2. No executable confirmation has established that the effective database is a
   recoverable local/development target. Therefore no database import, commit,
   or publication has been performed.
3. The formatted reader artifacts have been generated as
   `reader-artifacts/lexi-bridge-ff4-review-workbook.xlsx` and
   `reader-artifacts/lexi-bridge-ff4-review-packet.docx`. They are confirmed
   to retain the pending-review and no-publication boundary; their existence
   does not close the pedagogic or database-safety blockers above.

These are release blockers, not source-reading blockers. No record is marked
as published by this finding.
