"""09 - Reliability Patterns: retries, backoff, fallback, idempotency, eval hooks.

Run: python 03-advanced/09-reliability-patterns/main.py
"""

from __future__ import annotations

import random
import time
from dataclasses import dataclass, field
from typing import Any, Callable


class TransientError(RuntimeError):
    pass


@dataclass
class EvalEvent:
    name: str
    ok: bool
    attempts: int
    latency_ms: float
    detail: str = ""


EVAL_LOG: list[EvalEvent] = []


def with_retry(
    fn: Callable[[], Any],
    *,
    attempts: int = 3,
    base_delay_s: float = 0.05,
    fallback: Callable[[], Any] | None = None,
) -> Any:
    start = time.time()
    last_err: Exception | None = None
    for attempt in range(1, attempts + 1):
        try:
            result = fn()
            EVAL_LOG.append(
                EvalEvent("call", True, attempt, (time.time() - start) * 1000)
            )
            return result
        except TransientError as exc:
            last_err = exc
            print(f"[retry] attempt {attempt} failed: {exc}")
            if attempt < attempts:
                # Jittered exponential backoff.
                delay = base_delay_s * (2 ** (attempt - 1)) * (0.5 + random.random())
                time.sleep(delay)
        except Exception:
            raise  # non-transient: fail fast
    if fallback is not None:
        print("[fallback] using safe fallback")
        result = fallback()
        EVAL_LOG.append(
            EvalEvent(
                "call",
                True,
                attempts,
                (time.time() - start) * 1000,
                detail="fallback",
            )
        )
        return result
    EVAL_LOG.append(
        EvalEvent("call", False, attempts, (time.time() - start) * 1000, detail=str(last_err))
    )
    raise RuntimeError(f"failed after {attempts} attempts") from last_err


# --- Idempotency demo: retries must not duplicate side effects ---

PROCESSED_KEYS: set[str] = set()
SIDE_EFFECT_COUNT = 0


def create_invoice(idempotency_key: str) -> dict[str, Any]:
    global SIDE_EFFECT_COUNT
    if idempotency_key in PROCESSED_KEYS:
        return {"status": "duplicate_ignored", "key": idempotency_key}
    PROCESSED_KEYS.add(idempotency_key)
    SIDE_EFFECT_COUNT += 1
    return {"status": "created", "key": idempotency_key}


# --- Mock flaky operation ---

@dataclass
class FlakyService:
    fail_times: int
    calls: int = 0

    def call(self) -> str:
        self.calls += 1
        if self.calls <= self.fail_times:
            raise TransientError("temporary upstream timeout")
        return "primary result"


if __name__ == "__main__":
    random.seed(0)

    service = FlakyService(fail_times=2)
    result = with_retry(service.call, attempts=3, fallback=lambda: "fallback result")
    print(f"[result] {result}\n")

    # Safe to retry because of idempotency key.
    key = "invoice:user-42:2026-11"
    print(create_invoice(key))
    print(create_invoice(key))  # retry/duplicate call
    print(f"[side_effect_count] {SIDE_EFFECT_COUNT} (expected 1)\n")

    print("[eval]")
    for e in EVAL_LOG:
        print(f"  - {e}")
