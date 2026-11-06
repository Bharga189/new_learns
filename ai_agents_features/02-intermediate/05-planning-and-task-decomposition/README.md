# 05 — Planning & Task Decomposition

Complex goals are easier when the agent makes a **plan**, executes step by step, and **replans** when reality disagrees.

## What you'll learn

- Decompose a goal into small, verifiable tasks.
- Track task status (`pending`, `in_progress`, `done`, `blocked`).
- Replan when a step fails or new information appears.
- Keep the plan visible to the user (and bounded in size).

## Pattern

```text
goal -> create plan (tasks with dependencies)
while tasks remain:
  pick next ready task -> execute -> record result
  if result changes assumptions: replan
return final summary
```

## Run

```bash
python 02-intermediate/05-planning-and-task-decomposition/main.py
```

## Key ideas / best practices

- Make tasks concrete and testable ("run migration", not "handle database").
- Record evidence/observations, not just status changes.
- Bound replans and total steps to avoid thrashing.
- Prefer deterministic scheduling in code; use the model for decomposition/judgment.
