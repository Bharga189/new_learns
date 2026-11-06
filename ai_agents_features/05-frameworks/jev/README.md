# Jev (TypeSafe AI) — a typed decision layer inside your agent

**Jev is not an agent framework and not an LLM.** It is a *System One* model: you send it a **state** plus a map of **typed questions**, and it returns **typed answers with probabilities and confidence**. No prose, no tool calls, no conversation, no memory.

```text
state + typed questions  ->  structured decisions (with probabilities)
```

That inversion is the whole idea. A ReAct loop spends a full LLM call to answer questions like "which tool next?", "is this refund justified?", "are we done?" — questions whose answers are an enum, a number, or a boolean. Jev answers all of them in **one round trip** and hands your code numbers you compare against thresholds.

> The vendor (TypeSafe AI) reports up to ~200× faster and ~400× cheaper than comparable LLMs *on classification-style tasks*. Treat that as a vendor claim, not something this repo measured.

---

## 1. What it is not

A common misreading is that "Jev" is something you swap in as your agent's brain. It isn't, and TypeSafe's own docs say so directly.

| You want to… | Wrong tool | Right move |
|---|---|---|
| Replace the model driving a coding agent | Jev | Keep the LLM; use Jev for narrow decisions your code owns |
| Get orchestration, state, retries, checkpoints | Jev | An agent framework (LangGraph, CrewAI, AutoGen…) |
| Get "permission" to do something | Jev | Your policy engine / authorization code |

Jev answers **"what bounded decision follows from this state?"** It does not decide what is *permitted*. In this repo's [mediator policy demo](../../06-design-patterns/08-mediator/) the authorization rules live in code; a Jev call is the *judgment* input, never the permission itself.

---

## 2. The three primitives

| Type | Question shape | Answer | Typical use |
|---|---|---|---|
| **Choice** | One option from a fixed set you define with `criteria` | Chosen option + per-option probabilities | Route to queue/agent/model; pick next tool |
| **Score** | Rate this state on a rubric you define | A number on your rubric | Risk, urgency, quality, relevance |
| **Noul** | A proposition that is true or false *of the state* | Probability in `[0, 1]` | Gates: asked for a refund? within policy? are we done? |

`Noul` is the primitive people underestimate. It is not "should we approve?" — that's a decision your code makes. It's "is this statement true of this state?", so the probability means something you can threshold and log.

**Write propositions, not decisions.** Compare:

- Bad: `"Should the agent refund this order?"` — mixes policy, authorization and evidence into one fuzzy question.
- Good: `"The situation the customer describes qualifies for a refund of \`refund.amount_cents\` under \`policy\`."` — one checkable claim; your code then decides what to do with 0.93 vs 0.55.

---

## 3. Wire format

Endpoint (via OpenRouter): `POST https://openrouter.ai/api/v1/systemone`, `Authorization: Bearer <key>`.

```json
{
  "model": "jev-1.13",
  "state": "I was charged twice for my subscription.",
  "questions": {
    "refund": { "type": "noul", "instructions": "Is the customer asking for money back?" },
    "department": {
      "type": "choice",
      "instructions": "Which team should handle this?",
      "criteria": { "billing": "Charges and refunds", "technical": "Bugs and outages" }
    }
  }
}
```

```json
{
  "id": "gen-dec-1789738314-X5e5eKGQdvR9rblyX250",
  "model": "typesafe/jev-1.13-20260917",
  "provider": "TypeSafe",
  "answers": { "refund": { "type": "noul", "noul": 0.98 } },
  "usage": { "input_tokens": 275, "output_tokens": 20, "cost": 0.00003 }
}
```

Notes that bite in practice:

- **Answer keys mirror question keys.** Missing answers must be treated as failures, never as a pass.
- **Model IDs:** `jev-1.13`, `jev-latest`, or the prefixed `typesafe/jev-1.13`. The response `model` tells you which prefixed build actually served the request — log it for reproducibility.
- **`state`** can be text, a JSON object, or an array of text. Everything a careful reviewer would look at belongs in it, and *nothing else*: accuracy degrades when state fills with unrelated transcript.
- 1–8 questions per request; multiple questions share one evaluation of the same state, which is where the cost win comes from.
- Reads text and images (PNG/JPEG); convert audio/video to text or structured fields first.

---

## 4. Where it sits in an agent loop

| Position | Question you hand Jev | What your code does with it |
|---|---|---|
| **Router / triage** | Choice over queues, owners, models | Threshold the top probability; low confidence → human queue |
| **Tool gate** | Noul: "does the user's request authorize this action?" | `approve` / `block` / `review` — see §5 |
| **Loop control** | Noul: "the requested change is made and verified" | Stop, continue, or escalate instead of spinning |
| **Output verification** | Noul: "this draft is grounded in the supplied policy" | Send, or regenerate — cheaper than a second LLM judge |
| **Cascade** | Noul: "this cheap-model answer is correct" | Accept cheap answer or escalate to a strong model — see [`cascade.py`](cascade.py) |
| **Batch labeling** | Choice tags + Score per row | Runs per row at a price an LLM call per row can't match |

The pattern is always the same: **LLM for generation, code for execution, Jev for the bounded judgment in between.**

---

## 5. Threshold policy (the part people get wrong)

A probability is not a verdict. Your code owns the mapping:

```python
APPROVE_AT = 0.80   # every proposition must clear this
BLOCK_AT   = 0.20   # any proposition below this is a clear no

if any(p <= BLOCK_AT for p in probs):        -> block
elif all(p >= APPROVE_AT for p in probs):    -> approve
else:                                        -> review (human)
```

Three rules that make this safe:

1. **Three outcomes, not two.** `review` is the honest home for 0.5, and it's where a per-call gate earns its keep: instead of pausing *every* call to a risky tool, you pause only the genuinely unclear ones.
2. **Fail closed.** Non-2xx, missing answer, or a probability outside `[0, 1]` must raise — and the handler must land on `review`/`block`, never `approve`. A broken check that silently approves is worse than no check.
3. **Tune on your own traces.** Thresholds are business policy, not defaults to copy. Log probabilities with every decision so you can move cutoffs with evidence, and watch calibration drift as the state distribution shifts.

Keep cheap deterministic checks **before** the model call (order exists, amount ≤ remaining balance, amount > 0). Don't pay for a judgment you can compute.

---

## 6. Real-time usage cases

- **Support triage at inbound rate:** one request per ticket returns queue (Choice), priority (Score), and "does this ask for money back" (Noul). Uncertain routing → human queue.
- **Per-call authorization for high-impact tools:** the refund gate in this example. Static rules stay for tools that are always dangerous; Jev handles tools whose safety depends on the call.
- **Human-in-the-loop reduction:** pause only the ambiguous calls, cutting review volume while keeping a human on exactly the calls that need judgment.
- **Coding-agent permission prompts:** decide which shell/file actions are routine for the session so a developer isn't approving the same `ls` for the tenth time.
- **Cost cascade:** verify a cheap model's answer before escalating; escalate only when the verification probability is low.
- **Classification/labeling at volume:** tags and sentiment scores on millions of rows where per-row LLM calls are unaffordable.
- **Model routing:** pick per-request model and effort tier, then let the chosen LLM generate.

---

## 7. Where it does not fit

- **Anything requiring generated text.** It won't draft the reply; keep your LLM for that.
- **Multi-step reasoning or tool use.** It takes no steps and calls no tools.
- **Authorization.** It has no notion of who may do what; that's your policy engine's job.
- **Garbage state.** Bad or bloated state produces confidently wrong probabilities.
- **Decisions you can compute.** Balance arithmetic doesn't need a model.
- **Regulated/opaque decisions without review.** Calibrated ≠ correct; you still own the audit trail and the appeal path.

---

## 8. This example

`main.py` runs **offline with no key** using a clearly-labelled `MockJevClient`: the probabilities are canned per scenario so the threshold logic, the fail-closed path and the HITL hand-off are all visible. The mock is **not** a decision model — it does no reasoning.

```bash
python 05-frameworks/jev/main.py
```

To hit the real model, set a key (the client is stdlib `urllib`, no dependencies):

```bash
export JEV_API_KEY=...            # or OPENROUTER_API_KEY
export JEV_MODEL=jev-1.13         # optional
export JEV_BASE_URL=https://openrouter.ai/api   # optional
python 05-frameworks/jev/main.py --real
```

The demo shows, in order:

1. **Choice routing** with a confidence floor — a low top-probability falls back to `human_review` instead of guessing.
2. **Score** on a risk rubric, used as one input to escalation.
3. **Noul gate** on a refund: deterministic pre-checks first (amount vs. remaining balance), then one Jev request with three propositions → `approve` / `block` / `review`.
4. **Fail-closed**: the mock omits an answer for one case, and the gate degrades to `review` rather than approving.

The request body is printed for the real client so you can see exactly what goes on the wire.

### 8.1 `--probe`: confirm the real answer shapes before trusting them

The accessors (`noul` / `choice` / `score`) enforce the documented shapes, but the *only* way to know what a build actually returns is to look. `--probe` sends one request and prints the wire body, the raw JSON response, the observed field names and types per answer, and whether the accessors accepted them:

```bash
python 05-frameworks/jev/main.py --probe      # one request, full response
python 05-frameworks/jev/main.py --real       # the whole demo, live (10 requests)
```

Run `--probe` first against any new model id — if a field name has changed, you will see `ACCESSOR REJECTED` instead of a silently wrong decision. In `--real` mode the demo also reports the served `model` string and the accumulated `usage` cost, so the run is reproducible and attributable.

### 8.2 `cascade.py`: verify cheap answers, escalate only when unconvinced

The cost cascade, where Jev replaces a second LLM judge:

```bash
python 05-frameworks/jev/cascade.py           # offline: scripted drafts, canned verdicts
python 05-frameworks/jev/cascade.py --real    # real drafts + real checks
```

```text
cheap model drafts  ->  one Jev request, two Nouls  ->  both >= 0.85 ? ship it
                                                    ->  else strong model, verify again
                                                    ->  still unconvinced ? human
```

The propositions are `grounded` ("every claim about what the company will do is supported by `policy`") and `resolves` ("gives a concrete next step that answers what was asked"). Both matter: a draft can be perfectly grounded and still useless ("Damaged items are covered by our refund policy" — true, but says nothing about what happens next), which is exactly what the `C-04` case shows.

Five cases: cheap draft already good; cheap draft invents a refund the policy forbids; neither tier resolves a chargeback threat; cheap draft grounded but not resolving; and a **broken first check** — which must escalate, not accept. In a cascade the safe direction on an unavailable check is *up*, never *accept*, and that's the same fail-closed rule as the gate, pointed the other way.

The cost table is in **illustrative units, not real prices** (cheap 1, strong 40, verify 0.2). It exists to show the arithmetic, not to claim a ratio: the win depends entirely on how often the cheap draft is actually good. At a low acceptance rate a cascade is *slower and more expensive* than going straight to the strong model, because you pay for the cheap attempt and the verification on the way to the same place. Measure your own acceptance rate before adopting it.

#### Real models, real checks: `--real`

```bash
export OPENAI_API_KEY=...            # drafts, via shared/llm_adapter.py
export OPENAI_MODEL=gpt-4.1-mini     # cheap tier (adapter default)
export OPENAI_STRONG_MODEL=gpt-4.1   # strong tier
export JEV_API_KEY=...               # verification
python 05-frameworks/jev/cascade.py --real
```

Both tiers are `shared.llm_adapter.OpenAICompatibleClient`, so any OpenAI-compatible endpoint works (`OPENAI_BASE_URL`) — point it at a local server for a free run. The check is the same `HttpJevClient` used by `main.py`.

Two rules the code enforces rather than documents:

- **No silent mock in `--real`.** The shared adapter returns `MockLLMClient` when no key is set; the cascade detects exactly that and exits with an error instead of quietly printing canned text and calling it a live run.
- **No invented prices.** Real runs report **tokens**, not money — the illustrative unit table would be a fabrication at that point. Multiply by your own per-token rates to compare against always-strong.

This is the closest thing here to a production cascade, and it's the honest version of the cheaper-model claim: what you get back is the acceptance rate on *your* traffic and the token totals, which is the only evidence that decides whether the cascade is worth it.

### 8.3 The gate is under test

`evals/cases/jev_gate.py` runs ten cases against the refund gate, one per failure mode: upstream `JevError`, a non-`JevError` (timeout/bug), empty answers, one missing answer, probability `1.4`, a `choice` where a `noul` was asked, a stringified probability, an ambiguous `0.55` (→ `review`), a clean check (→ `approve`, the positive control that keeps the suite from passing vacuously), and the arithmetic pre-check (→ `block`, **zero** model calls).

```bash
python evals/run_evals.py    # includes the jev_gate suite
```

---

## 9. Further reading

- TypeSafe docs — <https://docs.typesafe.ai>
- Jev on OpenRouter (model page, cookbooks, Decisions API) — <https://openrouter.ai/typesafe>
- Gate agent tool calls with Jev — <https://openrouter.ai/docs/cookbook/building-agents/gate-tool-calls-with-jev>
- Tests in this repo that keep the gate honest — [`evals/cases/jev_gate.py`](../../evals/cases/jev_gate.py)
- LangChain's write-up — <https://www.langchain.com/blog/building-a-harness-with-jev>
