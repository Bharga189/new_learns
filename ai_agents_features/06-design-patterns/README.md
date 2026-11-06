# Agentic AI & Agent Communication Design Patterns

A catalog of major **agentic AI design patterns** and **agent communication patterns**.
Each entry here explains **what the pattern is**, **when to use it**, **tradeoffs**, and
**where it is demonstrated** in this repo. This is a reference + decision aid, not a
tutorial: the `main.py` for each pattern contains the runnable code and the comments trace
the control flow.

---

## How to read this catalog

Pick a pattern, then read the columns in order:

1. **What it is** — the reusable structure in one sentence.
2. **When to use** — the problem shape it fits.
3. **How it works** — the step-by-step flow (in the pattern's `main.py`).
4. **Tradeoffs** — cost, latency, complexity, failure modes.
5. **Production rules** — the rules that make it safe and debuggable.
6. **Anti-patterns** — the ways this pattern silently fails.
7. **Related patterns** — the patterns you will reach for next.
8. **Demo** — the runnable example that shows the shape.

Use the **pattern selection guide** at the end when you are deciding between families.

---

## 1. Single-agent reasoning & control patterns

| Pattern | Description | Use when | Demo |
|---|---|---|---|
| **Basic agent loop** | Send messages (system + history + user) to a model, append response; repeat. The app owns state. | Simple assistants, first prototype. | [01 Hello Agent](../01-basic/01-hello-agent/) |
| **ReAct (Reason + Act)** | Interleave reasoning with tool actions and observations until a final answer. | Tasks needing external info/actions step by step. | [03 ReAct Loop](../01-basic/03-react-loop/) |
| **Plan-and-Execute** | Produce a plan, execute steps, track status, replan when observations diverge. | Multi-step goals with dependencies. | [05 Planning & Task Decomposition](02-intermediate/05-planning-and-task-decomposition/) |
| **Reflection / Self-Refine** | Draft → critique against criteria → revise, bounded. | Quality-sensitive outputs (writing, reasoning, code review text). | [08 Reflection & Self-Critique](03-advanced/08-reflection-and-self-critique/) |
| **Evaluator–Optimizer** | Separate generator and evaluator roles; iterate until criteria pass or budget ends. | When criteria are explicit (correctness, format, safety, rubric). | [04 Evaluator–Optimizer Loop](04-evaluator-optimizer-loop/) |
| **Memory & State** | Keep what the model needs across turns: short-term history, summarization, long-term store, explicit state. | Anything that spans more than one turn — continuity, context budget, learned facts. | [04 Memory & State](02-intermediate/04-memory-and-state/) |
| **Structured output & guardrails** | Validate JSON/schema; retry with errors; gate actions by policy. | Machine-consumed outputs, safe tool routing. | [06 Structured Output & Guardrails](02-intermediate/06-structured-output-and-guardrails/) |

### Deep dive

#### Evaluator–Optimizer
**What it is:** two distinct roles — a `generator` that produces a candidate, and an
`evaluator` that scores the candidate against explicit criteria and returns actionable
feedback. The generator revises with that feedback. This repeats until the criteria pass or
the iteration budget is exhausted.

**When to use:** you have measurable acceptance criteria (format, correctness checks, policy,
rubric) and quality matters more than single-pass latency.

**How it works** (see `04-evaluator-optimizer-loop/main.py`):
1. Run the generator once with no feedback → it produces a weak first draft (`"Deploy now."`).
2. Run the evaluator → it checks 3 criteria (`mentions tests`, `mentions approval`, `mentions rollback`) and returns feedback + a score.
3. Feed that feedback back into the generator; the generator now emits a structured plan.
4. Re-evaluate; when `passed == True`, return the candidate. Cap rounds so a bad generator cannot spin forever.

**Tradeoffs:**
- **Cost/latency** scales with rounds; each round is a model call (or an evaluation pass).
- **Evaluator quality is the binding constraint** — a weak evaluator can't fix a weak generator.
- **Bounding matters** — `max_rounds` is mandatory; otherwise a failing candidate loops forever.

**Production rules:**
- Criteria must be **deterministic and testable**. Prefer code/test/rules for the evaluator over a second LLM judge.
- **Never feed vague feedback** like `\"be better\"`; make it actionable per criterion.
- **Cap rounds** and report where it stopped.
- **Measure cost per round** so you can switch to a cheaper evaluator mid-run.

**Anti-patterns:**
- Vague criteria; the evaluator accepts anything.
- No round cap — an uncompliant generator loops forever.
- Evaluator trained on the same bias as the generator.

**Related patterns:** Reflection/Self-Refine (same shape, prose-focused), Evaluation harness (how you test the evaluator itself), `09 Reliability Patterns` (binding the loop with timeouts).

#### Basic agent loop / ReAct / Plan-and-Execute
**What it is:** the smallest solvable unit of agentic control. A loop that takes a prompt,
gets a response, optionally acts, and repeats until a stopping condition.

**When to use:** you are building the "hello world" of agents, single-step tool use, or
multi-step work with a clear plan.

**How it works:** (1) assemble system + history + user message; (2) call the model; (3) append
the response; (4) if the response contains a tool action, execute it and append the result;
(5) repeat until a `done`/max-steps condition. ReAct interleaves "think, act, observe"
explicitly; Plan-and-Execute materializes a plan and tracks per-step status, replanning when
observations diverge.

**Tradeoffs:** basic loops grow context cheaply and give you no grounding; ReAct adds an
action loop that can hallucinate actions; Plan-and-Execute adds planning overhead and stale
plans if not re-planned.

**Production rules:** explicit stopping conditions, bounded steps, structured tool results
the model can recover from, and deterministic orchestration for routing/state transitions.

---

#### Memory & State
**What it is:** the mechanisms that let a single agent **remember** across turns — what fits
in the current context window, what is compressed away, what is stored durably and retrieved
by relevance.

**When to use:** the task outlives one round trip — multi-turn workflows, learned user
preferences, persistent goals, or any conversation longer than the context window allows.

**How it works** (see `02-intermediate/04-memory-and-state/main.py`):
1. New message arrives → appended to **short-term history** (the active conversation window).
2. If history exceeds the window size → run a **summarization** step that compresses old
   turns into a summary and keeps only the most recent turns (so nothing is lost, only
   compressed).
3. Periodically (or on query) **retrieve** relevant long-term facts by relevance and inject
   them into context — never dump the whole store.
4. After the model responds, optionally write durable facts to the **long-term store**
   (with provenance/timestamps for later auditing).
5. Agent **state** (goal, plan, constraints) is a separate explicit variable the model reads
   and updates, distinct from the conversation history.

**Tradeoffs:** short-term history is fast but fills up; summarization loses detail;
long-term retrieval adds latency and must be relevant (dumped context degrades quality);
writing memory is itself a policy decision about what is safe/useful to remember.

**Production rules:**
- Keep recent/raw messages for the current task; summaries are lossy, not a replacement.
- Store long-term facts with **provenance + timestamps**; re-check volatile facts before
  acting on them.
- **Retrieve by relevance**, never dump the whole store into every prompt.
- Treat memory writes as policy decisions — what may be remembered, and for how long.

**Anti-patterns:**
- Using summaries as the only memory (lossy detail is gone).
- Dumping the whole memory store into the prompt (context pollution, privacy risk).
- No timestamps/provenance on stored facts (you can't re-check or audit).

**Related patterns:** Reflection/Self-Refine (reuse of past critique), RAG (external
long-term evidence), Handoff protocol (state carried between agents).

---

#### Structured output & guardrails
**What it is:** the pattern that makes model output **usable and safe** — request structured
(JSON/schema) output, validate it, retry with structured errors when it is invalid, and
gate every proposed action through an allowlist before it executes.

**When to use:** anything the model produces must be consumed programmatically, or any
proposed action must be approved before it has effect. It is the companion of tool calling
and the foundation of HITL gates.

**How it works** (see `02-intermediate/06-structured-output-and-guardrails/main.py`):
1. **Request structured output** — tell the model the exact JSON schema/format to return.
2. **Parse and validate** the result against the schema.
3. If invalid → **retry with the validation error included** in the prompt (bounded rounds
   so a broken model cannot loop forever).
4. **Input guardrail** — check the request for missing/unsafe content before spending the
   model call.
5. **Output guardrail** — after the model responds, re-check for policy violations.
6. **Tool/action allowlist** — only approve actions whose parameters match a known-safe
   white-list.

**Tradeoffs:** schema/request adds prompt overhead and can constrain the model's freedom;
too-strict validation pushes legitimate requests into retries; the cost is an extra layer of
code between the model and the side effect.

**Production rules:**
- Treat model output as **untrusted until validated**.
- **Bound retries**; always include the validation error in the retry prompt.
- Use **allowlists** (not blocklists) for tools, actions, and sensitive parameters.
- **Separate "can't parse/validate" from "policy denied"** in logs and responses — they are
  different failure modes and need different handling.

**Anti-patterns:**
- Executing unvalidated model output.
- Blocklists instead of allowlists (hard to enumerate every "bad" thing, easy to miss one).
- Infinite retries on a schema mismatch (the model can't fix a schema it was never told).

**Related patterns:** Tool/function calling, Human-in-the-loop approval gate, Evaluator–Optimizer (the evaluator is the guardrail for generated output).

---


## 2. Tool use & knowledge patterns

| Pattern | Description | Use when | Demo |
|---|---|---|---|
| **Tool/function calling** | Model emits structured tool requests; app validates, executes, returns results. | Any external action/data access. | [02 Tool Calling](../01-basic/02-tool-calling/), [shared adapter](../shared/) |
| **Code-as-action** | Agent writes/runs code in a sandbox to solve tasks. | Data analysis, transformations, automation. | Guidance in `docs/best-practices.md`, [AutoGen README](../05-frameworks/autogen/README.md) |
| **RAG (Retrieval-Augmented Generation)** | Retrieve relevant evidence, ground answer in it, cite; abstain when missing. | Factual/current/project-specific Q&A. | [07 RAG Agent](03-advanced/07-rag-agent/) |
| **Memory tiers** | Short-term window + summarization + long-term store with provenance/timestamps. | Personalization, continuity across turns/sessions. | [04 Memory & State](02-intermediate/04-memory-and-state/) |
| **Structured output + constrained decoding** | Validate JSON/schema; retry with errors; gate actions by policy. | Machine-consumed outputs, safe tool routing. | [06 Structured Output & Guardrails](02-intermediate/06-structured-output-and-guardrails/) |
| **Cache/memoization** | Reuse results for identical/similar safe requests (with invalidation). | Repeated lookups, cost/latency reduction. | Reliability concept; extend [09](../03-advanced/09-reliability-patterns/). |

### Deep dive

#### Tool/function calling
**What it is:** the model emits structured tool requests (JSON schemas); your code
validates, executes, and returns results back to the model. This is the bridge between
generative output and deterministic effect.

**When to use:** any time the agent must read/write external state, call an API, or perform
an action.

**How it works:** (1) register tools with schemas; (2) ask the model to pick/invoke; (3)
validate the tool args against the schema **before** calling; (4) execute with a timeout and
bounded retries; (5) return structured results (status, data, actionable errors) back to the
model; (6) the model reasons over those results.

**Tradeoffs:** schema/validation burden; security risk if a tool is over-privileged; the
model's tool choice is probabilistic, so it can invoke the wrong tool or malformed args.

**Production rules:** least privilege (read-only by default), idempotent tools with
idempotency keys, structured error formats the model can recover from, deterministic
routing logic for tool dispatch, and **validate tool results before acting on them**.

**Anti-patterns:** executing unvalidated tool args; tools that have side effects with no
idempotency; overloading one generic tool with many responsibilities.

#### RAG (Retrieval-Augmented Generation)
**What it is:** retrieve relevant evidence, ground the answer in it, cite sources, and
**abstain** when the evidence is missing.

**When to use:** factual, current, or project-specific Q&A where hallucination is
unacceptable.

**Tradeoffs:** retrieval quality dominates correctness; prompt injection is possible via
retrieved content.

**Production rules:** label evidence as data (never as instructions), constrain tool choice,
validate retrieved content, and require the model to say "I don't have enough information"
when no source supports an answer.

#### Cache/memoization
**What it is:** reuse a previously computed result for an identical (or safely similar)
request, with an invalidation policy so stale responses do not get served.

**When to use:** repeated, safe, read-mostly lookups where latency and cost matter and the
result will not change during the cache TTL.

**Tradeoffs:** cache freshness vs. accuracy; invalidation is the hard part; a too-short TTL
kills the benefit, a too-long TTL means stale data.

**Production rules:** version cache keys with the input/schema version; invalidate on write;
store a cache-bust flag for manual override; log cache hits vs. misses to measure benefit.

**Anti-patterns:** serving stale results without a TTL/version; no invalidation on write; not
measuring hit-rate.

**Related patterns:** Reliability patterns (retry back-off on cache miss), Evaluator–Optimizer
(cache scores/verdicts).

---

## 3. Reliability, safety & oversight patterns

| Pattern | Description | Use when | Demo |
|---|---|---|---|
| **Guardrails / policy checks** | Validate inputs, outputs, and proposed actions; allowlists and block rules. | Any production agent, especially with tools. | [06 Guardrails](02-intermediate/06-structured-output-and-guardrails/) |
| **Retry/backoff + fallback** | Handle transient failures; degrade safely (fallback model/cache/partial answer). | External model/tool dependencies. | [09 Reliability Patterns](03-advanced/09-reliability-patterns/) |
| **Idempotency** | Design side-effecting operations so retries are safe (keys/dedup). | Payments, tickets, writes, workflows. | [09 Reliability Patterns](03-advanced/09-reliability-patterns/) |
| **Human-in-the-loop (HITL) approval gate** | Pause before high-impact actions; resume only with explicit approval. | Refunds, deletes, external sends, prod changes, regulated steps. | [06 Human Approval Gate](06-human-approval-gate/), [Jev gate](../05-frameworks/jev/) |
| **Sandboxed execution** | Run untrusted/model-generated actions in isolated environments with limits. | Code execution, browsing, file ops. | Best-practice guidance. |
| **Observability/tracing** | Record spans, tool calls, latency/cost, errors with redaction/correlation IDs. | Debugging, evals, production ops. | [shared/tracing.py](../shared/tracing.py), [evals](../evals/) |
| **Evaluation harness / regression tests** | Golden tasks, tool-failure/adversarial cases, baseline comparisons. | Any prompt/schema/orchestration change. | [evals/](../evals/) |

### Deep dive

#### Human-in-the-loop (HITL) approval gate
**What it is:** pause before a **high-impact action**, present a structured proposal, and
resume only with an explicit, auditable approval.

**When to use:** payments, refunds, deletes, external sends, access/permission changes,
production deployments/changes, regulated workflows.

**How it works** (see `06-human-approval-gate/main.py`):
1. Define a high-impact action set (`refund`, `delete`, `send_external`, `deploy_prod`,
   `change_access`).
2. Build an `ActionProposal` with known fields: action, target, args, reason, risk,
   reversibility.
3. `requires_approval()` returns True only for high-impact actions (low-risk actions skip
   the pause).
4. Present a structured summary to the approver (here a `decide` callback).
5. `approve` → execute; `reject` → hold with a reason; `review` → escalate for human
   judgment.
6. Audit log records proposal + decision + execution result (not shown in code;
   documented).

**Tradeoffs:** adds latency and requires an approval UX/channel. It must handle timeouts,
rejection, modification, and audit logging.

**Production rules:**
- **Fail closed** on timeout or ambiguous decision — unknown = held, never auto-approve.
- Use **idempotency keys** on every executed action.
- Record proposal + decision + result; never log secrets.
- Keep the approval UX/channel separate from the action execution path.

**Anti-patterns:** asking for approval on low-risk actions (reduces trust/velocity);
auto-approving on timeout; no audit trail.

**Related patterns:** `Jev` threshold gate ([05-frameworks/jev/README.md]), `Mediator`
policy enforcement ([08-mediator/]), `Guardrails` ([06-structured-output-and-guardrails/]).

---

#### Retry/backoff + fallback
**What it is:** a pattern that makes the agent **survive** transient failures — retries with
exponential backoff, jitter, a max budget, and a safe fallback (different model, cached
result, partial answer) when all retries are exhausted.

**When to use:** any external dependency — the LLM provider, a tool, a database, a queue —
can be slow, flaky, or rate-limited. Unguarded calls turn a 1% failure rate into a broken
agent.

**How it works:**
1. On failure, wait `base * backoff^attempt` seconds (with jitter so concurrent retries do
   not thundering-herd).
2. Retry up to a **hard budget** (attempts or wall-clock time).
3. If the budget is exhausted, fall back to a **degraded path**: a cheaper/faster model,
   a cached/stale answer, a partial result, or a human handoff.
4. Log every attempt with the error, attempt count, and direction taken, so the failure is
   diagnosable.

**Tradeoffs:** retries add latency and cost on top of the original failure; backoff must be
bounded or the agent looks "hung"; a fallback must be chosen by a policy, not left to chance.

**Production rules:**
- **Bound both attempts and wall-clock time** — latency is a constraint, not a hint.
- Add **jitter** to backoff so retries from many agents do not synchronize.
- **Fail over to a deliberately safe fallback**; never guess a repair.
- Log every attempt (error, attempt, direction) for diagnosis.

**Anti-patterns:** unbounded retries (infinite loops on a down dependency); no jitter;
fallback that is *more* dangerous than the failure; silence after budget exhaustion.

**Related patterns:** Evaluator–Optimizer (bounded optimization), Human approval gate
(fallback on refusal), Tracing (observe retry behavior).

#### Idempotency
**What it is:** designing **side-effecting operations so that retries are safe** — the second
call produces the same result as the first, not a double charge, double send, or duplicate
write.

**When to use:** anything that persists state — payments, ticket creation, email sends,
API writes, workflow steps, auction/bidding.

**How it works:**
1. The caller passes an **idempotency key** with the operation.
2. The server stores the key with the result; on a duplicate key it returns the stored
   result instead of re-executing.
3. If no key is present, the system must still be designed so a retry is safe, or short-
   lived queue/exactly-once semantics are used.

**Tradeoffs:** adds a read/store step before the write; the key must be unique and long-
lived enough; lossy transports (UDP, fire-and-forget) still need a delivery guarantee even
with idempotency.

**Production rules:**
- Generate a unique idempotency key per logical operation (not per call).
- Store the key **with the result** before returning a 2xx.
- Treat key collision as a policy conflict (both calls must agree on what "same" means),
   not as a bug.

**Anti-patterns:** no idempotency key on a write; assuming retries are safe; key replayed
across different logical operations.

**Related patterns:** Human-in-the-loop gate (audit trail), Transfer/Funds (settle on key),
Contract net bidding (awarding).

---

## 4. Multi-agent topology patterns

| Pattern | Description | Use when | Demo |
|---|---|---|---|
| **Sequential handoff / pipeline** | Specialists run in order; each passes structured output to the next. | Clear stages (research → plan → write). | [10 Sequential Handoff](10-sequential-handoff/) |
| **Router / supervisor (star)** | Coordinator classifies/delegates to workers and aggregates. | Mixed intents, specialist skills. | [11 Router / Supervisor](11-router-supervisor/) |
| **Hierarchical delegation** | Manager delegates to sub-managers/workers; escalates on ambiguity/failure. | Large tasks needing decomposition across teams/roles. | [01 Hierarchical Delegation](01-hierarchical-delegation/) |
| **Parallel fan-out / aggregator (map-reduce)** | Dispatch independent subtasks in parallel; aggregate/verify. | Independent lookups/reviews, ensembling. | [02 Parallel Fan-out/Aggregator](02-parallel-fanout-aggregator/) |
| **Debate / consensus** | Proposer and critic(s) argue; arbiter decides by criteria/evidence. | Ambiguous or high-stakes decisions. | [12 Debate & Consensus](12-debate-and-consensus/) |
| **Shared blackboard** | Agents read/write shared state opportunistically under rules. | Opportunistic collaboration, partial contributions. | [13 Shared Blackboard](13-shared-blackboard/) |
| **Swarm / peer collaboration** | Many peers contribute; coordination emerges via rules or a light coordinator. | Brainstorming, broad exploration. | Conceptual; constrain with budgets + aggregator. |
| **Mixture-of-agents / ensemble** | Multiple agents produce candidates; selector/judge picks or synthesizes. | Improve quality via diversity. | Variant of evaluator/aggregator patterns. |

### Deep dive

#### Sequential handoff / pipeline
**What it is:** each specialist agent **finishes its part and passes a structured result to
the next**. It is the simplest multi-agent topology — a pipeline where the output of one
stage is the input of the next.

**When to use:** clear, ordered stages (research → plan → write → review); each stage has a
well-defined contract and the next stage does not depend on alternatives.

**How it works:**
1. The pipeline starts with a first agent (e.g., researcher).
2. It produces a structured intermediate result (notes, citations, draft).
3. The second agent consumes that result and produces the next structured artifact (plan,
   draft, review).
4. Each stage **validates its input** before processing — a bad handoff poisons the whole
   pipeline.
5. After the final stage, the result is returned with provenance (who produced what).

**Tradeoffs:** easy to reason about and debug; but each stage is a serial bottleneck and a
failure in one stage blocks the rest. No parallelism.

**Production rules:**
- **Validate each stage's output before passing it on** — contracts, not prose.
- Record **provenance** (stage + timestamp + agent) so you can trace any result back.
- Bound the number of stages and the size of each intermediate artifact.

**Anti-patterns:**
- Passing raw text between stages (validation is impossible).
- No error handling when a stage fails (the whole pipeline dies).
- Stages that call each other instead of passing a contract (breaks the topology).

**Related patterns:** Parallel fan-out (for independent branches), Handoff protocol (the
contract), Router/supervisor (when stages differ in nature and should be routed, not
chained).

#### Router / supervisor (star)
**What it is:** a coordinator **classifies** incoming requests and delegates to a
specialist, then aggregates the specialist's output. One place that owns intent routing.

**When to use:** mixed intents where different workers have different skills (billing vs
technical vs fallback); the request shape is known in advance, but the specialist is not.

**How it works:**
1. The supervisor receives a request and classifies its intent (rule-based or model-based).
2. It dispatches to the matching specialist worker.
3. It collects the specialist's output and **aggregates** (confidence-weighted, first-
   valid-answer, or synthesized).
4. If no specialist matches or the specialist fails, it falls back to a default/passback.

**Tradeoffs:** simpler than hierarchical delegation; but the supervisor is a bottleneck and
a single point of failure. When the star grows too large, move to hierarchical or contract
net bidding.

**Production rules:**
- **Classify deterministically** where possible (rule-based) and reserve the model for
  ambiguous cases.
- Bound the supervisor's decision tree; add timeouts at dispatch.
- Aggregate specialist outputs with explicit conflict handling.

**Anti-patterns:**
- Routing every request through the model (defeats the purpose; cost/latency).
- No fallback when no specialist matches.
- Supervisors that also do the specialists' work (they become a single point of failure).

**Related patterns:** Hierarchical delegation (flatter alternative), Contract net (when
routing rules are unknown in advance — use bidding instead), Handoff protocol (contracts).

#### Shared blackboard
**What it is:** agents read/write a shared state (blackboard) opportunistically, with
**rules** controlling who can write which kind of entry and what happens on conflicts.

**When to use:** opportunistic collaboration where agents arrive at different times and each
contributes partial information; no single coordinator decides the order; the result emerges
from the contributions.

**How it works:**
1. A **blackboard** holds entries, each tagged with a type, source, and evidence.
2. Agents subscribe to entry types they can contribute to and pull entries they need.
3. A **merge/conflict rule** decides what happens when two agents write the same entry
   (last-write-wins, conflict-fenced, or prioritize by confidence).
4. New entries can trigger downstream agents (signals, not handoffs).

**Tradeoffs:** expressive and emergent, but conflict resolution can be complex; without
discipline it becomes write chaos. Requires a disciplined merge/conflict policy.

**Production rules:**
- Define who can write which entry type and under which rule.
- Track **provenance + evidence + confidence** per entry.
- Log all writes so you can replay/decide later; bound the board size.

**Anti-patterns:**
- No conflict rule → last-write-wins silently erases contributions.
- No provenance on entries → you can't audit why a value exists.
- Unbounded board → context pollution.

**Related patterns:** Event-driven pub/sub (when agents should observe changes, not poll
the board), Mediator (when you need a central policy point on writes).

---

## 5. Agent communication patterns & protocols

| Pattern | Description | Use when | Demo |
|---|---|---|---|
| **Explicit handoff contract** | Structured payload (goal, context, constraints, evidence, expected output). | Any agent-to-agent transfer. | [10 Handoff](../04-multi-agent/10-sequential-handoff/), [05 Handoff Protocol](05-handoff-protocol/) |
| **Request–response** | One agent sends a request and waits for a correlated response. | Tool-like delegation, RPC over messages. | Common inside [11](../04-multi-agent/11-router-supervisor/) |
| **Event-driven pub/sub** | Agents publish events to topics; subscribers react asynchronously. | Decoupled workflows, notifications, pipelines. | [03 Event-Driven Pub/Sub](03-event-driven-pubsub/) |
| **Message envelope / protocol** | Standard fields: `id`, `type`, `sender`, `receiver`, `correlation_id`, `payload`, `status`, `timestamp`. | Reliable inter-agent comms and tracing. | [05 Handoff Protocol](05-handoff-protocol/) |
| **Shared state with provenance** | Shared memory/blackboard entries include author, evidence, timestamp, confidence. | Collaboration with auditability. | [13 Shared Blackboard](../04-multi-agent/13-shared-blackboard/) |
| **Contract net / bidding (task auction)** | Manager announces task; workers bid by capability/cost; manager awards. | Dynamic capability routing/load balancing. | [07 Contract Net](07-contract-net-bidding/) |
| **Conversation/group chat with turn-taking** | Agents speak in a managed thread; a policy selects the next speaker. | Collaborative drafting/review. | [AutoGen README](../05-frameworks/autogen/README.md) |
| **Mediator** | Central component routes/transforms messages so peers don't couple directly. | Reduce N×N integration complexity. | [08 Mediator](08-mediator/) |

### Deep dive

#### Explicit handoff contract
**What it is:** a **structured payload** — goal, context, constraints, evidence, expected
output — that one agent hands to the next. The contract is explicit, versioned, and
validated, so a handoff cannot silently lose information.

**When to use:** any agent-to-agent transfer where information must be preserved across the
boundary — a pipeline, a handoff, a delegation, or a routing result.

**How it works:**
1. Define the **contract** (fields that must be present, their types, and their meaning).
2. The sender fills the contract with evidence, constraints, and the expected output shape.
3. The receiver **validates** the contract before acting on it (contract-first, not
   contract-after).
4. The receiver produces a new contract for the next stage, passing along what it needs.

**Tradeoffs:** explicit and safe, but the contract must be designed up front — too little
and you lose information, too much and you over-constrain. Medium friction between a loose
handoff and a full API call.

**Production rules:**
- Version the contract; never add required fields without a release.
- The handoff payload must be **validated, not trusted**.
- Include **provenance** (id, timestamp, source) so you can trace why a value is present.

**Anti-patterns:**
- Passing prose/JSON without a schema — you cannot validate it.
- Silent field drops between agents.
- No fallback when a contract is incomplete.

**Related patterns:** Handoff protocol, Mediator (transport), Router/supervisor (decision
handoff), Blackboard (shared state).

#### Message envelope / protocol
**What it is:** every message carries a **standard envelope** of fields — `id`, `type`,
`sender`, `receiver`, `correlation_id`, `payload`, `status`, `timestamp` — so agents can
route, correlate, and trace each other without knowing who the other agents are.

**When to use:** reliable inter-agent communication and tracing; when agents must be
swapped, added, or deprecated without breaking the system.

**How it works:**
1. Every message carries the envelope fields.
2. The sender sets `id` + `correlation_id` to make a request/response pair traceable.
3. The receiver reads the envelope, validates, and responds with the same `correlation_id`
   so the pair can be matched.
4. `status` tracks delivery state (sent, delivered, error, ack), enabling tracing and
   retries.

**Tradeoffs:** adds overhead to every message but pays for it in debuggability and
decoupling. The envelope is the shared vocabulary of the whole system.

**Production rules:**
- `correlation_id` must be included on requests **and** echoed on responses.
- Marshal/unmarshal the envelope at the edges only; keep it human-readable for tracing.
- Never put business logic in the envelope; it is a protocol, not a domain model.

**Anti-patterns:**
- Inconsistent envelope across agents (can't correlate).
- Missing correlation_id → untraceable messages.
- Treating `status` as business state (it is delivery state).

**Related patterns:** Handoff protocol, Mediator (policy enforcement point).

#### Request–response
**What it is:** one agent sends a request and **waits for a correlated response**. The
correlation id ties the two messages into a pair.

**When to use:** tool-like delegation, RPC over messages, scenario where you need a value
from another agent and have no reason to process asynchronously.

**How it works:**
1. The sender creates a request with a unique `id` and a `correlation_id` it will reuse.
2. It sends the request, then **waits** (blocking) or **channels** for the response.
3. The receiver processes the request and returns a response with the **same** `correlation_id`.
4. The sender matches the response to the request by `correlation_id` and can then take the
   next step (or retry with backoff).

**Tradeoffs:** synchronous by nature; latency is bounded by the slowest call. If the
responder is slow or unavailable, the sender blocks — add timeouts and fallback.

**Production rules:**
- Always set a **timeout** on a request; never block forever.
- Pass `correlation_id` through the whole chain so errors can be correlated end-to-end.
- Idempotent requests so a retransmit does not duplicate side effects.

**Anti-patterns:**
- No timeout → hangs forever.
- Different `correlation_id` on request and response → unmatchable.
- Non-idempotent request body (duplicate on retry).

**Related patterns:** Handoff protocol, Mediator, Tool calling (the app-level form of RPC).

#### Conversation / group chat with turn-taking
**What it is:** agents speak in a **managed thread**; a policy selects whose turn it is,
and a turn can end with a decision to continue, yield, or close.

**When to use:** collaborative drafting/review, multi-party debate, or any workflow where
agents speak in sequence rather than broadcasting.

**How it works:**
1. A shared turn-taking policy determines the next speaker (round-robin, by role, by score,
   by policy rule).
2. The current agent produces a message; the system appends it to the thread and switches.
3. A **stop condition** decides when the conversation is done — all agents spoke, criteria
   met, or a quorum voted yes.
4. The final artifact is the last message, optionally merged.

**Tradeoffs:** natural for collaboration but requires a turn policy and a stop condition;
without a stop condition, conversations can run forever (bound the rounds).

**Production rules:**
- **Bound the turn budget** (max rounds).
- Make the stop condition explicit and testable.
- Keep the turn policy in code, not in a model's whim (deterministic beats probabilistic).

**Anti-patterns:**
- No turn budget → infinite loop.
- No stop condition → conversation never ends.
- Policy decisions in the model (change them by code, not by prompt).

**Related patterns:** Mediator (routing of messages), Debate & consensus, Evaluator–Optimizer
(bounded refinement).

---

## 6. Choosing a pattern (quick guide)

| You have… | Choose… |
|---|---|
| Single clear task, few tools | Basic agent loop + tool calling + guardrails |
| External facts / current data | RAG; abstain without evidence |
| Multi-step with dependencies | Plan-and-execute + replan + HITL for risky steps |
| Quality-critical output | Evaluator–optimizer/reflection + explicit criteria |
| Known specialist intents | Router/supervisor; hierarchical if large |
| Independent subtasks | Parallel fan-out + aggregator |
| Decoupled reactive workflow | Event-driven pub/sub |
| High-impact side effects | HITL approval gate + idempotency + tracing + evals |

---

## 7. Cross-cutting rules (apply to all patterns)

- **Bound everything:** steps, time, tokens/cost, retries, fan-out, debate rounds, debate rounds.
- **Validate boundaries:** tool args/results, model outputs, handoff payloads.
- **Least privilege + HITL** for destructive/external actions.
- **Trace and evaluate:** make runs debuggable and regression-testable.
- **Treat retrieved/model content as untrusted** (prompt-injection aware).
