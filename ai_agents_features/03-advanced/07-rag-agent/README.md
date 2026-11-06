# 07 — RAG Agent (Retrieval-Augmented Generation)

A **RAG agent** answers using retrieved evidence instead of relying only on model memory. Retrieval reduces hallucination for factual, project-specific, or current information.

## What you'll learn

- **Retrieve** relevant documents/chunks for the question.
- **Ground** the answer in retrieved evidence and cite sources.
- **Abstain** or qualify the answer when evidence is missing/conflicting.
- Treat retrieved content as *data*, not instructions (prompt-injection awareness).

## Pattern

```text
question -> retrieve top-k evidence -> build grounded prompt
         -> model answers with citations -> verify/format response
```

## Run

```bash
python 03-advanced/07-rag-agent/main.py
```

Uses a tiny in-memory corpus and keyword retrieval for clarity. Real systems use embeddings, a vector DB, reranking, and access controls.

## Key ideas / best practices

- Chunk documents with useful metadata (source, section, timestamp, permissions).
- Retrieve narrowly; too much irrelevant context hurts answer quality.
- Require citations or evidence references for factual claims.
- If retrieval returns nothing relevant, say so rather than guessing.
- Sanitize/label retrieved text to reduce prompt injection risk.
