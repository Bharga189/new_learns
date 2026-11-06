"""CrewAI mapping: role-based agents, tasks, sequential process, HITL approval.

Run: python 05-frameworks/crewai/main.py
Mock demo runs without deps/keys.
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class Task:
    description: str
    agent: str
    expected_output: str
    output: str = ""


@dataclass
class Agent:
    role: str
    goal: str
    backstory: str
    tools: list[str] = field(default_factory=list)

    def execute(self, task: Task, context: list[str]) -> str:
        ctx = " | ".join(context) or "no context"
        return f"{self.role} result for '{task.description}' using [{ctx}]"


def run_mock() -> None:
    researcher = Agent("Researcher", "Find facts", "Careful analyst")
    planner = Agent("Planner", "Propose safe plan", "Operations planner")
    crew = [researcher, planner]
    tasks = [
        Task("Investigate 5xx spike", "Researcher", "facts"),
        Task("Propose remediation", "Planner", "plan"),
    ]
    context: list[str] = []
    by_role = {a.role: a for a in crew}
    for task in tasks:
        task.output = by_role[task.agent].execute(task, context)
        print(f"[task] {task.agent}: {task.output}")
        context.append(task.output)

    # HITL gate before high-impact execution.
    proposal = context[-1]
    high_impact = "remediation" in proposal.lower()
    approved = False if high_impact else True
    print(f"[hitl] high_impact={high_impact} approved={approved}")
    if approved:
        print("[execute] running approved plan")
    else:
        print("[execute] held - human approval required before production changes")


def run_crewai_note() -> None:
    # Real CrewAI structure (APIs may evolve):
    # from crewai import Agent, Task, Crew, Process
    # crew = Crew(agents=[...], tasks=[...], process=Process.sequential)
    # result = crew.kickoff(inputs={...})
    print("CrewAI installed-path placeholder: construct Agent/Task/Crew per current docs.")


if __name__ == "__main__":
    try:
        import crewai  # noqa: F401

        run_crewai_note()
    except Exception:
        print("[mode] mock fallback\n")
        run_mock()
