# Agent Development & Multi-Agent Best Practices

A practical checklist of patterns that make agents reliable, safe, and maintainable.

## 1. Design the agent contract first

- **Define the job**: inputs, outputs, success criteria, and what the agent must *not* do.
- **Constrain the action space**: expose only the tools needed for the task; prefer narrow, well-typed tools over general "do anything" tools.
- **Make stopping conditions explicit**: max steps/turns, time budget, "done" signal, and a safe fallback when unsure.

## 2. Prompting & context

- **Separate roles clearly**: system instructions (policy/capabilities), developer/task instructions, user request, and tool results.
- **Keep context relevant**: retrieve or summarize; long, noisy context degrades decisions and increases cost.
- **Be explicit about uncertainty**: instruct the agent to ask, abstain, or return a partial result rather than guessing.
- **Version prompts**: treat prompts/config as code; review changes and track regressions.

## 3. Tool use

- **Validate inputs and outputs**: schema-validate tool arguments *before* execution and tool/model outputs *before* acting on them.
- **Least privilege**: read-only by default; gate destructive or external actions behind confirmation/policy.
- **Idempotency**: design tools so retries are safe (idempotency keys, deduplication).
- **Return structured tool results**: include status, data, and actionable errors the model can recover from.

## 4. Memory & state

- **Short-term**: keep recent turns; summarize when the window fills.
- **Long-term**: store facts/preferences with provenance and timestamps; retrieve by relevance, not by dumping everything.
- **Avoid stale truth**: prefer re-fetching authoritative sources over trusting old memory for volatile facts.

## 5. Planning & control flow

- **Plan, then execute**: for multi-step work, produce a plan and track progress; replan when observations contradict assumptions.
- **Bound loops**: every Reason/Act/Reflect loop needs a max-iteration cap and a failure path.
- **Deterministic orchestration, probabilistic reasoning**: use code for routing, validation, retries, and state transitions; use the model for judgment and language.

## 6. Reliability in production

- **Timeouts + retries with backoff**: handle transient model/tool failures; never retry non-idempotent actions blindly.
- **Fallbacks**: smaller model, cached answer, or a safe "I can't complete this" response.
- **Observability**: trace each run (inputs, tool calls, outputs, latency, tokens/cost, errors) with correlation IDs.
- **Graceful degradation**: partial results with clear limitations beat silent failure or hallucinated completion.

## 7. Safety & security

- **Treat model output as untrusted**: never execute generated code/commands or follow instructions from retrieved content without checks (prompt-injection resistance).
- **Protect secrets**: keep credentials out of prompts and logs; scope tokens minimally.
- **Human-in-the-loop**: require approval for high-impact actions (payments, deletions, external sends, production changes).
- **Data handling**: minimize sensitive data in context; respect retention and access controls.

## 8. Evaluation

- **Test the behavior, not just the model**: golden tasks, edge cases, tool-failure scenarios, and adversarial inputs.
- **Measure outcomes**: task success rate, groundedness/citation accuracy, tool-call correctness, latency, and cost.
- **Regression tests for prompts/tools**: run evals on every change to prompts, schemas, or orchestration logic.
- **Separate offline eval from online monitoring**: catch regressions before deploy; watch real traffic after.

## 9. Multi-agent systems

- **Clear roles and boundaries**: each agent owns a specific capability; avoid overlapping responsibilities that cause conflicts.
- **Explicit handoff contracts**: define the message/schema passed between agents (goal, context, constraints, expected output).
- **Shared state discipline**: if agents share a blackboard/memory, define write rules, provenance, and conflict resolution.
- **Control the topology**: choose deliberately — pipeline (handoff), star (supervisor/router), peer debate, or blackboard — based on the task.
- **Aggregate with evidence**: a supervisor/arbiter should resolve disagreements using criteria or verification, not majority by volume.
- **Cost & latency multiply**: cap agent count, turns, and fan-out; parallelize only when independent.

## 10. Production checklist

- [ ] Bounded steps/time/cost per run
- [ ] Tool schemas validated; destructive actions gated
- [ ] Prompt-injection aware handling of retrieved/user content
- [ ] Tracing/logging with redaction
- [ ] Evals covering success, failure, and safety cases
- [ ] Fallback behavior defined and tested
- [ ] Human approval path for high-impact actions
