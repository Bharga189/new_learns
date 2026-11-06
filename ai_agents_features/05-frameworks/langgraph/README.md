# LangGraph (framework mapping)

## Description

**LangGraph** is a graph-based orchestration framework (from the LangChain ecosystem) for building stateful, controllable agent workflows. You model an agent/system as a **graph**: nodes are steps (LLM calls, tool execution, routing, human checks), edges define transitions, and **state** flows through the graph. This makes loops (ReAct/reflection), branching, checkpoints, and interrupts explicit—mapping closely to the control-flow patterns in this repo.

## Pattern mapping (this repo → LangGraph)

| Repo pattern | LangGraph concept |
|---|---|
| ReAct loop (03) | Cyclic graph: `agent` node ↔ `tools` node, conditional edge until done |
| Planning/task decomposition (05) | State holds plan/tasks; nodes execute steps and route to replan/finish |
| Structured output/guardrails (06) | Validation node; conditional edge to retry/fail-safe/human review |
| Router/supervisor (11) | Supervisor node returns next worker; conditional edges to specialist nodes |
| Human-in-the-loop approval | `interrupt`/checkpoint before high-impact tool node; resume with approval |

## Real-time usage cases

- **Customer support triage (live):** classify intent → route to billing/tech/policy agents → draft response → require human approval before issuing refunds or account changes.
- **Incident response copilot:** ingest alert → gather diagnostics via tools → propose remediation plan → pause for on-call engineer approval before running any production action.
- **Sales/CRM assistant:** retrieve account context (RAG) → generate email/update steps → human approves outbound message or CRM mutation.
- **Code change assistant:** plan edits → run tests/tools → reflect on failures → open PR draft; require approval before merge/deploy actions.
- **Long-running workflows:** checkpointed state lets a workflow pause for minutes/hours (human input, external event) and resume safely.

## Human-in-the-loop (HITL) for high-impact actions

- Model high-impact operations (payments, deletions, external sends, production changes) as **separate nodes** behind a gate.
- Before that node, add an **interrupt/checkpoint** with a clear summary: proposed action, arguments, risk, and expected effect.
- Resume only with an explicit approval payload (who approved, when, scope). Reject/modify routes back to planning.
- Log approvals in traces; keep tool execution **idempotent** in case of retries/resumes.

## Run

```bash
pip install -r 05-frameworks/langgraph/requirements.txt
python 05-frameworks/langgraph/main.py
```

`main.py` runs a **mock demo** when LangGraph (or a model key) isn’t available, and shows the real graph structure. Set `OPENAI_API_KEY` and install deps to run with a real model (adapt the model client as needed).

## Notes

LangGraph APIs evolve; check the current LangGraph docs for exact class/function names and HITL/interrupt APIs.
