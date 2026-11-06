"""03 - ReAct loop: Thought -> Action -> Observation -> ... -> Answer.

Run: python 01-basic/03-react-loop/main.py
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any, Callable


def lookup_order(order_id: str) -> dict[str, Any]:
    orders = {"A100": {"status": "shipped", "eta": "2026-11-08"}}
    return orders.get(order_id, {"error": "order not found"})


TOOLS: dict[str, Callable[..., dict[str, Any]]] = {"lookup_order": lookup_order}


@dataclass
class Step:
    thought: str
    action: str | None = None  # tool name
    action_input: dict[str, Any] | None = None
    answer: str | None = None


class MockReActLLM:
    """Scripted ReAct responses. A real model would generate these steps."""

    def __init__(self) -> None:
        self._steps = [
            Step(
                thought="I need the current status for order A100.",
                action="lookup_order",
                action_input={"order_id": "A100"},
            ),
            Step(
                thought="The observation shows the order shipped with an ETA. I can answer.",
                answer="Order A100 has shipped and the ETA is 2026-11-08.",
            ),
        ]
        self._i = 0

    def next_step(self, transcript: str) -> Step:
        _ = transcript
        step = self._steps[self._i]
        self._i += 1
        return step


def run_react(question: str, max_steps: int = 6) -> str:
    llm = MockReActLLM()
    transcript = f"Question: {question}\n"

    for step_num in range(1, max_steps + 1):
        step = llm.next_step(transcript)
        transcript += f"Thought {step_num}: {step.thought}\n"
        print(f"Thought {step_num}: {step.thought}")

        if step.answer is not None:
            transcript += f"Answer: {step.answer}\n"
            print(f"Answer: {step.answer}")
            return step.answer

        if not step.action:
            return "Stopped: model produced neither an action nor an answer."

        tool = TOOLS.get(step.action)
        if tool is None:
            observation = {"error": f"unknown tool: {step.action}"}
        else:
            observation = tool(**(step.action_input or {}))

        obs_text = json.dumps(observation)
        transcript += f"Action {step_num}: {step.action}({json.dumps(step.action_input)})\n"
        transcript += f"Observation {step_num}: {obs_text}\n"
        print(f"Action {step_num}: {step.action}({json.dumps(step.action_input)})")
        print(f"Observation {step_num}: {obs_text}")

    return "Stopped: max steps reached without a final answer."


if __name__ == "__main__":
    run_react("Where is my order A100?")
