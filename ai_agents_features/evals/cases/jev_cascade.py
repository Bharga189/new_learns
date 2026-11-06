"""Evals for the Jev cost cascade.

The rule under test: an unverified draft is **never** accepted. When the cheap
tier's check breaks, the cascade must move *up* (strong draft, verified) or hand
off to a human -- it must never ship the cheap draft on a missing or malformed
verdict. A positive control keeps the suite from passing vacuously: a clean cheap
draft still gets accepted without paying for the strong model.
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


CASCADE = load_module("05-frameworks/jev/cascade.py", "jev_cascade")

CHEAP = CASCADE.ScriptedModel("cheap", CASCADE.COST_CHEAP, {"E-01": "cheap draft"})
STRONG = CASCADE.ScriptedModel("strong", CASCADE.COST_STRONG, {"E-01": "strong draft"})


def case() -> dict[str, Any]:
    return {"id": "E-01", "customer_message": "The mug arrived cracked. What now?"}


def verdicts(grounded: float, resolves: float) -> dict[str, dict[str, Any]]:
    return {
        "grounded": {"type": "noul", "noul": grounded},
        "resolves": {"type": "noul", "noul": resolves},
    }


def run(cheap: float, strong: float, broken: dict[tuple[str, str], str] | None = None) -> dict[str, Any]:
    client = CASCADE.TableJev(
        {("E-01", "cheap"): verdicts(cheap, cheap), ("E-01", "strong"): verdicts(strong, strong)},
        broken=broken,
    )
    outcome = CASCADE.run_cascade(client, case(), CHEAP, STRONG)
    return {
        "handled_by": outcome.handled_by,
        "accepted": outcome.accepted,
        "probabilities": outcome.probabilities,
        "llm_calls": outcome.llm_calls,
        "verify_calls": outcome.verify_calls,
        "cost": outcome.cost,
        "reason": outcome.reason,
    }


def cases() -> list[Case]:
    def never_ungrounded(output: dict[str, Any]) -> tuple[bool, str]:
        """The core invariant: no acceptance without a real, complete verdict."""
        if output["accepted"] and len(output["probabilities"]) != 2:
            return False, f"accepted without a full verdict: {output}"
        return True, f"{output['handled_by']} accepted={output['accepted']}"

    def escalated_not_accepted(output: dict[str, Any]) -> tuple[bool, str]:
        ok = output["handled_by"] != "cheap" and output["llm_calls"] >= 1
        return ok, f"{output['handled_by']} accepted={output['accepted']}"

    return [
        Case(
            "cascade_accepts_clean_cheap_draft", "jev_cascade",
            "Positive control: clean cheap draft is accepted without the strong model",
            lambda: run(cheap=0.95, strong=0.99),
            lambda o: (
                o["handled_by"] == "cheap"
                and o["accepted"]
                and o["llm_calls"] == 1
                and abs(o["cost"] - (CASCADE.COST_CHEAP + CASCADE.COST_VERIFY)) < 1e-9,
                str(o),
            ),
        ),
        Case(
            "cascade_escalates_when_cheap_draft_ungrounded", "jev_cascade",
            "Grounded 0.30 escalates and accepts the verified strong draft",
            lambda: run(cheap=0.30, strong=0.95),
            lambda o: (o["handled_by"] == "strong" and o["accepted"] and o["llm_calls"] == 2, str(o)),
        ),
        Case(
            "cascade_broken_cheap_check_never_accepts", "jev_cascade",
            "A raising cheap check escalates instead of accepting the cheap draft",
            lambda: run(cheap=0.99, strong=0.95, broken={("E-01", "cheap"): "raise"}),
            lambda o: (
                o["handled_by"] == "strong" and o["accepted"] and o["verify_calls"] == 2,
                f"{o['handled_by']} accepted={o['accepted']} verify_calls={o['verify_calls']}",
            ),
        ),
        Case(
            "cascade_empty_cheap_verdict_never_accepts", "jev_cascade",
            "Cheap check returning no answers escalates, even with good drafts",
            lambda: run(cheap=0.99, strong=0.96, broken={("E-01", "cheap"): "empty"}),
            lambda o: (o["handled_by"] == "strong" and o["accepted"], str(o)),
        ),
        Case(
            "cascade_both_checks_broken_goes_to_human", "jev_cascade",
            "Broken cheap AND strong checks end at a human, never an acceptance",
            lambda: run(
                cheap=0.99, strong=0.99,
                broken={("E-01", "cheap"): "raise", ("E-01", "strong"): "empty"},
            ),
            lambda o: (o["handled_by"] == "human" and not o["accepted"] and not o["probabilities"], str(o)),
        ),
        Case(
            "cascade_unresolved_strong_draft_goes_to_human", "jev_cascade",
            "Grounded but non-resolving strong draft (0.55) is not accepted",
            lambda: run(cheap=0.20, strong=0.55),
            lambda o: (o["handled_by"] == "human" and not o["accepted"], str(o)),
        ),
        Case(
            "cascade_never_accepts_unverified_draft", "jev_cascade",
            "Invariant across failure modes: no acceptance without a full verdict",
            lambda: [
                run(cheap=0.99, strong=0.99, broken={("E-01", "cheap"): "raise"}),
                run(cheap=0.99, strong=0.99, broken={("E-01", "cheap"): "empty"}),
                run(
                    cheap=0.99, strong=0.99,
                    broken={("E-01", "cheap"): "empty", ("E-01", "strong"): "raise"},
                ),
            ],
            lambda outputs: (
                all(never_ungrounded(o)[0] for o in outputs),
                "; ".join(never_ungrounded(o)[1] for o in outputs),
            ),
        ),
        Case(
            "cascade_unverifiable_is_not_verified", "jev_cascade",
            "verify() reports None on a broken check rather than a false 0.0",
            lambda: (
                lambda client: {
                    "result": CASCADE.verify(client, case(), "draft", "cheap")[0],
                    "note": CASCADE.verify(client, case(), "draft", "cheap")[1],
                }
            )(CASCADE.TableJev({}, broken={("E-01", "cheap"): "raise"})),
            lambda o: (o["result"] is None and "check unavailable" in o["note"], str(o)),
        ),
    ]
