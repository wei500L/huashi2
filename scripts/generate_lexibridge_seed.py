"""Build the immutable LEXIBRIDGE_RESEARCH_V1 seed package from the two source DOCX files.

The questionnaire DOCX is the content authority. The analysis DOCX only enriches
items with answers, explanations, option-level rationale, and research labels.
No source wording is rewritten by this generator.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

from docx import Document


PACKAGE_CODE = "LEXIBRIDGE_RESEARCH_V1"
OPTION_RE = re.compile(r"^([A-D])\.\s*(.*)$")
NUMBER_RE = re.compile(r"^\d+\.\s*")


def with_content_hash(payload: dict) -> dict:
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    payload["contentHash"] = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    return payload


def paragraphs(path: Path) -> list[str]:
    return [paragraph.text.strip() for paragraph in Document(path).paragraphs]


def clean_stem(value: str) -> str:
    return NUMBER_RE.sub("", value).strip()


def parse_four_choice(source: list[str], stem_index: int) -> tuple[str, list[dict[str, str]]]:
    stem = clean_stem(source[stem_index])
    options: list[dict[str, str]] = []
    for raw in source[stem_index + 1 : stem_index + 5]:
        match = OPTION_RE.match(raw)
        if not match:
            raise ValueError(f"Expected option after paragraph {stem_index}: {raw!r}")
        options.append({"key": match.group(1), "label": match.group(2).strip()})
    return stem, options


def parse_inline_options(raw: str) -> list[dict[str, str]]:
    matches = list(re.finditer(r"(?:^|\s)([A-D])\.\s*", raw))
    options: list[dict[str, str]] = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(raw)
        options.append({"key": match.group(1), "label": raw[match.end() : end].strip()})
    if len(options) != 4:
        raise ValueError(f"Expected four inline options: {raw!r}")
    return options


def analysis_block(analysis: list[str], start: int, end: int) -> list[str]:
    return [value for value in analysis[start:end] if value]


def infer_transfer(block: list[str]) -> str | None:
    text = "\n".join(block)
    if "假朋友" in text or "同形异义词" in text:
        return "FALSE_FRIEND"
    if "同源词" in text:
        return "COGNATE"
    if "纯法语" in text or "对照词" in text:
        return "FRENCH_CONTROL"
    return None


def option_explanations(block: list[str], correct: str) -> dict[str, str]:
    result: dict[str, str] = {}
    general: list[str] = []
    for value in block:
        match = re.search(r"(?:干扰项|[•·])\s*([A-D])\s*", value)
        if match:
            result[match.group(1)] = value
        elif not NUMBER_RE.match(value) and "答案" not in value and not value.startswith("题型"):
            general.append(value)
    if general:
        result.setdefault(correct, "\n".join(general))
    return result


def item(
    *,
    code: str,
    section: str,
    order: int,
    stem: str,
    options: list[dict[str, str]],
    answer: str,
    block: list[str],
    context: str,
    target: str | None,
    question_type: str = "SINGLE_CHOICE",
    shared_material_ref: str | None = None,
) -> dict:
    transfer = infer_transfer(block)
    construct = "LEXICAL_TRANSFER" if context in {"WORD", "PHRASE"} else "CONTEXT_REPAIR"
    explanation = "\n".join(block)
    payload = {
        "itemCode": code,
        "sectionCode": section,
        "sortOrder": order,
        "questionType": question_type,
        "stemText": stem,
        "promptText": None,
        "options": options,
        "correctAnswers": [answer],
        "explanationText": explanation,
        "optionExplanations": option_explanations(block, answer) if answer in "ABCD" else {},
        "requiredAnswer": True,
        "scored": True,
        "score": 1,
        "weight": 1,
        "transferCategory": transfer,
        "contextLevel": context,
        "constructCode": construct,
        "targetWord": target,
        "displayCondition": None,
        "sharedMaterialRef": shared_material_ref,
        "sourceReference": f"questionnaire:{code}",
    }
    return with_content_hash(payload)


def basic_items(source: list[str]) -> list[dict]:
    def basic(code: str, order: int, question_type: str, stem: str, options=None, required=False,
              display_condition=None, source_reference=None):
        return with_content_hash({
            "itemCode": code,
            "sectionCode": "BASIC_INFO",
            "sortOrder": order,
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
            "sourceReference": source_reference or f"questionnaire:paragraph-{58 + order}",
        })

    consent = "\n".join(value for value in source[55:58] if value)
    english_major_condition = {
        "fieldCode": "BASIC-ENGLISH-MAJOR",
        "operator": "EQ",
        "value": "ENGLISH_MAJOR",
    }
    return [
        basic("BASIC-INSTRUCTION", 1, "INSTRUCTION", consent),
        basic("BASIC-NAME", 2, "SHORT_TEXT", source[59], required=True, source_reference="questionnaire:paragraph-60"),
        basic("BASIC-CONTACT", 3, "SHORT_TEXT", source[60], source_reference="questionnaire:paragraph-61"),
        basic("BASIC-GAOKAO-ENGLISH", 4, "NUMBER", source[66], source_reference="questionnaire:paragraph-63"),
        basic("BASIC-ENGLISH-MAJOR", 5, "SINGLE_CHOICE", "您是否为英语专业学生：", [
            {"key": "ENGLISH_MAJOR", "label": "英语专业"},
            {"key": "NON_ENGLISH_MAJOR", "label": "非英语专业"},
        ], required=True, source_reference="questionnaire:fix-2026-08-basic-english-major"),
        basic("BASIC-CET4", 6, "NUMBER", source[68], source_reference="questionnaire:paragraph-64"),
        basic("BASIC-CET6", 7, "NUMBER", source[69], source_reference="questionnaire:paragraph-65"),
        basic("BASIC-TEM4", 8, "NUMBER", source[73], display_condition=english_major_condition,
              source_reference="questionnaire:paragraph-66"),
        basic("BASIC-TEM8", 9, "NUMBER", source[74], display_condition=english_major_condition,
              source_reference="questionnaire:paragraph-67"),
    ]


def build(questionnaire_path: Path, analysis_path: Path) -> dict:
    source = paragraphs(questionnaire_path)
    analysis = paragraphs(analysis_path)

    sections = [
        {"sectionCode": "BASIC_INFO", "title": source[58], "sortOrder": 0, "formalSection": False,
         "scoredItemCount": 0, "description": None, "sharedMaterial": None},
        {"sectionCode": "P1A", "title": "Partie 1 / Section A", "sortOrder": 1, "formalSection": True,
         "scoredItemCount": 10, "description": source[89], "sharedMaterial": None},
        {"sectionCode": "P1B", "title": "Partie 1 / Section B", "sortOrder": 2, "formalSection": True,
         "scoredItemCount": 10, "description": source[142], "sharedMaterial": None},
        {"sectionCode": "P2", "title": source[195], "sortOrder": 3, "formalSection": True,
         "scoredItemCount": 10, "description": source[196], "sharedMaterial": None},
        {"sectionCode": "P3", "title": source[248], "sortOrder": 4, "formalSection": True,
         "scoredItemCount": 10, "description": source[249], "sharedMaterial": "\n".join(source[249:253])},
        {"sectionCode": "P4T1", "title": f"{source[264]} / {source[265]}", "sortOrder": 5, "formalSection": True,
         "scoredItemCount": 5, "description": None, "sharedMaterial": "\n".join(source[266:268])},
        {"sectionCode": "P4T2", "title": source[294], "sortOrder": 6, "formalSection": True,
         "scoredItemCount": 5, "description": None, "sharedMaterial": "\n".join(source[295:297])},
        {"sectionCode": "P4T3", "title": source[324], "sortOrder": 7, "formalSection": True,
         "scoredItemCount": 10, "description": source[323], "sharedMaterial": "\n".join(source[325:331])},
    ]

    specs = [
        ("P1A", "WORD", [90, 95, 100, 105, 110, 115, 120, 125, 130, 135],
         [2, 8, 15, 21, 27, 34, 40, 47, 53, 59], 67,
         list("ABCADAAABA"),
         ["description", "actuellement", "librairie", "bavarder", "hésiter", "participer", "prétendre", "injurier", "se promener", "respecter"]),
        ("P1B", "PHRASE", [143, 148, 153, 158, 163, 169, 174, 179, 184, 189],
         [69, 75, 81, 88, 94, 102, 108, 114, 121, 127], 135,
         list("BAACBBADAB"),
         ["entreprise", "courses", "boisson", "blesser", "accomplir", "important", "ennuyeux", "sensible", "propre", "exacte"]),
        ("P2", "SENTENCE", [197, 202, 207, 212, 217, 222, 227, 232, 237, 242],
         [136, 141, 146, 151, 156, 162, 167, 172, 177, 182], 188,
         list("CABDCBCBAB"),
         ["location", "commerce", "peinture", "reporter", "annoncer", "capturer", "se dépêcher", "physicien", "sale", "constituer"]),
        ("P4T1", "READING", [268, 273, 278, 283, 288],
         [253, 260, 267, 274, 281], 289,
         list("BACAD"), [None, "ignorer", "sensible", "éventuel", "essentiel"]),
        ("P4T2", "READING", [297, 302, 307, 312, 317],
         [290, 297, 304, 311, 318], 326,
         list("BBCDC"), [None, "collège", "formation", "passer", "stimulante"]),
    ]

    items = basic_items(source)
    formal_order_base = {"P1A": 1, "P1B": 11, "P2": 21, "P4T1": 41, "P4T2": 46}
    for section_code, context, stem_indices, block_starts, block_end, answers, targets in specs:
        for index, stem_index in enumerate(stem_indices):
            stem, options = parse_four_choice(source, stem_index)
            end = block_starts[index + 1] if index + 1 < len(block_starts) else block_end
            block = analysis_block(analysis, block_starts[index], end)
            items.append(item(code=f"{section_code}-{index + 1:02d}", section=section_code,
                              order=formal_order_base[section_code] + index, stem=stem, options=options,
                              answer=answers[index], block=block,
                              context=context, target=targets[index],
                              shared_material_ref=section_code if section_code.startswith("P4") else None))

    p3_starts = [189, 195, 201, 207, 213, 219, 225, 231, 237, 243]
    p3_answers = list("CDBADB CACB".replace(" ", ""))
    p3_targets = ["chemin", "manquer", "habit", "emploi", "répondre", "entente", "assister", "décevoir", "évidence", "habituel"]
    p3_items = []
    for index, option_index in enumerate(range(253, 263)):
        end = p3_starts[index + 1] if index + 1 < len(p3_starts) else 251
        block = analysis_block(analysis, p3_starts[index], end)
        p3_items.append(item(code=f"P3-{index + 1:02d}", section="P3", order=31 + index,
                             stem=f"Une journée de travail — blanc ({index + 1})", options=parse_inline_options(source[option_index]),
                             answer=p3_answers[index], block=block, context="CLOZE", target=p3_targets[index],
                             shared_material_ref="P3"))
    items.extend(p3_items)

    p4t3_starts = [327, 332, 337, 343, 349, 355, 361, 367, 373, 379]
    p4t3_answers = list("VVFFFFFFVV")
    p4t3_targets = ["attirait", "réservé", "étouffante", "pareilles", "carte", "place", "engagés", "formidable", "figure", "contemplation"]
    for index, stem_index in enumerate(range(331, 341)):
        end = p4t3_starts[index + 1] if index + 1 < len(p4t3_starts) else len(analysis)
        block = analysis_block(analysis, p4t3_starts[index], end)
        items.append(item(code=f"P4T3-{index + 1:02d}", section="P4T3", order=51 + index,
                          stem=source[stem_index], options=[{"key": "V", "label": "正确"}, {"key": "F", "label": "错误"}],
                          answer=p4t3_answers[index], block=block, context="READING", target=p4t3_targets[index],
                          question_type="TRUE_FALSE_WITH_JUSTIFICATION", shared_material_ref="P4T3"))

    review_issues = [
        {"issueCode": "SOURCE_NUMBERING_ABSENT", "severity": "REVIEW_REQUIRED", "itemCode": "P1A-01",
         "description": "The first question number is represented by Word auto-numbering and is absent from extracted text."},
        {"issueCode": "ANALYSIS_ITEM_NUMBER_ABSENT", "severity": "REVIEW_REQUIRED", "itemCode": "P2-04",
         "description": "The analysis document contains the answer and rationale but omits the item number and stem."},
        {"issueCode": "ANALYSIS_FORMAT_INCONSISTENT", "severity": "REVIEW_REQUIRED", "itemCode": "P3-06",
         "description": "The answer is present before an analysis marker that does not repeat the answer."},
        {"issueCode": "ANALYSIS_FORMAT_INCONSISTENT", "severity": "REVIEW_REQUIRED", "itemCode": "P3-10",
         "description": "The answer is present before an analysis marker that does not repeat the answer."},
        {"issueCode": "DUPLICATED_SOURCE_SENTENCE", "severity": "REVIEW_REQUIRED", "itemCode": "P4T3-01",
         "description": "The final paragraph of Texte 3 appends the first true/false statement, which is then repeated as item 1."},
    ]
    for current in items:
        if current.get("scored") and current.get("transferCategory") is None:
            review_issues.append({"issueCode": "UNKNOWN_TRANSFER_CATEGORY", "severity": "REVIEW_REQUIRED",
                                  "itemCode": current["itemCode"],
                                  "description": "The analysis document does not explicitly assign a transfer category."})

    formal_items = [current for current in items if current["scored"]]
    if len(formal_items) != 60:
        raise ValueError(f"Expected 60 scored items, found {len(formal_items)}")
    if len([section for section in sections if section["formalSection"]]) != 7:
        raise ValueError("Expected seven formal sections")
    items.sort(key=lambda current: (0 if current["sectionCode"] == "BASIC_INFO" else 1, current["sortOrder"]))

    return {
        "packageCode": PACKAGE_CODE,
        "source": {
            "questionnaireDocx": str(questionnaire_path),
            "analysisDocx": str(analysis_path),
            "questionnaireSha256": hashlib.sha256(questionnaire_path.read_bytes()).hexdigest(),
            "analysisSha256": hashlib.sha256(analysis_path.read_bytes()).hexdigest(),
        },
        "questionnaire": {
            "questionnaireCode": PACKAGE_CODE,
            "title": "Lexi-bridge 英法词汇认知迁移研究问卷",
            "description": source[0],
            "durationMinutes": 60,
            "resultReleasePolicy": "IMMEDIATE",
            "scoringVersion": "SCORING_V1",
            "aiPromptVersion": "assessment-analysis/v2",
            "versionNo": 1,
            "status": "REVIEW_REQUIRED" if review_issues else "DRAFT",
        },
        "sections": sections,
        "items": items,
        "options": [
            {"itemCode": current["itemCode"], "optionKey": option["key"], "label": option["label"],
             "explanation": current["optionExplanations"].get(option["key"])}
            for current in items for option in current["options"]
        ],
        "reviewIssues": review_issues,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("questionnaire_docx", type=Path)
    parser.add_argument("analysis_docx", type=Path)
    parser.add_argument("output_json", type=Path)
    args = parser.parse_args()
    package = build(args.questionnaire_docx, args.analysis_docx)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(package, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"generated {args.output_json} with 60 scored items and 7 formal sections")


if __name__ == "__main__":
    main()
