"""Hierarchical delegation: manager -> specialists, with escalation.

Run: python 06-design-patterns/01-hierarchical-delegation/main.py
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class TaskResult:
    owner: str
    status: str  # ok | blocked | escalate
    output: str


class Worker:
    def __init__(self, name: str, can_handle: list[str]) -> None:
        self.name = name
        self.can_handle = can_handle

    def handle(self, task: str) -> TaskResult:
        if any(k in task.lower() for k in self.can_handle):
            return TaskResult(self.name, "ok", f"completed: {task}")
        return TaskResult(self.name, "escalate", f"outside scope: {task}")


class Manager:
    def __init__(self, name: str, workers: list[Worker]) -> None:
        self.name = name
        self.workers = workers

    def delegate(self, task: str) -> TaskResult:
        print(f"[{self.name}] delegating: {task}")
        for w in self.workers:
            result = w.handle(task)
            if result.status == "ok":
                print(f"[{self.name}] accepted from {w.name}")
                return result
        escalated = TaskResult(self.name, "escalate", f"needs higher-level decision: {task}")
        print(f"[{self.name}] escalating")
        return escalated


class Director:
    def run(self, goal: str, subtasks: list[str]) -> list[TaskResult]:
        eng = Manager("eng-manager", [Worker("backend", ["api", "db"]), Worker("frontend", ["ui"])])
        ops = Manager("ops-manager", [Worker("sre", ["deploy", "monitor"])])
        managers = [eng, ops]
        results = []
        print(f"[director] goal: {goal}")
        for task in subtasks:
            handled = None
            for m in managers:
                r = m.delegate(task)
                if r.status == "ok":
                    handled = r
                    break
            if handled is None:
                handled = TaskResult("director", "blocked", f"unresolved/escalated: {task}")
                print(f"[director] blocked: {task}")
            results.append(handled)
        return results


if __name__ == "__main__":
    results = Director().run(
        "Ship feature safely",
        ["build API endpoint", "design UI form", "set legal policy"],
    )
    print("\n[summary]")
    for r in results:
        print(f"- {r.status:8} {r.owner:10} {r.output}")
