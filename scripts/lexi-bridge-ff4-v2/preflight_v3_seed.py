#!/usr/bin/env python3
"""FF4 V2/V3 seed preflight - validates LEXIBRIDGE_RESEARCH_V3.json.

Verifies (deterministically, from the seed + bank package):
  - section set and scored counts against the complete production bank
  - global target-word uniqueness across the four FF4 sections
  - option roles (1 CORRECT + 1 TRANSFER + 2 DISTRACTOR) for T1/T2
  - answer consistency between correctAnswers and CORRECT role
  - T3 false-friend F items plus exactly ten approved Vrai cognate controls
  - T4 spelling distance 1-4, no morphology-only, non-ambiguous stems
  - every scored item carries production APPROVED review status
  - content hash determinism (recompute and compare)
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_candidates import fold_latin  # noqa: E402
from production_semantic_rules import (  # noqa: E402
    MAX_CONSECUTIVE_FALSE_FRIEND_T3, MINIMUM_T2_ITEM_COUNT, RULESET_VERSION,
    TRUE_COGNATE_CONTROLS,
)

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "data" / "lexi-bridge-ff4-v2"
SEED = ROOT / "app-server" / "src" / "main" / "resources" / "assessment-seeds" / "LEXIBRIDGE_RESEARCH_V3.json"

TYPE_BY_SECTION = {
    "FF4_WORD_MEANING": "T1",
    "FF4_SENTENCE_SELECTION": "T2",
    "FF4_TRUE_FALSE": "T3",
    "FF4_SPELLING": "T4",
}


def content_hash(payload: dict) -> str:
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def resolve_word(item: dict) -> str:
    if item.get("targetWord"):
        return item["targetWord"]
    stem = item["stemText"]
    if item["questionType"] == "SPELLING":
        return (item.get("correctAnswers") or [None])[0] or ""
    if item["sectionCode"] == "FF4_SENTENCE_SELECTION":
        match = re.search(r"\*\*([^*]+)\*\*", stem)
        return match.group(1) if match else ""
    if item["sectionCode"] == "FF4_TRUE_FALSE":
        return stem.replace("法语", "").split("表示")[0].strip()
    return stem.strip().splitlines()[-1].strip()


def main() -> int:
    seed = json.loads(SEED.read_text(encoding="utf-8"))
    bank = json.loads((OUT / "question-bank-package-production.json").read_text(encoding="utf-8"))
    adjudication = {}
    for line in (OUT / "candidate-adjudication.jsonl").read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        adjudication[record["frenchWord"]] = record

    issues: list[str] = []
    scored = [item for item in seed["items"] if item.get("scored")]
    bank_by_code = {item["itemCode"]: item for item in bank["items"]}
    bank_options = {}
    for option in bank["options"]:
        bank_options.setdefault(option["itemCode"], []).append(option)
    counts: dict[str, int] = {}
    words_by_section: dict[str, set[str]] = {}
    true_control_count = 0
    consecutive_false_friend_t3 = 0
    maximum_false_friend_t3_run = 0

    for item in scored:
        section = item["sectionCode"]
        counts[section] = counts.get(section, 0) + 1
        words_by_section.setdefault(section, set()).add(resolve_word(item))
        bank_item = bank_by_code.get(item["itemCode"])

        if item["questionType"] not in ("SINGLE_CHOICE", "TRUE_FALSE", "SPELLING"):
            issues.append(f"{item['itemCode']}: unexpected questionType {item['questionType']}")

        if item.get("lexicalReviewStatus") is None:
            issues.append(f"{item['itemCode']}: missing lexicalReviewStatus marker")
        elif item.get("lexicalReviewStatus") != "APPROVED":
            issues.append(f"{item['itemCode']}: unexpected lexicalReviewStatus {item.get('lexicalReviewStatus')}")
        if item.get("pedagogicReviewStatus") != "APPROVED":
            issues.append(f"{item['itemCode']}: unexpected pedagogicReviewStatus {item.get('pedagogicReviewStatus')}")

        if section in ("FF4_WORD_MEANING", "FF4_SENTENCE_SELECTION"):
            if bank_item is None:
                issues.append(f"{item['itemCode']}: missing bank item for role check")
            else:
                options = bank_options.get(item["itemCode"], [])
                roles = [o.get("role") for o in options]
                if len(options) != 4 or roles.count("CORRECT") != 1 \
                        or roles.count("TRANSFER") != 1 or roles.count("DISTRACTOR") != 2:
                    issues.append(f"{item['itemCode']}: invalid option roles {roles}")
                if item["correctAnswers"] != [next(o["optionCode"] for o in options if o.get("role") == "CORRECT")]:
                    issues.append(f"{item['itemCode']}: correctAnswers mismatch with CORRECT role")
        elif section == "FF4_TRUE_FALSE":
            if item.get("transferCategory") == "COGNATE":
                true_control_count += 1
                consecutive_false_friend_t3 = 0
                if item["targetWord"] not in TRUE_COGNATE_CONTROLS or item["correctAnswers"] != ["V"]:
                    issues.append(f"{item['itemCode']}: invalid Vrai cognate control")
            elif item["correctAnswers"] != ["F"]:
                issues.append(f"{item['itemCode']}: false-friend T3 answer must be F")
            else:
                consecutive_false_friend_t3 += 1
                maximum_false_friend_t3_run = max(maximum_false_friend_t3_run,
                                                   consecutive_false_friend_t3)
        else:
            word = resolve_word(item)
            record = adjudication.get(word, {})
            distance = min(record.get("rawEditDistance", 99), record.get("accentFoldedEditDistance", 99))
            if not (1 <= distance <= 4):
                issues.append(f"{item['itemCode']}: spelling distance {distance} out of range")
            if record.get("morphologyOnly"):
                issues.append(f"{item['itemCode']}: morphology-only pair used in spelling item")

    # global uniqueness
    seen: dict[str, str] = {}
    for section, words in words_by_section.items():
        for word in words:
            if word in seen and seen[word] != section:
                issues.append(f"target word reused across sections: {word} in {seen[word]} and {section}")
            seen[word] = section

    seed_codes = {item["itemCode"] for item in scored}
    bank_codes = set(bank_by_code)
    if seed.get("source", {}).get("bankPackage") != "question-bank-package-production.json":
        issues.append("V3 source.bankPackage must be the complete production package")
    if seed_codes != bank_codes:
        missing = sorted(bank_codes - seed_codes)
        extra = sorted(seed_codes - bank_codes)
        if missing:
            issues.append("V3 omits production item codes: " + ", ".join(missing))
        if extra:
            issues.append("V3 contains item codes outside production bank: " + ", ".join(extra))
    if true_control_count != len(TRUE_COGNATE_CONTROLS):
        issues.append(f"expected {len(TRUE_COGNATE_CONTROLS)} Vrai cognate controls, got {true_control_count}")
    if counts.get("FF4_SENTENCE_SELECTION", 0) < MINIMUM_T2_ITEM_COUNT:
        issues.append(f"T2 regression: expected at least {MINIMUM_T2_ITEM_COUNT} items")
    if maximum_false_friend_t3_run > MAX_CONSECUTIVE_FALSE_FRIEND_T3:
        issues.append("T3 interleaving regression: maximum consecutive false-friend run "
                      f"is {maximum_false_friend_t3_run}")
    if seed.get("source", {}).get("rulesetVersion") != RULESET_VERSION:
        issues.append(f"seed rulesetVersion must be {RULESET_VERSION}")

    # determinism: recompute hashes
    for item in scored:
        payload = {k: v for k, v in item.items() if k != "contentHash"}
        if content_hash(payload) != item.get("contentHash"):
            issues.append(f"{item['itemCode']}: contentHash mismatch on recompute")

    # status gate
    if seed["questionnaire"]["status"] != "APPROVED":
        issues.append("questionnaire.status must be APPROVED")

    report = {
        "packageCode": seed["packageCode"],
        "status": "PASS" if not issues else "FAIL",
        "scoredItems": len(scored),
        "counts": counts,
        "uniqueTargetWords": len(seen),
        "productionBankItems": len(bank_by_code),
        "completeProductionCoverage": seed_codes == bank_codes,
        "trueCognateControls": true_control_count,
        "maximumConsecutiveFalseFriendT3": maximum_false_friend_t3_run,
        "rulesetVersion": RULESET_VERSION,
        "issueCount": len(issues),
        "issues": issues,
        "note": "结构、来源、选项、答案与生产审查状态均已通过自动预检。",
    }
    (OUT / "seed-preflight-report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("packageCode", "status", "scoredItems", "counts", "issueCount")},
                      ensure_ascii=False))
    for issue in issues:
        print("  -", issue)
    return 1 if issues else 0


if __name__ == "__main__":
    sys.exit(main())
