# 10 — Sequential Handoff (Pipeline)

In a **sequential handoff**, each specialist agent completes its part and passes a structured result to the next. It's the simplest multi-agent topology: a pipeline.

## What you'll learn

- Defining **specialist roles** with narrow responsibilities.
- **Handoff contracts**: the schema each stage produces/consumes.
- Preserving context and provenance across stages.
- When to stop the pipeline on errors.

## Pattern

```text
input -> [Researcher] -> findings
      -> [Planner]    -> plan
      -> [Writer]     -> final output
```

## Run

```bash
python 04-multi-agent/10-sequential-handoff/main.py
```

## Key ideas / best practices

- Make handoff payloads explicit (goal, inputs, constraints, output, evidence).
- Validate each stage's output before passing it on.
- Keep stages independent where possible; avoid hidden shared state.
- Add checkpoints/logging per stage for debugging and evals.
- Use pipelines when work is naturally ordered; avoid them when steps need negotiation.
