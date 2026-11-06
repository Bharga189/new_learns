from __future__ import annotations

import importlib.util
from pathlib import Path

from evals.harness import Case

ROOT = Path(__file__).resolve().parents[2]


def load_module(rel_path: str, name: str):
    spec = importlib.util.spec_from_file_location(name, ROOT / rel_path)
    mod = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    import sys
    sys.modules[name] = mod  # required for dataclasses/type resolution
    spec.loader.exec_module(mod)
    return mod


def cases() -> list[Case]:
    router = load_module("04-multi-agent/11-router-supervisor/main.py", "router_sup")
    guard = load_module(
        "02-intermediate/06-structured-output-and-guardrails/main.py", "guardrails"
    )
    rag = load_module("03-advanced/07-rag-agent/main.py", "rag_agent")

    def run_router():
        return router.Supervisor().handle("The API returns a 500 error on login")

    def run_guardrails():
        return guard.get_structured_action("Create a high priority ticket for login errors.")

    def run_rag_grounded():
        return rag.rag_answer("How does service alpha handle retries and duplicates?")

    def run_rag_abstain():
        return rag.rag_answer("What is the on-call phone number?")

    return [
        Case(
            "router_technical", "golden", "Routes technical issue to technical agent",
            run_router,
            lambda o: (o.get("route") == "technical", f"route={o.get('route')}"),
        ),
        Case(
            "guardrails_valid_action", "golden", "Produces validated, allowed action",
            run_guardrails,
            lambda o: (
                o.get("action") in {"create_ticket", "send_summary"} and bool(o.get("title")),
                f"action={o.get('action')}",
            ),
        ),
        Case(
            "rag_grounded", "golden", "Grounds answer when evidence exists",
            run_rag_grounded,
            lambda o: (o.grounded and len(o.citations) > 0, f"grounded={o.grounded}"),
        ),
        Case(
            "rag_abstains", "golden", "Abstains when evidence is missing",
            run_rag_abstain,
            lambda o: (o.grounded is False, f"grounded={o.grounded}"),
        ),
    ]
