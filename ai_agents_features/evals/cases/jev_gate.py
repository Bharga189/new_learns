"""Evals for the Jev refund gate: a broken check must never become an approval.

Each case swaps in an adversarial client that fails a different way, and asserts
the gate degrades to `review`. A positive control keeps the suite honest: if the
gate simply never approved anything, these tests would pass vacuously.
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path
from typing import Any

from evals.harness import Case

ROOT = Path(__file__).resolve().parents[2]


def load_module(rel_path: str, name: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / rel_path)
    mod = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


class StubClient:
    """Client whose `system_one` does whatever the case needs."""

    def __init__(self, behaviour) -> None:
        self.behaviour = behaviour
        self.calls = 0

    def system_one(self, state: Any, questions: dict[str, dict[str, Any]]):
        self.calls += 1
        return self.behaviour(questions)


def good_answers(questions: dict[str, dict[str, Any]]) -> dict[str, dict[str, Any]]:
    return {key: {"type": "noul", "noul": 0.93} for key in questions}


def cases() -> list[Case]:
    jev = load_module("05-frameworks/jev/main.py", "jev_gate_main")
    ticket = jev.TICKETS["T-1001"]
    refund = {"order_id": "O-9001", "amount_cents": 8400, "reason": "customer request"}

    def gate(behaviour) -> dict[str, Any]:
        client = StubClient(behaviour)
        decision = jev.gate_refund(client, ticket, refund)
        return {
            "verdict": decision.verdict,
            "reason": decision.reason,
            "calls": client.calls,
            "probabilities": decision.probabilities,
        }

    def raises_jev_error(questions):
        raise jev.JevError("HTTP 503: upstream unavailable")

    def raises_unexpected(questions):
        raise TimeoutError("socket timed out")

    def empty_answers(questions):
        return jev.JevResult(model="stub", answers={})

    def missing_one(questions):
        answers = good_answers(questions)
        answers.pop("policy_covers")
        return jev.JevResult(model="stub", answers=answers)

    def out_of_range(questions):
        answers = good_answers(questions)
        answers["policy_covers"] = {"type": "noul", "noul": 1.4}
        return jev.JevResult(model="stub", answers=answers)

    def wrong_type(questions):
        answers = good_answers(questions)
        answers["policy_covers"] = {"type": "choice", "choice": "assume_yes",
                                    "probabilities": {"assume_yes": 0.99}}
        return jev.JevResult(model="stub", answers=answers)

    def string_probability(questions):
        answers = good_answers(questions)
        answers["policy_covers"] = {"type": "noul", "noul": "0.93"}
        return jev.JevResult(model="stub", answers=answers)

    def low_confidence(questions):
        answers = good_answers(questions)
        answers["policy_covers"] = {"type": "noul", "noul": 0.55}
        return jev.JevResult(model="stub", answers=answers)

    def all_good(questions):
        return jev.JevResult(model="stub", answers=good_answers(questions))

    def never_approves(output: dict[str, Any]) -> tuple[bool, str]:
        ok = output["verdict"] != "approve" and output["verdict"] in {"review", "block"} and bool(output["reason"])
        return ok, f"{output['verdict']} | {output['reason']}"

    def is_review(output: dict[str, Any]) -> tuple[bool, str]:
        ok = output["verdict"] == "review" and "fail closed" in output["reason"]
        return ok, f"{output['verdict']} | {output['reason']}"

    return [
        Case(
            "gate_fails_closed_on_jev_error", "jev_gate",
            "Upstream 503 (JevError) degrades to review",
            lambda: gate(raises_jev_error),
            is_review,
        ),
        Case(
            "gate_fails_closed_on_unexpected_exception", "jev_gate",
            "A non-JevError (timeout/bug) must not escape or approve",
            lambda: gate(raises_unexpected),
            is_review,
        ),
        Case(
            "gate_fails_closed_on_empty_answers", "jev_gate",
            "Response with no answers at all degrades to review",
            lambda: gate(empty_answers),
            is_review,
        ),
        Case(
            "gate_fails_closed_on_missing_answer", "jev_gate",
            "One proposition missing from the response degrades to review",
            lambda: gate(missing_one),
            is_review,
        ),
        Case(
            "gate_rejects_out_of_range_probability", "jev_gate",
            "Probability 1.4 is malformed, not 'very confident'",
            lambda: gate(out_of_range),
            is_review,
        ),
        Case(
            "gate_rejects_wrong_answer_type", "jev_gate",
            "A choice answer where a noul was asked degrades to review",
            lambda: gate(wrong_type),
            is_review,
        ),
        Case(
            "gate_rejects_string_probability", "jev_gate",
            "A stringified probability is not coerced into a number",
            lambda: gate(string_probability),
            is_review,
        ),
        Case(
            "gate_reviews_ambiguous_probability", "jev_gate",
            "0.55 is neither a clear yes nor a clear no -> human",
            lambda: gate(low_confidence),
            never_approves,
        ),
        Case(
            "gate_approves_only_when_every_check_clears", "jev_gate",
            "Positive control: a clean check still approves",
            lambda: gate(all_good),
            lambda o: (o["verdict"] == "approve", f"{o['verdict']} | {o['reason']}"),
        ),
        Case(
            "gate_precheck_blocks_without_model_call", "jev_gate",
            "Oversize refund is blocked by arithmetic, not by a model",
            lambda: (
                lambda client: {
                    "verdict": jev.gate_refund(
                        client, ticket,
                        {"order_id": "O-9001", "amount_cents": 99999, "reason": "customer request"},
                    ).verdict,
                    "calls": client.calls,
                }
            )(StubClient(all_good)),
            lambda o: (o["verdict"] == "block" and o["calls"] == 0, str(o)),
        ),
    ]
