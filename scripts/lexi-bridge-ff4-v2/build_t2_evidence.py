#!/usr/bin/env python3
"""FF4 V2 - Type 2 (sentence selection) evidence extractor.

Type 2 requires, for every item, three source-backed elements:
  1. a complete locatable TEM4 example sentence (already in candidate-adjudication)
  2. a French near-synonym that can replace the target in that sentence
     (evidence: TEM4 '= X' synonym lines or Kirk-Greene French synonyms)
  3. a French transfer distractor expressing the English confusable's meaning
     (evidence: Kirk-Greene 'English = French' equivalence lines)

Nothing here invents synonyms or glosses: only OCR text that appears on the
cited source pages is accepted, and every extracted candidate carries the
exact source line it came from. Words without sufficient evidence stay
ineligible for Type 2 (SOURCE_EXAMPLE_MISSING / SYNONYM_EVIDENCE_MISSING /
TRANSFER_EVIDENCE_MISSING).

Outputs:
  docs/data/lexi-bridge-ff4-v2/t2-evidence.jsonl
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_candidates import ROOT, fold_latin

DATA = ROOT / "docs" / "data" / "lexi-bridge-ff4"
OUT = ROOT / "docs" / "data" / "lexi-bridge-ff4-v2"
OCR_TEM4 = OUT / "source-ocr-cache" / "tem4-pages.jsonl"
OCR_FF = DATA / "source-ocr-cache" / "false-friends-pages.jsonl"

FR_WORD = r"[a-zA-Zàâäéèêëîïôöùûüçœæ'’][a-zA-Zàâäéèêëîïôöùûüçœæ'’-]*"


def load_jsonl(path: Path) -> list[dict]:
    rows = []
    with path.open(encoding="utf-8-sig") as handle:
        for line in handle:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows


def load_pages(path: Path, key_prefix: str | None = None) -> dict[int, str]:
    pages: dict[int, str] = {}
    for row in load_jsonl(path):
        if key_prefix:
            match = re.search(key_prefix + r"(\d+)", row.get("path") or "")
            if match:
                pages[int(match.group(1))] = row.get("text") or ""
        else:
            pages[row["pdf_page"]] = row.get("text") or ""
    return pages


def extract_tem4_synonyms(page_lines: list[str], sentence: str, french_word: str) -> list[dict]:
    """Find '= X' synonym lines for the target word's TEM4 example.

    The TEM4 book marks synonym equivalences as '... = X' (e.g. 'Ça vaut la
    peine ... = valoir le coup de' or 'On le presse de partir. = pousser qn.').
    We scan a tight window around the located example sentence and only accept
    lines whose left side contains the target word, the '~' placeholder, or
    that sit directly after the sentence line.
    """
    folded = fold_latin(french_word)
    sentence_index = next(
        (i for i, line in enumerate(page_lines) if fold_latin(sentence)[:40] in fold_latin(line)),
        None,
    )
    if sentence_index is None:
        return []
    results = []
    window = range(max(0, sentence_index - 2), min(len(page_lines), sentence_index + 6))
    for index in window:
        line = page_lines[index]
        if "=" not in line:
            continue
        left_part = re.split(r"=", line)[0]
        left_hit = folded[:4] in fold_latin(left_part) or "~" in left_part
        adjacent = index in (sentence_index + 1, sentence_index + 2)
        if not left_hit and not adjacent:
            continue
        match = re.search(r"=\s*(" + FR_WORD + r"(?:\s+[a-zA-Zàâäéèêëîïôöùûüçœæ'’-]+){0,5})\s*(?:[,;.]|$)", line)
        if not match:
            match = re.search(r"=\s*(" + FR_WORD + r"(?:\s+[a-zA-Zàâäéèêëîïôöùûüçœæ'’-]+){0,5})\s*(?:[,;.\n]|$)", line)
        if not match:
            continue
        synonym = match.group(1).strip()
        synonym = re.sub(r"\s+(de|à|du|des|la|le|les|au|aux|en|que|qui)\s*$", "", synonym).strip()
        if re.search(r"\d|‰|\$|anti|\-$|^\-", synonym, re.IGNORECASE) or len(synonym) < 3:
            continue
        if fold_latin(synonym) == folded:
            continue
        if not left_hit and not adjacent:
            continue
        results.append({"synonym": synonym, "sourceLine": line.strip(), "page": None})
    seen = set()
    unique = []
    for result in results:
        key = fold_latin(result["synonym"])
        if key not in seen:
            seen.add(key)
            unique.append(result)
    return unique


def _trim_entry_region(entry_text: str, english_word: str) -> str:
    """Cut an FF entry region from the confusable's line to the next headword.

    The region starts at the line containing the English confusable (its own
    headword line or the line that introduces it) and stops at the next
    headword-like paragraph, so equivalence patterns cannot bleed across
    neighbouring dictionary entries.
    """
    lines = entry_text.split("\n")
    start = next((i for i, line in enumerate(lines)
                  if re.search(r"\b" + re.escape(english_word.strip()) + r"\b", line, re.IGNORECASE)), 0)
    kept = [lines[start]]
    for line in lines[start + 1:]:
        if re.match(r"^[a-zà-ÿ][a-zà-ÿ'’\-]*\s*(?:\(m\)|\(f\)|\(m/f\)|v\.|To\s|to\s)", line):
            break
        kept.append(line)
    return "\n".join(kept)


def extract_ff_equivalences(entry_text: str, english_word: str) -> list[dict]:
    """Find Kirk-Greene 'English = French' equivalences for the confusable.

    Example: 'Sympathetic = compatissant.' or 'pain (= douleur (f))' or
    'To remark (often) = dire.'
    """
    results = []
    en = english_word.strip().lower()
    region = _trim_entry_region(entry_text, english_word)
    patterns = [
        (r"(?:^|\b)" + re.escape(en) + r"\b[^(]*?\(=\s*(" + FR_WORD + r")\s*(?:\([^)]*\))?\s*\)", "PAREN_EQUIV"),
        (r"(?:^|\b)" + re.escape(en) + r"\b\s*=\s*(" + FR_WORD + r"(?:\s+[a-zA-Zàâäéèêëîïôöùûüçœæ'’-]+){0,4})\s*\.?", "EQUALS"),
        (r"(?:^|\b)to\s+" + re.escape(en) + r"\b[^=;\n]{0,80}?\s*=\s*(" + FR_WORD + r"(?:\s+[a-zA-Zàâäéèêëîïôöùûüçœæ'’-]+){0,4})\s*\.?", "TO_EQUALS"),
        (r"(?:^|\b)" + re.escape(en) + r"\b[^=;\n]{0,80}?\(=\s*(" + FR_WORD + r")\s*\)", "INLINE_PAREN"),
    ]
    for pattern, kind in patterns:
        for match in re.finditer(pattern, region, re.IGNORECASE):
            results.append({"french": match.group(1), "kind": kind,
                            "sourceLine": "…" + region[max(0, match.start() - 40): match.end() + 20].replace("\n", " ")})
    seen = set()
    unique = []
    for result in results:
        key = fold_latin(result["french"])
        if key not in seen:
            seen.add(key)
            unique.append(result)
    return unique


def build() -> list[dict]:
    candidates = load_jsonl(OUT / "candidate-adjudication.jsonl")
    tem4_pages = load_pages(OCR_TEM4)
    ff_pages = load_pages(OCR_FF, key_prefix=r"ff-")

    records = []
    for candidate in candidates:
        if candidate["exampleSentenceStatus"] != "SOURCE_VERIFIED":
            candidate["t2SynonymEvidence"] = []
            candidate["t2TransferEvidence"] = []
            candidate["t2EvidenceStatus"] = "SOURCE_EXAMPLE_MISSING"
            records.append(candidate)
            continue
        page_lines = tem4_pages.get(candidate["tem4PdfPage"], "").split("\n")
        synonyms = extract_tem4_synonyms(page_lines, candidate["tem4ExampleSentence"], candidate["frenchWord"])

        ff_entry = ff_pages.get(candidate["falseFriendsPdfPage"], "")
        idx = ff_entry.lower().find(candidate["englishConfusable"].lower())
        entry_region = ff_entry[max(0, idx - 80): idx + 700] if idx >= 0 else ff_entry
        transfers = extract_ff_equivalences(entry_region, candidate["englishConfusable"])

        candidate["t2SynonymEvidence"] = synonyms
        candidate["t2TransferEvidence"] = transfers
        if not synonyms:
            candidate["t2EvidenceStatus"] = "SYNONYM_EVIDENCE_MISSING"
        elif not transfers:
            candidate["t2EvidenceStatus"] = "TRANSFER_EVIDENCE_MISSING"
        else:
            candidate["t2EvidenceStatus"] = "EVIDENCE_READY"
        records.append(candidate)

    with (OUT / "t2-evidence.jsonl").open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")

    ready = [r for r in records if r["t2EvidenceStatus"] == "EVIDENCE_READY"]
    missing_syn = [r for r in records if r["t2EvidenceStatus"] == "SYNONYM_EVIDENCE_MISSING"]
    missing_transfer = [r for r in records if r["t2EvidenceStatus"] == "TRANSFER_EVIDENCE_MISSING"]
    summary = {
        "evidenceReady": len(ready),
        "synonymMissing": len(missing_syn),
        "transferMissing": len(missing_transfer),
        "readyWords": [r["frenchWord"] for r in ready],
    }
    (OUT / "t2-evidence-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False))
    return records


if __name__ == "__main__":
    build()
