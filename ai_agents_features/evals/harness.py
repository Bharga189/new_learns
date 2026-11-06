"""Minimal eval harness. Extend with rubrics, LLM judges, and CI integration."""

from __future__ import annotations

import json
import time
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any, Callable


@dataclass
class Case:
    id: str
    suite: str
    description: str
    run: Callable[[], Any]
    expect: Callable[[Any], tuple[bool, str]]


@dataclass
class CaseResult:
    id: str
    suite: str
    passed: bool
    detail: str
    duration_ms: float
    output_preview: str = ""


@dataclass
class Report:
    total: int = 0
    passed: int = 0
    failed: int = 0
    results: list[CaseResult] = field(default_factory=list)

    def add(self, result: CaseResult) -> None:
        self.results.append(result)
        self.total += 1
        if result.passed:
            self.passed += 1
        else:
            self.failed += 1


def run_cases(cases: list[Case], report_path: str | Path = "evals/report.json") -> Report:
    report = Report()
    for case in cases:
        start = time.time()
        try:
            output = case.run()
            ok, detail = case.expect(output)
            preview = str(output)[:200]
        except Exception as exc:  # eval captures failures as results
            ok, detail, preview = False, f"{type(exc).__name__}: {exc}", ""
        result = CaseResult(
            id=case.id,
            suite=case.suite,
            passed=ok,
            detail=detail,
            duration_ms=round((time.time() - start) * 1000, 2),
            output_preview=preview,
        )
        report.add(result)
        status = "PASS" if ok else "FAIL"
        print(f"[{status}] {case.suite}/{case.id}: {detail}")
    path = Path(report_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(asdict(report), indent=2), encoding="utf-8")
    print(f"\nSummary: {report.passed}/{report.total} passed. Report: {path}")
    return report
