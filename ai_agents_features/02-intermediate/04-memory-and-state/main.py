"""04 - Memory & State: short-term window, summarization, long-term recall.

Run: python 02-intermediate/04-memory-and-state/main.py
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone


@dataclass
class MemoryStore:
    """Simple long-term store. Production systems use a DB/vector store + metadata."""

    facts: list[dict[str, str]] = field(default_factory=list)

    def remember(self, text: str, source: str) -> None:
        self.facts.append(
            {
                "text": text,
                "source": source,
                "ts": datetime.now(timezone.utc).isoformat(),
            }
        )

    def recall(self, query: str, limit: int = 3) -> list[dict[str, str]]:
        # Demo retrieval: keyword overlap. Real systems use embeddings/ranking.
        terms = set(query.lower().split())
        scored = []
        for fact in self.facts:
            score = len(terms & set(fact["text"].lower().split()))
            if score:
                scored.append((score, fact))
        scored.sort(key=lambda x: x[0], reverse=True)
        return [f for _, f in scored[:limit]]


class MockLLM:
    def summarize(self, messages: list[dict[str, str]]) -> str:
        users = [m["content"] for m in messages if m["role"] == "user"]
        return "Earlier conversation summary: user discussed " + "; ".join(users[:2])

    def answer(self, context: str, user_text: str) -> str:
        if "timezone" in user_text.lower() and "UTC+3" in context:
            return "Noted from memory: your timezone is UTC+3. I can schedule accordingly."
        return f"(mock reply using context) You said: {user_text}"


@dataclass
class StatefulAgent:
    llm: MockLLM
    memory: MemoryStore
    max_messages: int = 4
    messages: list[dict[str, str]] = field(default_factory=list)

    def _compact_if_needed(self) -> None:
        if len(self.messages) <= self.max_messages:
            return
        old = self.messages[:-2]  # keep the most recent exchange raw
        summary = self.llm.summarize(old)
        self.messages = [{"role": "system", "content": summary}] + self.messages[-2:]
        print(f"[memory] compacted history -> {summary}")

    def chat(self, user_text: str) -> str:
        recalled = self.memory.recall(user_text)
        context = "\n".join(f"- {f['text']}" for f in recalled) or "(no relevant memory)"
        print(f"[memory] recalled: {context}")

        self.messages.append({"role": "user", "content": user_text})
        self._compact_if_needed()
        prompt_context = context + "\n" + "\n".join(
            f"{m['role']}: {m['content']}" for m in self.messages
        )
        reply = self.llm.answer(prompt_context, user_text)
        self.messages.append({"role": "assistant", "content": reply})

        # Policy decision: store durable, user-confirmed preferences/facts.
        if "timezone" in user_text.lower() and "utc" in user_text.lower():
            self.memory.remember("User timezone preference: UTC+3", source="user")
            print("[memory] stored durable fact")
        return reply


if __name__ == "__main__":
    agent = StatefulAgent(llm=MockLLM(), memory=MemoryStore())
    turns = [
        "My timezone is UTC+3.",
        "Tell me about agent memory.",
        "How do summaries help?",
        "Can you plan around my timezone?",
    ]
    for t in turns:
        print(f"\nuser: {t}")
        print(f"assistant: {agent.chat(t)}")
