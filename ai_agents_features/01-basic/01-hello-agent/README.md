# 01 — Hello Agent

The smallest useful agent: a loop that sends conversation history to a model and appends the model's reply.

## What you'll learn

- An "agent" starts as a **message loop**: system instructions + user turns + assistant turns.
- The model is stateless between calls; **you** own the state by resending history.
- A clear system prompt defines role, boundaries, and output style.

## How it works

1. Build `messages` with a `system` prompt and the user's input.
2. Call the LLM (`llm_call`) and get an assistant message.
3. Append the assistant message to history so the next turn has context.
4. Repeat until the user exits.

## Run

```bash
python 01-basic/01-hello-agent/main.py
```

The demo uses a `MockLLM` (scripted responses) so it runs without API keys. To use a real model, replace `MockLLM` with a provider client in `llm_call` and pass `messages` in the provider's format.

## Key ideas / best practices

- Keep the system prompt short and specific about the task and limits.
- Cap history length (token/window budget) before it grows unbounded — see example 04.
- Log/trace messages in production (with redaction) for debugging and evals.
