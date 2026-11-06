# LangChain (framework mapping)

## Description

**LangChain** is a framework for building applications powered by language models. It provides composable building blocks for connecting models to prompts, tools, data sources, memory, and output parsers. Its modern style emphasizes **LCEL (LangChain Expression Language)** for composing runnable steps (`prompt | model | parser`) and higher-level abstractions for **agents**, **retrievers (RAG)**, and **tool calling**.

> **LangChain vs. LangGraph:** LangChain provides core components and composition (chains/runnables, tools, retrievers, basic agent interfaces). **LangGraph** (also in the LangChain ecosystem) is the lower-level/stateful graph orchestration layer for controllable multi-step/looping/branching agent workflows. Use LangChain components inside workflows; use LangGraph when you need explicit state, cycles, checkpoints, or HITL interrupts.

## Core concepts (deep details)

| Concept | What it is | Maps to this repo |
|---|---|---|
| **Model I/O** | Chat/model wrappers with a common interface; messages, params, streaming | `shared/llm_adapter.py` provider abstraction |
| **Prompts** | Templates/messages that structure instructions and variables | System/task/user message design (01, 06) |
| **Output parsers / structured output** | Parse/validate model text into typed/structured data | Structured output & guardrails (06) |
| **Tools** | Functions/APIs the model can call; schemas + execution | Tool calling (02), ReAct (03) |
| **Agents** | Model-driven loops that choose tools/actions until done | ReAct/agent loops (03, 05) |
| **Retrievers & vector stores** | Fetch relevant documents/chunks for grounding (RAG) | RAG agent (07) |
| **Memory** | Persist/recall conversation or state (use carefully; verify volatile facts) | Memory & state (04) |
| **LCEL / Runnables** | Composable pipeline (`a \| b \| c`) with streaming/batching/async hooks | Deterministic orchestration + tracing (09, shared) |
| **Callbacks/tracing** | Hooks for observability; integrates with LangSmith/OpenTelemetry patterns | `shared/tracing.py` / evals (09, evals) |

## Pattern mapping (this repo → LangChain)

| Repo pattern | LangChain approach |
|---|---|
| Hello agent (01) | Prompt template + chat model runnable |
| Tool calling / ReAct (02, 03) | `bind_tools` / tool-calling model + agent executor or custom loop |
| Memory & state (04) | Chat history/message stores; explicit state preferred for critical workflows |
| Planning (05) | Structured plan output + runnable steps; LangGraph for robust loops/replanning |
| Guardrails (06) | Schema-constrained output, parsers/validators, tool arg checks, retry logic |
| RAG (07) | Loader/splitter → embeddings/vector store → retriever → grounded prompt/chain |
| Reliability/evals (09, evals) | Callbacks/tracing, retries/fallbacks, LangSmith-style eval datasets (conceptually) |
| Multi-agent (10–13) | Possible via custom orchestration; LangGraph is usually clearer for complex control flow |

## Real-time usage cases

- **RAG knowledge assistant:** retrieve current policy/docs, ground answers with citations, abstain when evidence is missing (support, IT, HR, compliance).
- **Tool-using operations copilot:** look up orders/tickets/metrics via tools, draft actions, and require approval before writes (refunds, account changes, notifications).
- **Document processing pipeline:** ingest files → split/embed → extract structured fields → validate → human review for regulated outputs.
- **Research/summarization workflow:** fetch sources, synthesize findings, produce structured briefs with evidence links; schedule/batch via runnable interfaces.
- **Developer support agent:** search code/docs/runbooks (retriever), propose fixes, and gate code execution or deployment actions behind HITL.

## Human-in-the-loop (HITL) for high-impact actions

- Treat model/tool outputs as **proposals** until validated. Put high-impact operations (payments, deletes, external sends, production changes) behind an explicit approval step.
- In simple chains, pause before the execution function and require an authenticated approval payload. For complex/stateful HITL (interrupt/resume/checkpoints), prefer **LangGraph**.
- Use **least-privilege tools**, argument validation, and idempotency keys. Log tool calls, approvals, and outcomes via callbacks/tracing.
- Defend against prompt injection from retrieved/user content: label evidence as data, constrain tool choice, and validate outputs.

## Run

```bash
pip install -r 05-frameworks/langchain/requirements.txt
python 05-frameworks/langchain/main.py
```

`main.py` runs a **mock, dependency-light demo** showing a LangChain-style LCEL/tool-calling flow and a HITL gate. It prints the real LangChain code path as guidance. Install deps and configure a provider (e.g., `OPENAI_API_KEY` with `langchain-openai`, per current docs) to run the live path.

## Notes

LangChain APIs change frequently (package splits like `langchain-core`, `langchain-community`, provider packages, agent APIs). Use this example for **concepts and mapping**; verify current class/function names and HITL/agent recommendations in the official LangChain docs.
