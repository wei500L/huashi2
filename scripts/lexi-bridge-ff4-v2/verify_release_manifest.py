#!/usr/bin/env python3
"""Verify release artifacts against production-release-manifest.json."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "docs/data/lexi-bridge-ff4-v2/production-release-manifest.json"


def main() -> int:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    issues = []
    for artifact in manifest["artifacts"]:
        path = ROOT / artifact["path"]
        if not path.is_file():
            issues.append(f"missing artifact: {artifact['path']}")
            continue
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest != artifact["sha256"]:
            issues.append(f"sha256 mismatch: {artifact['path']}")
        if path.stat().st_size != artifact["sizeBytes"]:
            issues.append(f"size mismatch: {artifact['path']}")
    result = {
        "status": "PASS" if not issues else "FAIL",
        "artifactCount": len(manifest["artifacts"]),
        "issueCount": len(issues),
        "issues": issues,
    }
    print(json.dumps(result, ensure_ascii=False))
    return 1 if issues else 0


if __name__ == "__main__":
    raise SystemExit(main())
