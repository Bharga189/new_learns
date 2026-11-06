# Agentic Engineering Examples

A curated, runnable set of examples for learning **agent development** from basic to advanced, including **multi-agent communication** patterns and best practices.

## How to use this repo

- Each folder is a small, self-contained project with a `README.md` (concepts + walkthrough) and `main.py` (runnable code).
- Examples run with **no API keys** by default using a `MockLLM`/scripted adapter so you can study the control flow. Swap in a real provider where noted (OpenAI, Anthropic, local models, etc.).
- Requirements: Python 3.10+. Most examples use only the standard library; any extras are listed in the example's `requirements.txt`.

```bash
python 01-basic/01-hello-agent/main.py
```

## Learning path

### 01 — Basic
| # | Project | Concepts |
|---|---------|----------|
| 01 | [Hello Agent](01-basic/01-hello-agent/) | Minimal agent loop, system prompt, user/assistant turns |
| 02 | [Tool Calling](01-basic/02-tool-calling/) | Function schemas, tool dispatch, tool results back to the model |
| 03 | [ReAct Loop](01-basic/03-react-loop/) | Reason + Act cycle, observation, stopping conditions |

### 02 — Intermediate
| # | Project | Concepts |
|---|---------|----------|
| 04 | [Memory & State](02-intermediate/04-memory-and-state/) | Short-term chat history, summarization, long-term memory store |
| 05 | [Planning & Task Decomposition](02-intermediate/05-planning-and-task-decomposition/) | Plan → execute → replan, task graphs, progress tracking |
| 06 | [Structured Output & Guardrails](02-intermediate/06-structured-output-and-guardrails/) | Schema validation, retries, input/output checks, safe tool use |

### 03 — Advanced
| # | Project | Concepts |
|---|---------|----------|
| 07 | [RAG Agent](03-advanced/07-rag-agent/) | Retrieve → ground → answer, citations, when not to answer |
| 08 | [Reflection & Self-Critique](03-advanced/08-reflection-and-self-critique/) | Draft → critique → revise, verifier pass, bounded iteration |
| 09 | [Reliability Patterns](03-advanced/09-reliability-patterns/) | Timeouts, retries/backoff, fallbacks, idempotency, eval hooks |

### 04 — Multi-Agent Communication
| # | Project | Concepts |
|---|---------|----------|
| 10 | [Sequential Handoff](04-multi-agent/10-sequential-handoff/) | Pipeline of specialists, explicit handoff contracts |
| 11 | [Router / Supervisor](04-multi-agent/11-router-supervisor/) | Intent routing, delegation, result aggregation |
| 12 | [Debate & Consensus](04-multi-agent/12-debate-and-consensus/) | Proposer/critic roles, convergence, arbiter decisions |
| 13 | [Shared Blackboard](04-multi-agent/13-shared-blackboard/) | Shared state, concurrent contributions, conflict handling |

## Real provider, streaming, and tracing

- [shared/](shared/) — provider-agnostic `LLMClient` adapter (`MockLLMClient` default; `OpenAICompatibleClient` via `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_MODEL`), a streaming hook (`stream()` deltas), and lightweight trace spans (`shared/tracing.py`). Swap for official SDKs + OpenTelemetry/LangSmith in production.

## Automated evals

- [evals/](evals/) — minimal harness + JSON report. Suites include **golden** tasks, **tool-failure** cases (retries/idempotency), and **adversarial/safety** cases (unsafe action blocked, injection-shaped query does not produce a grounded hallucination). Run: `python evals/run_evals.py`.

## Framework mappings (with real-time usage cases + HITL)

Each framework folder has a `README.md` (description, pattern mapping, **real-time usage cases**, **human-in-the-loop** approval guidance) and a `main.py` that runs a **mock demo** without framework deps/keys and shows the real structure.

| Framework | Focus | Example |
|---|---|---|
| [LangChain](05-frameworks/langchain/) | Composable LLM building blocks (LCEL/runnables, tools, retrievers, agents) | LCEL-style flow + tool call with HITL gate |
| [LangGraph](05-frameworks/langgraph/) | Stateful graph orchestration, cycles/branches, checkpoints/interrupts | Routing + HITL approval gate |
| [AutoGen](05-frameworks/autogen/) | Conversable multi-agent messaging, group chat/routing patterns | Routed specialists + human proxy gate |
| [CrewAI](05-frameworks/crewai/) | Role/task/crew processes (sequential/hierarchical) | Research → plan crew + HITL hold |
| [Semantic Kernel](05-frameworks/semantic-kernel/) | Kernel + plugins/functions, filters/middleware, enterprise governance | Plugin call with approval filter |

Not a framework, but commonly confused for one: **[Jev (TypeSafe AI)](05-frameworks/jev/)** is a *System One decision model* — you send `state` + typed questions (`choice` / `score` / `noul`) and get typed answers with probabilities, no generated text. It slots **inside** an agent loop for routing, tool gating and done-checks. The example covers the wire format, threshold policy, a fail-closed refund gate, a cost cascade (`cascade.py`: verify a cheap draft, escalate only when unconvinced), a `--probe` mode that prints the real response shape, and ten eval cases proving the gate never approves on a broken check.

## Design patterns (agentic AI & agent communication)

- **[06-design-patterns/README.md](06-design-patterns/README.md)** — comprehensive catalog of major patterns: single-agent reasoning/control, tool & knowledge, reliability/safety/HITL, multi-agent topologies, and communication protocols (handoff contracts, request–response, pub/sub events, envelopes/correlation, provenance, bidding/mediator, etc.), with tradeoffs and demos.

New runnable pattern examples:
| # | Pattern | Folder |
|---|---------|--------|
| 01 | Hierarchical delegation | [06-design-patterns/01-hierarchical-delegation/](06-design-patterns/01-hierarchical-delegation/) |
| 02 | Parallel fan-out / aggregator | [06-design-patterns/02-parallel-fanout-aggregator/](06-design-patterns/02-parallel-fanout-aggregator/) |
| 03 | Event-driven pub/sub | [06-design-patterns/03-event-driven-pubsub/](06-design-patterns/03-event-driven-pubsub/) |
| 04 | Evaluator–optimizer loop | [06-design-patterns/04-evaluator-optimizer-loop/](06-design-patterns/04-evaluator-optimizer-loop/) |
| 05 | Handoff protocol (envelope/ACK/correlation) | [06-design-patterns/05-handoff-protocol/](06-design-patterns/05-handoff-protocol/) |
| 06 | Human approval gate (HITL) | [06-design-patterns/06-human-approval-gate/](06-design-patterns/06-human-approval-gate/) |
| 07 | Contract net / bidding (task auction) | [06-design-patterns/07-contract-net-bidding/](06-design-patterns/07-contract-net-bidding/) |
| 08 | Mediator (central routing + policy: authz, redaction, rate limits) | [06-design-patterns/08-mediator/](06-design-patterns/08-mediator/) |

## Cross-cutting guides

- [Best Practices](docs/best-practices.md) — design principles, safety, evaluation, and production checklist.

## Scope & next steps

This repo focuses on **core patterns and control flow** you can read and run locally, plus **framework-mapped examples** (mock-runnable; install deps and configure a provider for live runs). Natural next steps:

- Wire the `shared/` adapter into more pattern examples and add full SSE streaming parsing / official SDK clients.
- Export traces to OpenTelemetry and attach prompt/schema version attributes; add cost/latency dashboards.
- Expand evals with LLM-as-judge rubrics, RAG citation-accuracy checks, CI regression baselines, and cases for every production failure fixed.
- Deepen framework examples with real tool connectors, checkpointed HITL interrupts, and end-to-end tests.
- Add authenticated, auditable human approval workflows for high-impact actions (payments, deletes, external sends, production changes).
