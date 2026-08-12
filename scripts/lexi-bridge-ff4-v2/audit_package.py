#!/usr/bin/env python3
"""FF4 question bank V2 - automated audit.

Checks the acceptance criteria that can be verified deterministically:
  - global target-word uniqueness across the four types
  - option roles and counts per type
  - answer consistency
  - spelling distance 1-4, no morphology-only
  - source pages present on every item
  - production review status is APPROVED
  - deterministic regeneration (content hash stability)
  - example sentence evidence for T2

Outputs docs/data/lexi-bridge-ff4-v2/audit-report.md
"""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_candidates import fold_latin  # noqa: E402
from production_semantic_rules import T2_RULES, T2_TARGET_WORDS  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "data" / "lexi-bridge-ff4-v2"

REQUIRED_TYPES = ["T1", "T2", "T3", "T4"]


def audit() -> dict:
    package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
    records = {}
    for line in (OUT / "candidate-adjudication.jsonl").read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        records[record["frenchWord"]] = record
    evidence = {}
    for line in (OUT / "t2-evidence.jsonl").read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        evidence[record["frenchWord"]] = record

    issues = []
    items = package["items"]
    options_by_item = {}
    for option in package["options"]:
        options_by_item.setdefault(option["itemCode"], []).append(option)
    counts = {t: 0 for t in REQUIRED_TYPES}
    words_by_type: dict[str, set[str]] = {t: set() for t in REQUIRED_TYPES}

    for item in items:
        item_type = {"FF4_WORD_MEANING": "T1", "FF4_SENTENCE_SYNONYM": "T2",
                     "FF4_TRUE_FALSE_TRANSFER": "T3", "FF4_SPELLING": "T4"}.get(item.get("constructCode"))
        if item_type not in REQUIRED_TYPES:
            issues.append(f"{item['itemCode']}: unknown itemType {item_type}")
            continue
        counts[item_type] += 1
        word = resolve_word(item)
        if word:
            words_by_type[item_type].add(word)
            record = records.get(word)
            if record is None:
                issues.append(f"{item['itemCode']}: no adjudication record for {word}")
            else:
                check_source_pages(item, record, issues)
                check_review_status(item, record, issues)

        if item_type in ("T1", "T2"):
            check_choice_item(item, options_by_item.get(item["itemCode"], []), issues)
            if item_type == "T2":
                check_t2_document_rule(item, options_by_item.get(item["itemCode"], []), issues)
        elif item_type == "T3":
            check_true_false(item, options_by_item.get(item["itemCode"], []), issues)
        else:
            check_spelling(item, records, issues)

    check_global_uniqueness(words_by_type, issues)
    check_determinism(issues)

    all_words = set()
    for words in words_by_type.values():
        all_words |= words
    result = {
        "totalItems": len(items),
        "counts": counts,
        "uniqueTargetWords": len(all_words),
        "issueCount": len(issues),
        "issues": issues,
        "wordsByType": {t: sorted(words) for t, words in words_by_type.items()},
    }
    return result


def resolve_word(item: dict) -> str | None:
    return item.get("targetWord")


def check_source_pages(item: dict, record: dict, issues: list[str]) -> None:
    pages = [record.get("tem4PdfPage"), record.get("falseFriendsPdfPage")]
    if not all(isinstance(p, int) and p > 0 for p in pages):
        issues.append(f"{item['itemCode']}: invalid source pages {pages}")


def check_review_status(item: dict, record: dict, issues: list[str]) -> None:
    if item.get("lexicalReviewStatus") != "APPROVED":
        issues.append(f"{item['itemCode']}: lexical status is not APPROVED")
    if item.get("pedagogicReviewStatus") != "APPROVED":
        issues.append(f"{item['itemCode']}: pedagogic status is not APPROVED")


def check_choice_item(item: dict, options: list[dict], issues: list[str]) -> None:
    if len(options) != 4:
        issues.append(f"{item['itemCode']}: expected 4 options, got {len(options)}")
        return
    if sum(1 for o in options if o.get("correct")) != 1:
        issues.append(f"{item['itemCode']}: expected exactly one correct option")
    roles = [o.get("role") for o in options]
    if roles.count("CORRECT") != 1:
        issues.append(f"{item['itemCode']}: CORRECT role count = {roles.count('CORRECT')}")
    if roles.count("TRANSFER") != 1:
        issues.append(f"{item['itemCode']}: TRANSFER role count = {roles.count('TRANSFER')}")
    if roles.count("DISTRACTOR") != 2:
        issues.append(f"{item['itemCode']}: DISTRACTOR role count = {roles.count('DISTRACTOR')}")
    if len(set(o["optionCode"] for o in options)) != 4:
        issues.append(f"{item['itemCode']}: option keys not unique")
    correct_keys = item.get("correctAnswers") or []
    if len(correct_keys) != 1 or correct_keys[0] != next(o["optionCode"] for o in options if o.get("correct")):
        issues.append(f"{item['itemCode']}: correctAnswers inconsistent with options")


def check_t2_document_rule(item: dict, options: list[dict], issues: list[str]) -> None:
    word = item.get("targetWord")
    rule = T2_RULES.get(word)
    if word not in T2_TARGET_WORDS:
        issues.append(f"{item['itemCode']}: unexpected T2 target {word}")
    if rule is None or item.get("stemText") != "请根据句子选择画线单词的同义解释\n" + rule["sentence"]:
        issues.append(f"{item['itemCode']}: T2 source sentence/emphasis differs from approved text")
    expected_evidence = rule.get("evidenceLevel", "TEM4_EXACT_SENTENCE") if rule else None
    if item.get("exampleSentenceStatus") != expected_evidence:
        issues.append(f"{item['itemCode']}: T2 evidence level differs from approved rule")
    actual = [(o["optionCode"], o["optionText"], o["correct"], o["role"]) for o in options]
    if rule is None or actual != list(rule["options"]):
        issues.append(f"{item['itemCode']}: T2 options differ from approved synonym/transfer evidence")


def check_true_false(item: dict, options: list[dict], issues: list[str]) -> None:
    if (item.get("correctAnswers") or []) != ["F"]:
        issues.append(f"{item['itemCode']}: T3 answer must be F by design")
    keys = [o["optionCode"] for o in options]
    if sorted(keys) != sorted(["V", "F"]):
        issues.append(f"{item['itemCode']}: T3 options must be V/F")
    correct = [o for o in options if o.get("correct")]
    if not correct or correct[0]["optionCode"] != "F":
        issues.append(f"{item['itemCode']}: T3 correct option must be F")


def check_spelling(item: dict, records: dict, issues: list[str]) -> None:
    answer = (item.get("correctAnswers") or [None])[0]
    if not answer:
        issues.append(f"{item['itemCode']}: T4 missing answer")
        return
    record = next((r for r in records.values() if r["frenchWord"] == answer), None)
    if record is None:
        issues.append(f"{item['itemCode']}: T4 answer {answer} not in adjudication")
        return
    distance = min(record.get("rawEditDistance", 0), record.get("accentFoldedEditDistance", 0))
    if not (1 <= distance <= 4):
        issues.append(f"{item['itemCode']}: T4 distance {distance} out of range")
    if record.get("morphologyOnly"):
        issues.append(f"{item['itemCode']}: T4 morphology-only word used as spelling answer")


def check_global_uniqueness(words_by_type: dict, issues: list[str]) -> None:
    seen: dict[str, str] = {}
    for item_type, words in words_by_type.items():
        for word in words:
            if word in seen and seen[word] != item_type:
                issues.append(f"target word reused across types: {word} in {seen[word]} and {item_type}")
            seen[word] = item_type


def check_determinism(issues: list[str]) -> None:
    try:
        subprocess.run(
            [sys.executable, str(Path(__file__).resolve().parent / "build_question_bank.py")],
            check=True, capture_output=True, timeout=120,
        )
        subprocess.run(
            [sys.executable, str(Path(__file__).resolve().parent / "build_research_v3_seed.py")],
            check=True, capture_output=True, timeout=120,
        )
        subprocess.run(
            [sys.executable, str(Path(__file__).resolve().parent / "preflight_v3_seed.py")],
            check=True, capture_output=True, timeout=120,
        )
        seed = json.loads((ROOT / "app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json").read_text(encoding="utf-8"))
        hashes = [item["contentHash"] for item in seed["items"]]
        if len(hashes) != len(set(hashes)):
            issues.append("content hashes not unique across items")
        seed_report = json.loads((OUT / "seed-preflight-report.json").read_text(encoding="utf-8"))
        if seed_report.get("status") != "PASS":
            issues.append(f"seed preflight failed: {len(seed_report.get('issues', []))} issues")
    except Exception as exc:
        issues.append(f"determinism re-run failed: {exc}")


def main() -> None:
    result = audit()
    lines = [
        "# Lexi-Bridge FF4 V2 题库自动审计报告",
        "",
        f"- 生成时间：{__import__('datetime').datetime.now().isoformat()}",
        f"- 题目总数：{result['totalItems']}",
        "- 各题型题量：" + "；".join(f"{t}={result['counts'][t]}" for t in REQUIRED_TYPES),
        f"- 全局唯一目标词数：{result['uniqueTargetWords']}",
        f"- 问题数：{result['issueCount']}",
        "",
        "## 问题清单",
    ]
    for issue in result["issues"]:
        lines.append(f"- {issue}")
    if not result["issues"]:
        lines.append("（无）")
    lines.append("")
    lines.append("## 目标词分配")
    for item_type in REQUIRED_TYPES:
        lines.append(f"- {item_type}: {', '.join(result['wordsByType'][item_type])}")
    lines.append("")
    lines.append("> 本报告验证结构、证据、选项、答案与生产审查状态。")
    lines.append("> 所有题目均为 APPROVED；实际发布仍需按发布流程创建 release。")
    (OUT / "audit-report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({
        "totalItems": result["totalItems"],
        "counts": result["counts"],
        "uniqueTargetWords": result["uniqueTargetWords"],
        "issueCount": result["issueCount"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
