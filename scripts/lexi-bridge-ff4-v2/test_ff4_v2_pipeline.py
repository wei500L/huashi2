#!/usr/bin/env python3
"""FF4 V2 generator determinism + pipeline tests.

Run with: python3 -m unittest scripts/lexi-bridge-ff4-v2/test_ff4_v2_pipeline.py
(or from repo root: python3 -m unittest discover -s scripts/lexi-bridge-ff4-v2)
"""

from __future__ import annotations

import hashlib
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_candidates import (  # noqa: E402
    decide_semantic_overlap,
    detect_morphology_only,
    fold_latin,
    levenshtein,
)
from build_question_bank import content_hash  # noqa: E402
from production_semantic_rules import T2_OPTIONS, T2_SENTENCE, T2_TARGET_WORD  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "data" / "lexi-bridge-ff4-v2"


class SemanticOverlapTests(unittest.TestCase):

    def test_core_overlap_uses_exact_tokens(self):
        decision, reason, hits = decide_semantic_overlap(["班级"], ["班级"])
        self.assertEqual(decision, "CORE")
        self.assertIn("班级", hits)

    def test_core_overlap_uses_shared_shingles(self):
        decision, reason, hits = decide_semantic_overlap(
            ["给人深刻印象的", "巨大的"], ["令人印象深刻的"])
        self.assertEqual(decision, "CORE")
        self.assertTrue(any("印象" in h for h in hits))

    def test_no_overlap_returns_none(self):
        decision, reason, hits = decide_semantic_overlap(["书店"], ["图书馆"])
        self.assertEqual(decision, "NONE")
        self.assertEqual(hits, [])

    def test_partial_overlap_uses_equal_length_near_forms(self):
        decision, reason, hits = decide_semantic_overlap(["机会"], ["机遇"])
        self.assertEqual(decision, "PARTIAL")


class DistanceAndMorphologyTests(unittest.TestCase):

    def test_levenshtein_basics(self):
        self.assertEqual(levenshtein("librairie", "library"), 3)
        self.assertEqual(levenshtein("chat", "chat"), 0)
        self.assertEqual(levenshtein("appartement", "apartment"), 2)

    def test_accent_folding(self):
        self.assertEqual(fold_latin("déranger"), "deranger")
        self.assertEqual(levenshtein(fold_latin("déranger"), fold_latin("derange")), 1)

    def test_morphology_only_verb_infinitive(self):
        self.assertEqual(detect_morphology_only("classer", "class"), (True, "VERB_INFINITIVE_ER"))

    def test_morphology_only_final_r_after_english_e(self):
        self.assertEqual(detect_morphology_only("imaginer", "imagine"),
                         (True, "VERB_INFINITIVE_FINAL_R"))
        self.assertEqual(detect_morphology_only("fatiguer", "fatigue"),
                         (True, "VERB_INFINITIVE_FINAL_R"))

    def test_non_morphology_pair_is_allowed(self):
        self.assertEqual(detect_morphology_only("peine", "pain"), (False, None))
        self.assertEqual(detect_morphology_only("appartement", "apartment"), (False, None))


class DeterminismTests(unittest.TestCase):

    def test_adjudication_and_bank_are_deterministic(self):
        import subprocess
        generator_dir = Path(__file__).resolve().parent

        def run(script: str) -> bytes:
            result = subprocess.run([sys.executable, str(generator_dir / script)],
                                    capture_output=True, check=True, timeout=300)
            return result.stdout

        run("build_candidates.py")
        run("build_t2_evidence.py")
        run("build_question_bank.py")
        first = (OUT / "question-bank-package-v2.json").read_bytes()
        first_hash = hashlib.sha256(first).hexdigest()

        run("build_candidates.py")
        run("build_t2_evidence.py")
        run("build_question_bank.py")
        second = (OUT / "question-bank-package-v2.json").read_bytes()
        second_hash = hashlib.sha256(second).hexdigest()

        self.assertEqual(first_hash, second_hash, "repeated runs must be byte-identical")

    def test_every_item_hash_is_unique_and_stable(self):
        seed_path = ROOT / "app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json"
        import subprocess
        subprocess.run([sys.executable, str(Path(__file__).resolve().parent / "build_research_v3_seed.py")],
                       capture_output=True, check=True, timeout=300)
        seed = json.loads(seed_path.read_text(encoding="utf-8"))
        hashes = [item["contentHash"] for item in seed["items"]]
        self.assertEqual(len(hashes), len(set(hashes)))
        for item in seed["items"]:
            payload = {k: v for k, v in item.items() if k != "contentHash"}
            self.assertEqual(content_hash(payload), item["contentHash"], item["itemCode"])


class PackageStructureTests(unittest.TestCase):

    def test_global_target_word_uniqueness(self):
        package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
        words = [item.get("targetWord") for item in package["items"] if item.get("targetWord")]
        self.assertEqual(len(words), len(set(words)))

    def test_choice_items_have_four_options_with_roles(self):
        package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
        options_by_item = {}
        for option in package["options"]:
            options_by_item.setdefault(option["itemCode"], []).append(option)
        for item in package["items"]:
            if item["constructCode"] not in ("FF4_WORD_MEANING", "FF4_SENTENCE_SYNONYM"):
                continue
            options = options_by_item[item["itemCode"]]
            self.assertEqual(len(options), 4, item["itemCode"])
            roles = [o["role"] for o in options]
            self.assertEqual(roles.count("CORRECT"), 1, item["itemCode"])
            self.assertEqual(roles.count("TRANSFER"), 1, item["itemCode"])
            self.assertEqual(roles.count("DISTRACTOR"), 2, item["itemCode"])

    def test_true_false_items_are_all_false(self):
        package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
        for item in package["items"]:
            if item["constructCode"] == "FF4_TRUE_FALSE_TRANSFER":
                self.assertEqual(item["correctAnswers"], ["F"], item["itemCode"])
                self.assertEqual(item["stemText"], f"{item['targetWord']} = " + item["stemText"].split(" = ", 1)[1])

    def test_t2_matches_0811_document_rule(self):
        package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
        items = [item for item in package["items"] if item["constructCode"] == "FF4_SENTENCE_SYNONYM"]
        self.assertEqual(len(items), 1)
        item = items[0]
        self.assertEqual(item["targetWord"], T2_TARGET_WORD)
        self.assertEqual(item["stemText"], "请根据句子选择画线单词的同义解释\n" + T2_SENTENCE)
        options = [option for option in package["options"] if option["itemCode"] == item["itemCode"]]
        actual = [(o["optionCode"], o["optionText"], o["correct"], o["role"]) for o in options]
        self.assertEqual(actual, list(T2_OPTIONS))

    def test_import_package_matches_dto_shape(self):
        package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
        self.assertEqual(set(package), {"questionnaire", "sections", "items", "options"})
        required = {"itemCode", "sectionCode", "questionType", "correctAnswers", "requiredAnswer",
                    "scored", "weight", "constructCode", "targetWord", "lexicalReviewStatus",
                    "pedagogicReviewStatus"}
        for item in package["items"]:
            self.assertTrue(required <= set(item), item["itemCode"])

    def test_t1_distractors_have_same_coarse_pos(self):
        package = json.loads((OUT / "question-bank-package-v2.json").read_text(encoding="utf-8"))
        records = [json.loads(line) for line in (OUT / "candidate-adjudication.jsonl").read_text(encoding="utf-8").splitlines()]
        by_word = {record["frenchWord"]: record for record in records}
        options_by_item = {}
        for option in package["options"]:
            options_by_item.setdefault(option["itemCode"], []).append(option)
        for item in package["items"]:
            if item["constructCode"] != "FF4_WORD_MEANING":
                continue
            target_pos = (by_word[item["targetWord"]].get("partOfSpeech") or "").split(".")[0].strip()
            for option in options_by_item[item["itemCode"]]:
                if option["role"] != "DISTRACTOR":
                    continue
                matching = [record for record in records
                            if "; ".join((record.get("tem4TopSenses") or [])[:2]) == option["optionText"]]
                self.assertTrue(any((record.get("partOfSpeech") or "").split(".")[0].strip() == target_pos
                                    for record in matching), item["itemCode"])

    def test_instruction_reports_actual_scored_count(self):
        seed = json.loads((ROOT / "app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json").read_text(encoding="utf-8"))
        scored = sum(1 for item in seed["items"] if item.get("scored"))
        instruction = next(item for item in seed["items"] if item["itemCode"] == "BASIC-INSTRUCTION")
        self.assertIn(f"正式测试共 {scored} 题", instruction["stemText"])

    def test_v3_uses_complete_production_bank(self):
        seed = json.loads((ROOT / "app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json").read_text(encoding="utf-8"))
        production = json.loads((OUT / "question-bank-package-production.json").read_text(encoding="utf-8"))
        scored = [item for item in seed["items"] if item.get("scored")]
        self.assertEqual(seed["source"]["bankPackage"], "question-bank-package-production.json")
        self.assertEqual(len(scored), len(production["items"]))
        self.assertEqual({item["itemCode"] for item in scored},
                         {item["itemCode"] for item in production["items"]})


if __name__ == "__main__":
    unittest.main()
