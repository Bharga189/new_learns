"""11 - Router / Supervisor: classify intent, delegate, aggregate.

Run: python 04-multi-agent/11-router-supervisor/main.py
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


class Worker(Protocol):
    name: str

    def handle(self, request: str) -> dict[str, str]: ...


@dataclass
class BillingAgent:
    name: str = "billing"

    def handle(self, request: str) -> dict[str, str]:
        return {"agent": self.name, "answer": f"Billing help: check invoice status for '{request}'."}


@dataclass
class TechAgent:
    name: str = "technical"

    def handle(self, request: str) -> dict[str, str]:
        return {"agent": self.name, "answer": f"Technical help: troubleshoot '{request}'."}


@dataclass
class FallbackAgent:
    name: str = "fallback"

    def handle(self, request: str) -> dict[str, str]:
        return {
            "agent": self.name,
            "answer": "I need clarification: is this a billing or technical issue?",
        }


class Supervisor:
    def __init__(self) -> None:
        self.workers: dict[str, Worker] = {
            "billing": BillingAgent(),
            "technical": TechAgent(),
            "fallback": FallbackAgent(),
        }

    def route(self, request: str) -> str:
        # Deterministic routing first. A real system may use an LLM classifier.
        text = request.lower()
        if any(k in text for k in ["invoice", "refund", "charge", "payment"]):
            return "billing"
        if any(k in text for k in ["error", "bug", "crash", "login", "api"]):
            return "technical"
        return "fallback"

    def handle(self, request: str) -> dict[str, str]:
        route = self.route(request)
        print(f"[route] '{request}' -> {route}")
        result = self.workers[route].handle(request)
        # Aggregation point: normalize/validate worker output before returning.
        return {"route": route, **result}


if __name__ == "__main__":
    sup = Supervisor()
    for req in [
        "I was double charged on my invoice",
        "The API returns a 500 error on login",
        "Can you help me?",
    ]:
        print(sup.handle(req))
        print()
