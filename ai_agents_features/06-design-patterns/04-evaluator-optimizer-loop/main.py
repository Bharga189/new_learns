"""Evaluator-Optimizer: generate -> evaluate criteria -> optimize, bounded.

Run: python 06-design-patterns/04-evaluator-optimizer-loop/main.py
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Evaluation:
    passed: bool
    score: float
    feedback: list[str]


class Generator:
    def run(self, task: str, feedback: list[str] | None = None) -> str:
        if not feedback:
            return "Deploy now."  # intentionally weak
        return f"Plan for '{task}': run tests, get approval for production, deploy with rollback ready."


class Evaluator:
    criteria = ["mentions tests/verification", "mentions approval for production risk", "mentions rollback/safety"]

    def run(self, candidate: str) -> Evaluation:
        text = candidate.lower()
        feedback = []
        if "test" not in text and "verif" not in text:
            feedback.append("missing tests/verification")
        if "approval" not in text:
            feedback.append("missing approval for production risk")
        if "rollback" not in text and "safe" not in text:
            feedback.append("missing rollback/safety")
        score = (len(self.criteria) - len(feedback)) / len(self.criteria)
        return Evaluation(passed=not feedback, score=score, feedback=feedback)


def optimize(task: str, max_rounds: int = 3) -> str:
    gen, ev = Generator(), Evaluator()
    feedback = None
    candidate = ""
    for i in range(1, max_rounds + 1):
        candidate = gen.run(task, feedback)
        result = ev.run(candidate)
        print(f"[round {i}] score={result.score:.2f} passed={result.passed} feedback={result.feedback}")
        if result.passed:
            return candidate
        feedback = result.feedback
    return candidate + " [max rounds reached]"


if __name__ == "__main__":
    print("\n[final]", optimize("Deploy the service update"))
