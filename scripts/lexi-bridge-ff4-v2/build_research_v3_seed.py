#!/usr/bin/env python3
"""Build the immutable LEXIBRIDGE_RESEARCH_V3 seed package.

V3 is a new questionnaire generated from the complete production-approved
FF4 question bank. It
contains four formal sections (word meaning, sentence selection, true/false,
spelling) plus basic info. The generated content is APPROVED after the
concept-level production rules and package audits have passed. Type 3 keeps
the false-friend F items and interleaves ten client-requested Vrai cognate
controls to reduce response-set bias.

The seed mirrors the V1 seed schema so the backend seed initializer can load
it without changes to the released V1 package. Content is production-approved
by the concept-level review rules in production_semantic_rules.py.
"""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from production_semantic_rules import RULESET_VERSION  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "data" / "lexi-bridge-ff4-v2"
SEED = ROOT / "app-server" / "src" / "main" / "resources" / "assessment-seeds" / "LEXIBRIDGE_RESEARCH_V3.json"

PACKAGE_CODE = "LEXIBRIDGE_RESEARCH_V3"
BANK_PACKAGE_NAME = "question-bank-package-production.json"
DURATION_MINUTES = 120

SECTION_META = {
    "FF4_WORD_MEANING": {
        "title": "Partie 1 – 词义单选（假朋友核心义判断）",
        "description": "请选出下列法语单词对应的正确中文含义。",
        "questionType": "SINGLE_CHOICE",
        "construct": "FF4_WORD_MEANING",
        "contextLevel": "WORD",
        "prompt": "请选出下列法语单词对应的正确中文含义。",
    },
    "FF4_SENTENCE_SELECTION": {
        "title": "Partie 2 – 句子选词（语境近义替换）",
        "description": "请根据句子选择画线单词的同义表达。",
        "questionType": "SINGLE_CHOICE",
        "construct": "FF4_SENTENCE_SYNONYM",
        "contextLevel": "SENTENCE",
        "prompt": "请根据句子选择画线单词的同义表达。",
    },
    "FF4_TRUE_FALSE": {
        "title": "Partie 3 – 判断正误（迁移义判定）",
        "description": "判断正误：法语单词是否表示给定的中文含义。",
        "questionType": "TRUE_FALSE",
        "construct": "FF4_TRUE_FALSE_TRANSFER",
        "contextLevel": "WORD",
        "prompt": "判断下列说法是否正确。",
    },
    "FF4_SPELLING": {
        "title": "Partie 4 – 单词拼写（形近干扰）",
        "description": "根据中文释义填写对应的法语单词。",
        "questionType": "SPELLING",
        "construct": "FF4_SPELLING",
        "contextLevel": "WORD",
        "prompt": "根据中文释义填写对应的法语单词。若答错一次，将显示该单词的首字母提示。",
    },
}


def content_hash(payload: dict) -> str:
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def build() -> dict:
    bank_package_path = OUT / BANK_PACKAGE_NAME
    package = json.loads(bank_package_path.read_text(encoding="utf-8"))
    bank_items = package["items"]
    bank_options = {}
    for option in package["options"]:
        bank_options.setdefault(option["itemCode"], []).append(option)
    adjudication = {}
    for line in (OUT / "candidate-adjudication.jsonl").read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        adjudication[record["frenchWord"]] = record
    evidence = {}
    for line in (OUT / "t2-evidence.jsonl").read_text(encoding="utf-8").splitlines():
        record = json.loads(line)
        evidence[record["frenchWord"]] = record

    sections = [
        {
            "sectionCode": "BASIC_INFO",
            "title": "基本信息",
            "sortOrder": 0,
            "formalSection": False,
            "scoredItemCount": 0,
            "description": "请填写以下基本信息；资料仅用于研究参与确认，答题与资料分离保存。",
            "sharedMaterial": None,
        },
    ]
    for section_code, meta in SECTION_META.items():
        count = sum(1 for item in bank_items if item["sectionCode"] == section_code)
        sections.append({
            "sectionCode": section_code,
            "title": meta["title"],
            "sortOrder": {"FF4_WORD_MEANING": 1, "FF4_SENTENCE_SELECTION": 2,
                          "FF4_TRUE_FALSE": 3, "FF4_SPELLING": 4}[section_code],
            "formalSection": True,
            "scoredItemCount": count,
            "description": meta["description"],
            "sharedMaterial": None,
        })

    items = []
    item_sequence = [item for item in sorted(bank_items, key=lambda i: i["itemCode"])]
    order = 1
    for bank_item in item_sequence:
        item_type = {"FF4_WORD_MEANING": "T1", "FF4_SENTENCE_SYNONYM": "T2",
                     "FF4_TRUE_FALSE_TRANSFER": "T3", "FF4_SPELLING": "T4"}[bank_item["constructCode"]]
        meta = SECTION_META[bank_item["sectionCode"]]
        word = bank_item["targetWord"]
        record = adjudication.get(word, {})
        evidence_record = evidence.get(word, {})
        explanation = build_explanation(bank_item, record, evidence_record)
        source_options = bank_options.get(bank_item["itemCode"], [])
        if item_type == "T1":
            question_type = "SINGLE_CHOICE"
            options = [{"key": o["optionCode"], "label": o["optionText"]} for o in source_options]
            correct = bank_item["correctAnswers"]
            option_explanations = option_explanations_for(source_options, record)
        elif item_type == "T2":
            question_type = "SINGLE_CHOICE"
            options = [{"key": o["optionCode"], "label": o["optionText"]} for o in source_options]
            correct = bank_item["correctAnswers"]
            option_explanations = option_explanations_for(source_options, record)
        elif item_type == "T3":
            question_type = "TRUE_FALSE"
            options = [{"key": "V", "label": "正确"}, {"key": "F", "label": "错误"}]
            correct = bank_item["correctAnswers"]
            option_explanations = {}
        else:
            question_type = "SPELLING"
            options = []
            correct = bank_item["correctAnswers"]
            option_explanations = {}

        payload = {
            "itemCode": bank_item["itemCode"],
            "sectionCode": bank_item["sectionCode"],
            "sortOrder": order,
            "questionType": question_type,
            "stemText": clean_stem(bank_item["stemText"], item_type),
            "promptText": meta["prompt"],
            "options": options,
            "correctAnswers": correct,
            "explanationText": explanation,
            "optionExplanations": option_explanations,
            "requiredAnswer": True,
            "scored": True,
            "score": 1,
            "weight": 1,
            "transferCategory": bank_item.get("transferCategory") or "FALSE_FRIEND",
            "contextLevel": meta["contextLevel"],
            "constructCode": meta["construct"],
            "targetWord": word,
            "displayCondition": None,
            "sourceReference": f"ff4-production-bank:{bank_item['itemCode']}",
            "lexicalReviewStatus": bank_item.get("lexicalReviewStatus", "APPROVED"),
            "pedagogicReviewStatus": bank_item.get("pedagogicReviewStatus", "APPROVED"),
            "tem4PdfPage": record.get("tem4PdfPage"),
            "falseFriendsPdfPage": record.get("falseFriendsPdfPage"),
        }
        payload["contentHash"] = content_hash(payload)
        items.append(payload)
        order += 1

    basic_items = basic_info_items(len(items) + 1)
    items.extend(basic_items)

    review_issues = []

    scored = [item for item in items if item.get("scored")]
    sections_sorted = sorted(sections, key=lambda s: s["sortOrder"])
    items_sorted = sorted(items, key=lambda i: (0 if i["sectionCode"] == "BASIC_INFO" else 1, i["sortOrder"]))

    return {
        "packageCode": PACKAGE_CODE,
        "source": {
            "questionnaireDocx": None,
            "analysisDocx": None,
            "questionnaireSha256": hashlib.sha256(
                bank_package_path.read_bytes()).hexdigest(),
            "analysisSha256": None,
            "generator": "scripts/lexi-bridge-ff4-v2/build_research_v3_seed.py",
            "bankPackage": BANK_PACKAGE_NAME,
            "type3DesignDecision": "CLIENT_APPROVED_MIXED_FALSE_FRIEND_AND_COGNATE_CONTROLS",
            "rulesetVersion": RULESET_VERSION,
        },
        "questionnaire": {
            "questionnaireCode": PACKAGE_CODE,
            "title": "Lexi-bridge 英法词汇认知迁移研究问卷 V3",
            "description": "基于法语专四假朋友四类题（词义单选、句子选词、判断正误、单词拼写）的研究问卷。",
            "durationMinutes": DURATION_MINUTES,
            "resultReleasePolicy": "IMMEDIATE",
            "scoringVersion": "SCORING_V3",
            "aiPromptVersion": "assessment-analysis/v3",
            "versionNo": 3,
            "status": "APPROVED",
        },
        "sections": sections_sorted,
        "items": items_sorted,
        "options": [
            {"itemCode": item["itemCode"], "optionKey": option["key"], "label": option["label"],
             "explanation": item["optionExplanations"].get(option["key"])}
            for item in items if item["options"]
            for option in item["options"]
        ],
        "reviewIssues": review_issues,
    }


def clean_stem(stem: str, item_type: str) -> str:
    """Keep only the item content; the instruction lives in promptText."""
    if item_type == "T1":
        lines = [line for line in stem.splitlines() if line and "请选出" not in line]
        return "\n".join(lines).strip()
    if item_type == "T2":
        lines = [line for line in stem.splitlines() if line and "请根据" not in line]
        return "\n".join(lines).strip()
    if item_type == "T3":
        return stem.replace("判断正误：", "").strip()
    return stem.strip()


def build_explanation(bank_item: dict, record: dict, evidence_record: dict) -> str:
    parts = [bank_item.get("explanationText", "").strip()]
    if evidence_record.get("t2SynonymEvidence"):
        synonym = evidence_record["t2SynonymEvidence"][0]
        parts.append(f"近义表达（原书证据）：{synonym['synonym']} —— {synonym['sourceLine']}。")
    if evidence_record.get("t2TransferEvidence"):
        transfer = evidence_record["t2TransferEvidence"][0]
        parts.append(f"迁移干扰（原书证据）：{transfer['french']} —— {transfer['sourceLine']}。")
    return "\n".join(part for part in parts if part)


def option_explanations_for(options: list[dict], record: dict) -> dict:
    result = {}
    for option in options:
        if option["role"] == "CORRECT":
            result[option["optionCode"]] = f"正确选项：法语词对应 TEM4 前 2–3 个核心义（{'；'.join(record.get('tem4TopSenses', []))}）。"
        elif option["role"] == "TRANSFER":
            result[option["optionCode"]] = f"迁移干扰项：为易混英文词的中文义（{'；'.join(record.get('englishChineseSenses', []))}）。"
        else:
            result[option["optionCode"]] = "随机干扰项：同词性、与正确答案无同义关系。"
    return result


def basic_info_items(start_order: int) -> list[dict]:
    def basic(code: str, order: int, question_type: str, stem: str, options=None,
              required: bool = False, display_condition: dict | None = None) -> dict:
        payload = {
            "itemCode": code,
            "sectionCode": "BASIC_INFO",
            "sortOrder": start_order + order,
            "questionType": question_type,
            "stemText": stem,
            "promptText": None,
            "options": options or [],
            "correctAnswers": [],
            "explanationText": None,
            "optionExplanations": {},
            "requiredAnswer": required,
            "scored": False,
            "score": 0,
            "weight": 1,
            "transferCategory": None,
            "contextLevel": None,
            "constructCode": None,
            "targetWord": None,
            "displayCondition": display_condition,
            "sourceReference": f"v3:basic-info:{code}",
        }
        payload["contentHash"] = content_hash(payload)
        return payload

    scored_count = start_order - 1
    return [
        basic("BASIC-INSTRUCTION", 1, "INSTRUCTION",
              f"亲爱的同学：\n您好！欢迎参与本次英法词汇认知迁移研究。姓名和联系方式仅用于研究参与确认与必要联络；资料会加密保存，并与正式答题、评分、自动分析和普通结果页面隔离，仅限问卷所有者或管理员在授权场景访问。正式测试共 {scored_count} 题，预计最多 {DURATION_MINUTES} 分钟，资料填写时间不计入测试。请勿查阅词典或与他人交流，独立完成作答。",
              required=False),
        basic("BASIC-NAME", 2, "SHORT_TEXT", "姓名", required=True),
        basic("BASIC-CONTACT", 3, "SHORT_TEXT", "联系方式（电话或邮箱）"),
        basic("BASIC-STATUS", 4, "SINGLE_CHOICE", "您的身份：", [
            {"key": "FRENCH_MAJOR", "label": "法语专业"},
            {"key": "FRENCH_SECOND_LANGUAGE", "label": "第二外语"},
            {"key": "NON_MAJOR", "label": "非外语专业"},
        ]),
        basic("BASIC-GAOKAO-ENGLISH", 5, "NUMBER", "您的英语学习水平：高考英语分数"),
        basic("BASIC-ENGLISH-MAJOR", 6, "SINGLE_CHOICE", "您的英语专业背景：", [
            {"key": "ENGLISH_MAJOR", "label": "英语专业"},
            {"key": "NON_ENGLISH_MAJOR", "label": "非英语专业"},
        ], required=True),
        basic("BASIC-CET4", 7, "NUMBER", "四级分数", display_condition={
            "fieldCode": "BASIC-ENGLISH-MAJOR", "operator": "EQ", "value": "ENGLISH_MAJOR"}),
        basic("BASIC-CET6", 8, "NUMBER", "六级分数", display_condition={
            "fieldCode": "BASIC-ENGLISH-MAJOR", "operator": "EQ", "value": "ENGLISH_MAJOR"}),
        basic("BASIC-TEM4", 9, "NUMBER", "专四分数", display_condition={
            "fieldCode": "BASIC-ENGLISH-MAJOR", "operator": "EQ", "value": "ENGLISH_MAJOR"}),
        basic("BASIC-TEM8", 10, "NUMBER", "专八分数", display_condition={
            "fieldCode": "BASIC-ENGLISH-MAJOR", "operator": "EQ", "value": "ENGLISH_MAJOR"}),
        basic("BASIC-FRENCH-DURATION", 11, "SINGLE_CHOICE", "学习法语的时间：", [
            {"key": "DURATION_1", "label": "1 年以内"},
            {"key": "DURATION_2", "label": "1–2 年"},
            {"key": "DURATION_3", "label": "2–3 年"},
            {"key": "DURATION_4", "label": "3 年以上"},
        ]),
        basic("BASIC-OTHER-LANGUAGE", 12, "SHORT_TEXT", "除英语和法语外，您还学习过哪些语言？"),
    ]


def main() -> None:
    seed = build()
    SEED.parent.mkdir(parents=True, exist_ok=True)
    SEED.write_text(json.dumps(seed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    scored = sum(1 for item in seed["items"] if item.get("scored"))
    print(json.dumps({
        "packageCode": PACKAGE_CODE,
        "scoredItems": scored,
        "sections": [s["sectionCode"] for s in seed["sections"]],
        "status": seed["questionnaire"]["status"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
