"""08 - Reflection & Self-Critique: draft -> critique -> revise (bounded).

Run: python 03-advanced/08-reflection-and-self-critique/main.py
"""

from __future__ import annotations

from dataclasses import dataclass


CRITERIA = [
    "states assumptions",
    "includes a concrete next step",
    "avoids claiming certainty without evidence",
]


@dataclass
class Critique:
    passed: bool
    issues: list[str]


class MockLLM:
    def draft(self, task: str, revision_hint: str = "") -> str:
        if not revision_hint:
            return "You should add retries."  # intentionally incomplete
        return (
            "Assumption: failures may be transient. "
            "Add retries with exponential backoff for idempotent operations, "
            "and verify with a test that simulates a transient error."
        )

    def critique(self, draft: str, criteria: list[str]) -> Critique:
        issues = []
        if "assumption" not in draft.lower():
            issues.append("missing assumptions")
        if "test" not in draft.lower() and "next step" not in draft.lower():
            issues.append("missing a concrete next step")
        # Third criterion is subjective; mock checks for overclaiming.
        if "guaranteed" in draft.lower() or "always works" in draft.lower():
            issues.append("claims certainty without evidence")
        return Critique(passed=not issues, issues=issues)


def reflective_answer(task: str, max_revisions: int = 2) -> str:
    llm = MockLLM()
    draft = llm.draft(task)
    print(f"[draft v1] {draft}")

    for rev in range(1, max_revisions + 1):
        critique = llm.critique(draft, CRITERIA)
        print(f"[critique v{rev}] passed={critique.passed} issues={critique.issues}")
        if critique.passed:
            return draft
        hint = "Fix these issues: " + ", ".join(critique.issues)
        draft = llm.draft(task, revision_hint=hint)
        print(f"[draft v{rev + 1}] {draft}")

    return draft + " (Note: reached revision limit; verify before use.)"


if __name__ == "__main__":
    print("\n[final]", reflective_answer("How should we handle transient API failures?"))
