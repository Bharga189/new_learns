"""10 - Sequential Handoff: specialist agents in a pipeline with explicit contracts.

Run: python 04-multi-agent/10-sequential-handoff/main.py
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass
class Handoff:
    sender: str
    receiver: str
    payload: dict[str, Any]


class ResearchAgent:
    name = "researcher"

    def run(self, goal: str) -> Handoff:
        findings = [
            "Agents need bounded loops and explicit stopping conditions.",
            "Tool calls should be validated before execution.",
        ]
        return Handoff(self.name, "planner", {"goal": goal, "findings": findings})


class PlannerAgent:
    name = "planner"

    def run(self, handoff: Handoff) -> Handoff:
        findings = handoff.payload["findings"]
        plan = [f"Address: {f}" for f in findings] + ["Verify with a focused check."]
        return Handoff(self.name, "writer", {**handoff.payload, "plan": plan})


class WriterAgent:
    name = "writer"

    def run(self, handoff: Handoff) -> dict[str, Any]:
        plan = handoff.payload["plan"]
        text = "Recommendation:\n" + "\n".join(f"- {step}" for step in plan)
        return {"goal": handoff.payload["goal"], "output": text, "sources": [handoff.sender]}


def run_pipeline(goal: str) -> dict[str, Any]:
    r = ResearchAgent().run(goal)
    print(f"[handoff] {r.sender} -> {r.receiver}: {list(r.payload)}")
    p = PlannerAgent().run(r)
    print(f"[handoff] {p.sender} -> {p.receiver}: {list(p.payload)}")
    out = WriterAgent().run(p)
    print(f"[done] writer produced final output")
    return out


if __name__ == "__main__":
    result = run_pipeline("How should we build a safe first agent?")
    print("\n" + result["output"])
