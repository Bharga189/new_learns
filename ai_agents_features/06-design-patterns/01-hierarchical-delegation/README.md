# Hierarchical Delegation

A **manager agent** breaks work into areas, delegates to **sub-managers or workers**, collects results, and **escalates** when a subtask is ambiguous, blocked, or high risk.

## What it is

Hierarchical delegation adds **ownership layers** on top of a flat pool of workers. A director turns the goal into tasks; each manager routes tasks to the worker that can do the job; anything no one can do is escalated to the director, which reports it as blocked. The key idea is that **a task that cannot be handled near the bottom must be promoted up a level instead of falling through**.

## When to use

- Large goals that naturally decompose into domains (research, implementation, review).
- You need clear ownership with escalation paths.
- A supervisor/router is too flat for the task size (many distinct sub-teams).

## How it works

1. `Director.run()` receives a `goal` and a list of `subtasks`.
2. It builds two `Manager` instances (`eng-manager`, `ops-manager`), each holding typed `Worker` instances.
3. For each subtask it asks each manager in order: `Manager.delegate()` walks its workers and returns `ok` (the first worker whose keywords match), otherwise `escalate`.
4. If a manager escalates, the director moves to the next manager; if **all** managers escalate, the director records a `blocked` result for the director itself.
5. Results are collected with `owner` (who handled it), `status` (`ok` | `blocked`), and `output`.

## Code walkthrough (from `main.py`)

- `Worker.handle(task)` → class-level keyword matching; returns `ok` or `escalate`.
- `Manager.delegate(task)` → tries workers; on no match returns an `escalate` result with "needs higher-level decision".
- `Director.run()` → the top of the chain; implements the escalation policy (try each manager, else `blocked`).

## Run

```bash
python 06-design-patterns/01-hierarchical-delegation/main.py
```

Expected output (controller flow):

```text
[director] goal: Ship feature safely
[eng-manager] delegating: build API endpoint
[eng-manager] accepted from backend
[eng-manager] delegating: design UI form
[eng-manager] accepted from frontend
[eng-manager] delegating: set legal policy
[eng-manager] escalating
[ops-manager] delegating: set legal policy
[ops-manager] escalating
[director] blocked: set legal policy
```

This shows the **escalation + blocking** path, not just the happy path: `set legal policy` is outside every worker's scope, so it bubbles up to the director and becomes `blocked`.

## Tradeoffs

- More coordination/state than a single supervisor.
- Risk of unclear ownership or duplicated work; requires explicit contracts and budgets.

## Production rules

- Define the **escalation contract**: a worker that cannot handle a task must return a structured `escalate` signal with a reason, never a raw `None`/silent skip.
- Keep ownership explicit: each subtask has exactly one accountable owner.
- Set **timeouts** at each delegation boundary so a stuck worker cannot block the whole run.
- Validate at the boundary: worker scope should be typed (capability list), not a loose substring match, in production systems.

## Anti-patterns

- Workers that overlap in capability → duplicated work and race conditions.
- A manager who does the work instead of delegating → a point of serial failure.
- No escalation owner → tasks silently `None`/lost.

## Related patterns

- Router/supervisor (the flatter alternative)
- Handoff protocol (contracts for what a delegated task carries)
- Hierarchical policy / escalation policy
