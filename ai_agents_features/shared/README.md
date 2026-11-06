# Shared: Real Provider Adapter + Streaming + Tracing

This folder shows a small, provider-agnostic pattern for plugging a **real model** into the examples, with **streaming** and **tracing/eval hooks**.

## Files

- `llm_adapter.py` — `LLMClient` interface, `MockLLMClient` (default, no keys), and `OpenAICompatibleClient` (works with any OpenAI-compatible chat completions API via `OPENAI_API_KEY` + optional `OPENAI_BASE_URL` / `OPENAI_MODEL`). Uses only stdlib `urllib` for the real path to avoid required deps; swap for the official SDK in production.
- `tracing.py` — lightweight span/trace hooks (start/end events, attributes, errors). Replace with OpenTelemetry/LangSmith/etc. in production.

## Real provider (OpenAI-compatible)

```bash
export OPENAI_API_KEY="..."
# optional:
export OPENAI_BASE_URL="https://api.openai.com/v1"
export OPENAI_MODEL="gpt-4.1-mini"
python -c "from shared.llm_adapter import from_env; print(from_env().complete([{'role':'user','content':'Say hi'}]).text)"
```

## Streaming

```python
from shared.llm_adapter import from_env
for chunk in from_env().stream([{"role":"user","content":"Count to 3"}]):
    print(chunk.delta, end="")
```

The adapter emits deltas; your UI/agent loop decides how to render and when to treat the stream as a tool call / final answer.

## Tracing

Wrap calls with `trace_span(...)` to capture inputs (redacted as needed), outputs, latency, token/cost metadata, and errors. The eval harness reads the same event shape. In production, export spans via OpenTelemetry and add prompt/version attributes for regression analysis.
