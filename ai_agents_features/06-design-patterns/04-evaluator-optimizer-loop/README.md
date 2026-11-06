# Evaluator–Optimizer Loop

A **generator** produces a candidate; an **evaluator** scores it against explicit criteria and returns actionable feedback; the generator revises. This repeats until pass or budget is exhausted.

## What it is

A two-role loop split by **who** performs the work and **who** judges it. The generator creates something (text, a plan, an artifact). The evaluator checks it against explicit, testable criteria and returns structured feedback. The generator consumes that feedback and produces a better candidate. The loop is bounded by a max-rounds budget.

## When to use

- Clear acceptance criteria (format, correctness checks, policy, rubric).
- Quality matters more than single-pass latency.
- The evaluator can be code/tests/rules (best) or a separate model.

## How it works

1. Run the generator once with no feedback → weak first draft.
2. Run the evaluator → score + feedback per criterion.
3. If `passed`, return the candidate.
4. Otherwise feed the feedback back into the generator; the generator produces a revised candidate.
5. Repeat until `passed` or `max_rounds` is hit.

## Code walkthrough (from `main.py`)

- `Generator.run(task, feedback)` — no feedback → intentionally weak `"Deploy now."`; with feedback → a structured plan (`run tests`, `get approval`, `deploy with rollback`).
- `Evaluator` — static `criteria` list. `run(candidate)` checks each criterion deterministically (substring presence), builds a `feedback` list, and returns `Evaluation(passed, score, feedback)` where `score = (criteria - feedback) / criteria`.
- `optimize(task, max_rounds=3)` — the loop: generate → evaluate → stop if passed, else feed feedback back. Prints `[round i] score=... passed=...`.

## Run

```bash
python 06-design-patterns/04-evaluator-optimizer-loop/main.py
```

Expected output:

```text
[round 1] score=0.00 passed=False feedback=['missing tests/verification', 'missing approval for production risk', 'missing rollback/safety']
[round 2] score=1.00 passed=True feedback=[]
[final] Plan for 'Deploy the service update': run tests, get approval for production, deploy with rollback ready.
```

## Tradeoffs

- Extra cost/latency per iteration.
- Evaluator quality limits results; bound iterations.

## Production rules

- Criteria must be **deterministic and measurable**. Prefer code/rules over an LLM judge for the evaluator when possible.
- Feedback must be **actionable** ("missing tests/verification") not vague ("be better").
- **Always bound rounds** (`max_rounds`) and surface exactly where it stopped.
- Measure cost per round; a cheap evaluator can replace the model-based one as you learn.
- Keep the evaluator **independent** of the generator to avoid shared bias.

## Anti-patterns

- Vague criteria — the evaluator accepts anything.
- No round cap — an uncompliant generator loops forever.
- Evaluator mirrors the generator's bias → infinite improvement toward the same wrong target.

## Related patterns

- Reflection/Self-Refine (same shape, prose-focused)
- Evaluation harness (how you regression-test the evaluator)
- Reliability patterns (binding the loop with timeouts)
