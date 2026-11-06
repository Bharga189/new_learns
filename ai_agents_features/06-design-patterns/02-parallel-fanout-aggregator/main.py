"""Parallel fan-out / aggregator: independent subtasks -> aggregate.

Run: python 06-design-patterns/02-parallel-fanout-aggregator/main.py
"""

from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass


@dataclass
class WorkerResult:
    source: str
    ok: bool
    data: str


def check_source(name: str, query: str) -> WorkerResult:
    # Independent lookups; safe to parallelize.
    fake_db = {
        "docs": f"docs say: retry idempotent calls for '{query}'",
        "runbook": f"runbook says: use backoff and tracing for '{query}'",
        "alerts": f"alerts: no active incident related to '{query}'",
    }
    if name == "alerts":
        return WorkerResult(name, True, fake_db[name])
    return WorkerResult(name, name in fake_db, fake_db.get(name, "no data"))


def aggregate(results: list[WorkerResult]) -> dict:
    ok = [r for r in results if r.ok]
    failed = [r for r in results if not r.ok]
    # Aggregation policy: keep evidence, surface partial failures explicitly.
    return {
        "summary": " | ".join(r.data for r in ok),
        "sources_ok": [r.source for r in ok],
        "sources_failed": [r.source for r in failed],
    }


def fanout(query: str, sources: list[str], max_workers: int = 4) -> dict:
    results: list[WorkerResult] = []
    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        futures = {pool.submit(check_source, s, query): s for s in sources}
        for fut in as_completed(futures):
            r = fut.result()
            print(f"[worker:{r.source}] ok={r.ok}")
            results.append(r)
    return aggregate(results)


if __name__ == "__main__":
    out = fanout("API failures", ["docs", "runbook", "alerts"])
    print("\n[aggregated]", out)
