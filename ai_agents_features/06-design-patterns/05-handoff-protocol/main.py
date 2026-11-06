"""Handoff protocol: structured envelope, correlation, ACK/error.

Run: python 06-design-patterns/05-handoff-protocol/main.py
"""

from __future__ import annotations

import uuid
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from typing import Any


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid.uuid4().hex[:8]}"


@dataclass
class Envelope:
    type: str
    sender: str
    receiver: str
    payload: dict[str, Any]
    id: str = field(default_factory=lambda: new_id("msg"))
    correlation_id: str | None = None
    status: str = "pending"  # pending|ack|done|error
    ts: str = field(default_factory=now)
    error: str | None = None


class Agent:
    def __init__(self, name: str) -> None:
        self.name = name

    def send(self, receiver: "Agent", type_: str, payload: dict, correlation_id: str | None = None) -> Envelope:
        env = Envelope(type=type_, sender=self.name, receiver=receiver.name, payload=payload, correlation_id=correlation_id)
        print(f"[send] {env.sender}->{env.receiver} {env.type} id={env.id} corr={env.correlation_id}")
        return receiver.receive(env)

    def receive(self, env: Envelope) -> Envelope:
        raise NotImplementedError


class Planner(Agent):
    def receive(self, env: Envelope) -> Envelope:
        if "goal" not in env.payload:
            return Envelope("handoff.ack", self.name, env.sender, {}, correlation_id=env.id, status="error", error="missing goal")
        ack = Envelope("handoff.ack", self.name, env.sender, {"accepted": True}, correlation_id=env.id, status="ack")
        print(f"[recv:{self.name}] ack id={ack.id} corr={ack.correlation_id}")
        result = Envelope("handoff.result", self.name, env.sender,
                          {"plan": [f"analyze {env.payload['goal']}", "execute safely"]},
                          correlation_id=env.id, status="done")
        print(f"[recv:{self.name}] done payload={result.payload}")
        return result


class Coordinator(Agent):
    def run(self, planner: Planner, goal: str) -> None:
        result = self.send(planner, "handoff.request", {"goal": goal, "constraints": ["no prod writes"]})
        print(f"[{self.name}] final status={result.status} result={result.payload if result.status=='done' else result.error}")


if __name__ == "__main__":
    Coordinator("coordinator").run(Planner("planner"), "improve onboarding")
    print("\n[example envelope]")
    print(asdict(Envelope("handoff.request", "a", "b", {"goal": "x"})))
