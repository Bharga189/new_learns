"""07 - RAG Agent: retrieve evidence, ground the answer, cite sources.

Run: python 03-advanced/07-rag-agent/main.py
"""

from __future__ import annotations

from dataclasses import dataclass


CORPUS = [
    {
        "id": "doc-1",
        "source": "runbook.md",
        "text": "Service alpha retries failed jobs 3 times with exponential backoff.",
    },
    {
        "id": "doc-2",
        "source": "runbook.md",
        "text": "Service alpha stores idempotency keys for 24 hours to prevent duplicates.",
    },
    {
        "id": "doc-3",
        "source": "architecture.md",
        "text": "The billing worker reads events from the payments queue.",
    },
]


_STOP = {
    "a", "an", "and", "are", "do", "does", "for", "how", "i", "in", "is",
    "it", "me", "my", "of", "on", "or", "the", "to", "what", "when", "where",
    "who", "with", "you", "your",
}


def _terms(text: str) -> set[str]:
    return {t for t in text.lower().split() if t not in _STOP}


def retrieve(query: str, k: int = 2, min_score: int = 1) -> list[dict[str, str]]:
    # Demo retriever: keyword overlap minus stop words. Real systems use embeddings + reranking.
    q = _terms(query)
    if not q:
        return []
    scored = []
    for doc in CORPUS:
        score = len(q & _terms(doc["text"]))
        scored.append((score, doc))
    scored.sort(key=lambda x: x[0], reverse=True)
    return [d for score, d in scored[:k] if score >= min_score]


@dataclass
class GroundedAnswer:
    answer: str
    citations: list[str]
    grounded: bool


class MockLLM:
    def answer(self, question: str, evidence: list[dict[str, str]]) -> GroundedAnswer:
        if not evidence:
            return GroundedAnswer(
                answer="I don't have enough information in the available sources to answer that.",
                citations=[],
                grounded=False,
            )
        # Real model: synthesize only from evidence; cite each supporting chunk.
        citations = [f"{d['source']}#{d['id']}" for d in evidence]
        facts = " ".join(d["text"] for d in evidence)
        return GroundedAnswer(
            answer=f"Based on the sources: {facts}",
            citations=citations,
            grounded=True,
        )


def rag_answer(question: str) -> GroundedAnswer:
    evidence = retrieve(question)
    print(f"[retrieve] {[(d['id'], d['source']) for d in evidence]}")
    # Guardrail: label retrieved text as evidence, not instructions.
    prompt_evidence = [
        {"role": "evidence", "id": d["id"], "source": d["source"], "text": d["text"]}
        for d in evidence
    ]
    _ = prompt_evidence  # would be passed to the model in a real implementation
    return MockLLM().answer(question, evidence)


if __name__ == "__main__":
    result = rag_answer("How does service alpha handle retries and duplicates?")
    print(f"[grounded={result.grounded}] {result.answer}")
    print(f"[citations] {result.citations}")

    print()
    result2 = rag_answer("What is the on-call phone number?")
    print(f"[grounded={result2.grounded}] {result2.answer}")
