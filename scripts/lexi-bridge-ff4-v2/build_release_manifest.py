#!/usr/bin/env python3
"""Build a hash-pinned, non-deployment release manifest for FF4 V3."""

from __future__ import annotations

import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path

from production_semantic_rules import RULESET_VERSION, TRUE_COGNATE_CONTROLS

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "data" / "lexi-bridge-ff4-v2"
SEED = ROOT / "app-server/src/main/resources/assessment-seeds/LEXIBRIDGE_RESEARCH_V3.json"
TARGET = OUT / "production-release-manifest.json"

ARTIFACTS = [
    OUT / "法语专四假朋友题库_生产版.xlsx",
    OUT / "法语专四假朋友题库_V3.xlsx",
    OUT / "question-bank-package-production.json",
    SEED,
    OUT / "production-audit-report.md",
    OUT / "seed-preflight-report.json",
    OUT / "PRODUCTION_RULES.md",
    OUT / "TYPE3_DESIGN_DECISION.md",
]


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    package = json.loads((OUT / "question-bank-package-production.json").read_text(encoding="utf-8"))
    preflight = json.loads((OUT / "seed-preflight-report.json").read_text(encoding="utf-8"))
    counts: dict[str, int] = {}
    for item in package["items"]:
        counts[item["constructCode"]] = counts.get(item["constructCode"], 0) + 1
    manifest = {
        "manifestVersion": 1,
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "rulesetVersion": RULESET_VERSION,
        "packageCode": package["questionnaire"]["code"],
        "v3QuestionnaireCode": "LEXIBRIDGE_RESEARCH_V3",
        "itemCount": len(package["items"]),
        "uniqueTargetCount": len({item["targetWord"] for item in package["items"]}),
        "counts": counts,
        "trueCognateControlCount": len(TRUE_COGNATE_CONTROLS),
        "statuses": {
            "content": "APPROVED",
            "coverage": "MAXIMUM_RULE_COMPLIANT_COVERAGE",
            "import": "READY_FOR_IMPORT_VALIDATION",
            "preflight": preflight["status"],
            "deployment": "NOT_DEPLOYED",
        },
        "artifacts": [
            {
                "path": str(path.relative_to(ROOT)),
                "sizeBytes": path.stat().st_size,
                "sha256": sha256(path),
            }
            for path in ARTIFACTS
        ],
        "deploymentNotes": [
            "Manifest generation does not connect to or modify production.",
            "Back up the production database and current seed files before import.",
            "Run backend integration tests in a Java/Docker-enabled environment before creating a release.",
            "Keep the released V1 package and rollback path intact.",
        ],
    }
    TARGET.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(TARGET)


if __name__ == "__main__":
    main()
