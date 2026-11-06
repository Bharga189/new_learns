# Automated Evals

Evals test **agent behavior**, not just model text. This harness runs small, explicit cases and records pass/fail + diagnostics so prompt/orchestration changes can be regression-tested.

## Structure

- `harness.py` — minimal runner: executes cases, checks expectations, writes a JSON report.
- `cases/golden.py` — "should work" tasks with expected properties.
- `cases/tool_failures.py` — transient errors, bad tool args/results, missing data.
- `cases/adversarial.py` — prompt-injection-shaped retrieved content, unsafe action requests, ambiguity/abstention.
- `cases/jev_gate.py` — the Jev refund gate must never approve when its check is broken (upstream error, missing/malformed answer, wrong type) and must still approve on a clean check.
- `cases/jev_cascade.py` — the Jev cost cascade must never accept an unverified draft: a broken cheap check escalates, a broken strong check goes to a human, and a clean cheap draft still ships without the strong model.
- `run_evals.py` — runs all suites.

## Run

```bash
python evals/run_evals.py
```

## What to measure (extend in production)

- Task success / rubric score
- Groundedness and citation accuracy (RAG)
- Tool-call correctness and argument validity
- Safety: refused/blocked unsafe actions, injection resistance
- Reliability: behavior under tool failures/timeouts
- Cost/latency/tokens per run
- Regression diffs across prompt/schema/orchestration versions

## Regression workflow

1. Keep cases in version control alongside prompts/tool schemas.
2. Run evals on every change (CI) and compare to a baseline report.
3. Investigate regressions with traces (`shared/tracing.py` shape or OpenTelemetry).
4. Add a new case for every production failure you fix.
