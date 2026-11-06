"""Run all eval suites.

Run: python evals/run_evals.py
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from evals.cases.adversarial import cases as adversarial_cases
from evals.cases.golden import cases as golden_cases
from evals.cases.jev_cascade import cases as jev_cascade_cases
from evals.cases.jev_gate import cases as jev_gate_cases
from evals.cases.tool_failures import cases as tool_failure_cases
from evals.harness import run_cases


def main() -> int:
    cases = []
    cases.extend(golden_cases())
    cases.extend(tool_failure_cases())
    cases.extend(adversarial_cases())
    cases.extend(jev_gate_cases())
    cases.extend(jev_cascade_cases())
    report = run_cases(cases, report_path=ROOT / "evals" / "report.json")
    return 0 if report.failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
