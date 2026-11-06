"""13 - Shared Blackboard: agents collaborate through controlled shared state.

Run: python 04-multi-agent/13-shared-blackboard/main.py
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class Entry:
    author: str
    kind: str
    value: Any
    evidence: str = ""


@dataclass
class Blackboard:
    goal: str
    entries: list[Entry] = field(default_factory=list)
    status: str = "open"

    def write(self, entry: Entry, allowed_kinds: set[str]) -> None:
        # Controlled writes: agents can only contribute specific kinds.
        if entry.kind not in allowed_kinds:
            raise PermissionError(f"{entry.author} cannot write kind={entry.kind}")
        self.entries.append(entry)
        print(f"[write] {entry.author} -> {entry.kind}: {entry.value}")

    def read(self, kind: str) -> list[Entry]:
        return [e for e in self.entries if e.kind == kind]


class DiagnosticsAgent:
    name = "diagnostics"
    writes = {"fact"}

    def can_contribute(self, bb: Blackboard) -> bool:
        return not bb.read("fact")

    def run(self, bb: Blackboard) -> None:
        bb.write(
            Entry(self.name, "fact", "error_rate_spike=true", evidence="metrics:5xx > 3%"),
            self.writes,
        )


class CodeAgent:
    name = "code"
    writes = {"hypothesis"}

    def can_contribute(self, bb: Blackboard) -> bool:
        facts = [e.value for e in bb.read("fact")]
        return "error_rate_spike=true" in facts and not bb.read("hypothesis")

    def run(self, bb: Blackboard) -> None:
        bb.write(
            Entry(self.name, "hypothesis", "recent deploy may have introduced regression",
                  evidence="deploy timestamp matches spike"),
            self.writes,
        )


class CoordinatorAgent:
    name = "coordinator"
    writes = {"plan", "status"}

    def can_contribute(self, bb: Blackboard) -> bool:
        return bool(bb.read("hypothesis")) and not bb.read("plan")

    def run(self, bb: Blackboard) -> None:
        # Conflict handling (simple): if contradictory hypotheses existed, an arbiter
        # would compare evidence/confidence here. Demo has one hypothesis.
        bb.write(
            Entry(self.name, "plan", ["inspect recent deploy", "compare error traces", "rollback if confirmed"]),
            self.writes,
        )
        bb.write(Entry(self.name, "status", "ready_for_execution"), self.writes)
        bb.status = "ready_for_execution"


def run_blackboard(goal: str, max_rounds: int = 5) -> Blackboard:
    bb = Blackboard(goal=goal)
    agents = [DiagnosticsAgent(), CodeAgent(), CoordinatorAgent()]
    for round_num in range(1, max_rounds + 1):
        print(f"\n[round {round_num}] status={bb.status}")
        acted = False
        for agent in agents:
            if agent.can_contribute(bb):
                agent.run(bb)
                acted = True
        if bb.status != "open" or not acted:
            break
    return bb


if __name__ == "__main__":
    board = run_blackboard("Investigate production 5xx spike")
    print(f"\n[final status] {board.status}")
    print("[entries]")
    for e in board.entries:
        print(f"  - {e.author}/{e.kind}: {e.value} ({e.evidence})")
