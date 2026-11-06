"""01 - Hello Agent: the minimal message loop.

Run: python 01-basic/01-hello-agent/main.py
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class MockLLM:
    """Scripted stand-in for a real model. Swap with an API client in production."""

    scripted: list[str]
    i: int = 0

    def complete(self, messages: list[dict[str, str]]) -> str:
        # Real implementation: call your provider with `messages` and return text.
        # The message list shows the full context the model sees each turn.
        _ = messages
        if self.i < len(self.scripted):
            reply = self.scripted[self.i]
            self.i += 1
            return reply
        return "I don't have a scripted response for that."


@dataclass
class Agent:
    system_prompt: str
    llm: MockLLM
    messages: list[dict[str, str]] = field(default_factory=list)

    def __post_init__(self) -> None:
        self.messages.append({"role": "system", "content": self.system_prompt})

    def chat(self, user_text: str) -> str:
        self.messages.append({"role": "user", "content": user_text})
        reply = self.llm.complete(self.messages)
        self.messages.append({"role": "assistant", "content": reply})
        return reply


def main() -> None:
    agent = Agent(
        system_prompt=(
            "You are a helpful engineering tutor. Be concise and practical. "
            "If you are unsure, say so."
        ),
        llm=MockLLM(
            scripted=[
                "An agent is a program that uses a model to decide what to do next, "
                "often in a loop with tools and memory.",
                "Start with a single clear task, one or two tools, and explicit "
                "stopping conditions before adding complexity.",
            ]
        ),
    )

    demo_inputs = [
        "What is an AI agent in one sentence?",
        "What's the best first step when building one?",
    ]
    for text in demo_inputs:
        print(f"user: {text}")
        print(f"assistant: {agent.chat(text)}\n")

    print(f"[debug] total messages in history: {len(agent.messages)}")


if __name__ == "__main__":
    main()
