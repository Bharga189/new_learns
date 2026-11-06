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
    rel = load_module("03-advanced/09-reliability-patterns/main.py", "reliability")

    def retry_recovers():
        rel.EVAL_LOG.clear()
        service = rel.FlakyService(fail_times=2)
        result = rel.with_retry(service.call, attempts=3, fallback=lambda: "fallback")
        return {"result": result, "calls": service.calls, "ok_events": [e.ok for e in rel.EVAL_LOG]}

    def idempotent_retry():
        rel.PROCESSED_KEYS.clear()
        rel.SIDE_EFFECT_COUNT = 0
        key = "eval:key:1"
        rel.create_invoice(key)
        rel.create_invoice(key)
        return {"side_effects": rel.SIDE_EFFECT_COUNT}

    return [
        Case(
            "retry_recovers_transient", "tool_failures", "Retries recover transient errors",
            retry_recovers,
            lambda o: (o["result"] == "primary result" and o["calls"] == 3, str(o)),
        ),
        Case(
            "idempotency_prevents_duplicates", "tool_failures", "Duplicate calls do not duplicate side effects",
            idempotent_retry,
            lambda o: (o["side_effects"] == 1, str(o)),
        ),
    ]
