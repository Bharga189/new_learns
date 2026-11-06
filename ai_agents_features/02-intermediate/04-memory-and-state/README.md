# 04 — Memory & State

Agents need state across turns. **Memory** is how you decide what past information the model sees and what is stored for later.

## What you'll learn

- **Short-term memory**: the active conversation window.
- **Summarization**: compress old turns when the window is full.
- **Long-term memory**: store durable facts/preferences and retrieve relevant ones.
- **State**: explicit variables (goal, plan, constraints) the agent can read/update.

## Pattern

```text
new message -> update short-term history
if history too large: summarize old turns -> keep summary + recent turns
retrieve relevant long-term facts -> include in context
model responds -> maybe write durable facts to long-term store
```

## Run

```bash
python 02-intermediate/04-memory-and-state/main.py
```

## Key ideas / best practices

- Summaries lose detail; keep recent/raw messages for the current task.
- Store long-term facts with provenance and timestamps; re-check volatile facts.
- Retrieve by relevance, don't dump the whole memory store into every prompt.
- Treat memory writes as policy decisions (what is safe/useful to remember?).
