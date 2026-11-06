"""05 - Planning & Task Decomposition: plan -> execute -> replan.

Run: python 02-intermediate/05-planning-and-task-decomposition/main.py
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Literal

Status = Literal["pending", "done", "blocked"]


@dataclass
class Task:
    id: int
    title: str
    status: Status = "pending"
    note: str = ""


@dataclass
class PlannerAgent:
    goal: str
    tasks: list[Task] = field(default_factory=list)
    replans: int = 0
    max_replans: int = 2

    def plan(self) -> None:
        # Mock planner: real systems ask the model to decompose the goal.
        self.tasks = [
            Task(1, "Inspect current configuration"),
            Task(2, "Apply required change"),
            Task(3, "Run verification check"),
        ]
        print("[plan]", [t.title for t in self.tasks])

    def execute(self, task: Task) -> tuple[bool, str]:
        # Mock execution: task 2 fails once to demonstrate replanning.
        if task.id == 2 and self.replans == 0:
            return False, "change failed: missing permission"
        return True, f"completed: {task.title}"

    def replan(self, failed: Task, reason: str) -> None:
        if self.replans >= self.max_replans:
            failed.status = "blocked"
            failed.note = f"replan limit reached: {reason}"
            return
        self.replans += 1
        print(f"[replan #{self.replans}] reason: {reason}")
        # Insert a prerequisite step before retrying the failed task.
        insert_at = next(i for i, t in enumerate(self.tasks) if t.id == failed.id)
        new_task = Task(id=10 + self.replans, title="Request or fix permission")
        failed.status = "pending"
        self.tasks.insert(insert_at, new_task)

    def run(self) -> str:
        self.plan()
        steps = 0
        while steps < 20:
            steps += 1
            next_task = next((t for t in self.tasks if t.status == "pending"), None)
            if next_task is None:
                break
            print(f"[do] {next_task.id}: {next_task.title}")
            ok, note = self.execute(next_task)
            next_task.note = note
            if ok:
                next_task.status = "done"
                print(f"[ok] {note}")
            else:
                print(f"[fail] {note}")
                self.replan(next_task, note)

        done = [t for t in self.tasks if t.status == "done"]
        blocked = [t for t in self.tasks if t.status == "blocked"]
        return (
            f"Goal: {self.goal}\n"
            f"Done: {len(done)}/{len(self.tasks)} | Blocked: {len(blocked)} | "
            f"Replans: {self.replans}\n"
            + "\n".join(f"- [{t.status}] {t.title} ({t.note})" for t in self.tasks)
        )


if __name__ == "__main__":
    print(PlannerAgent(goal="Apply config change safely").run())
