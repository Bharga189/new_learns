# 02 — Tool Calling (wired to shared adapter + tracing)

Agents become useful when they can **act**: read data, do math, call APIs, query systems. Tool calling gives the model a structured way to request an action. This example is wired end-to-end to the repo's **shared real-provider adapter** and **trace spans**.

## What you'll learn

- **Tool schemas** (OpenAI-style `tools`) describe name, purpose, and arguments.
- The model returns **tool calls**; your code **dispatches** locally and sends **tool results** back.
- The model uses the results to produce the final answer.
- **Tracing**: each run records spans for `agent.run`, `llm.complete`, and `tool.dispatch` (see `shared/tracing.py`).

## Flow

```text
user -> adapter.complete(messages, tools) -> tool_call(name, args)
      -> dispatch (traced) -> tool result
tool_result -> adapter.complete(...) -> final answer (traced)
```

## Run

Deterministic mock demo (no keys, no network):

```bash
python 01-basic/02-tool-calling/main.py
```

Real OpenAI-compatible provider (end-to-end tool calling, if the model supports it):

```bash
export OPENAI_API_KEY="..."
# optional: OPENAI_BASE_URL, OPENAI_MODEL
python 01-basic/02-tool-calling/main.py
```

When `OPENAI_API_KEY` is set, `shared.llm_adapter.from_env()` provides the client; otherwise the example uses a local `ScriptedToolClient` with the same loop/trace behavior.

## Key ideas / best practices

- Validate arguments before execution (type, range, allowed values); keep tools least-privilege.
- Return structured errors so the model can recover or explain.
- Set a max tool-call budget to prevent infinite loops.
- Trace every LLM/tool step (redact secrets) for debugging and eval regression analysis.
- Never execute model-generated shell/code directly without policy checks.
