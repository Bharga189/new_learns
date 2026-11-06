# Semantic Kernel (framework mapping)

## Description

**Semantic Kernel (SK)** is an SDK (Microsoft; strong .NET and Python support) for integrating AI models with conventional code. Core concepts include the **kernel**, **plugins/functions** (native code and prompt templates), memory/connectors, and higher-level **agent**/orchestration patterns. SK is often used in enterprise apps where you want explicit function calling, dependency injection, filters, and governance around model interactions.

## Pattern mapping (this repo → Semantic Kernel)

| Repo pattern | Semantic Kernel concept |
|---|---|
| Tool calling (02) | Plugins/functions exposed to the model; automatic/manual function invocation |
| Planning (05) | Planner/agent patterns (where used) or explicit orchestration with state |
| Structured output/guardrails (06) | Prompt/function schemas, validation, filters, and policy checks |
| RAG (07) | Memory/vector-store connectors + grounding in prompt/functions |
| Router/supervisor / multi-agent (11) | Agent abstractions and orchestration; explicit routing in app logic |
| Human-in-the-loop | Function filters / approval middleware before high-impact plugin functions |

## Real-time usage cases

- **Enterprise copilot (internal apps):** answer employee questions using grounded data/plugins (HR, IT, docs) with permission-aware retrieval and audit logs.
- **Customer operations assistant:** read account/order state via plugins, draft actions, and require approval before issuing credits, changing subscriptions, or sending notices.
- **Document/workflow automation:** extract/validate data, call business APIs via plugins, and pause for human sign-off on regulated steps.
- **IT service desk:** diagnose via tools, propose remediations, and gate privileged operations (access changes, device actions) behind HITL.
- **Reporting/analytics assistant:** generate queries/summaries through approved functions; require review before exporting sensitive data.

## Human-in-the-loop (HITL) for high-impact actions

- Put **high-impact operations in dedicated plugin functions** (not generic "do anything" functions).
- Add an **approval filter/middleware** that intercepts those function calls and requires explicit, authenticated human approval with an action summary.
- Enforce least privilege in function implementations; validate arguments and targets.
- Record who/what/when/why in telemetry; design functions to be **idempotent** for safe retries/resumes.

## Run

```bash
pip install -r 05-frameworks/semantic-kernel/requirements.txt
python 05-frameworks/semantic-kernel/main.py
```

`main.py` runs a **mock kernel/plugin** flow without deps/keys and shows the SK-style mapping. Configure an AI service connector (OpenAI/Azure OpenAI/etc.) per current Semantic Kernel docs for live runs.

## Notes

Semantic Kernel’s Python/.NET APIs and agent abstractions evolve; check current docs for kernel setup, function calling, filters, and memory connectors.
