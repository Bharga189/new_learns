# 13 — Shared Blackboard

A **blackboard** is shared state that multiple agents read from and write to. Agents contribute partial solutions when they have relevant information.

## What you'll learn

- A shared **state object** (the blackboard) with controlled writes.
- Agents that act based on blackboard contents, not direct messages.
- **Provenance**: who wrote what, and why.
- Simple **conflict handling** / resolution rules.

## Pattern

```text
blackboard = { goal, facts, hypotheses, plan, status }
while not done:
  for agent in agents:
    if agent.can_contribute(blackboard): agent.write(blackboard)
  resolve conflicts / check completion
```

## Run

```bash
python 04-multi-agent/13-shared-blackboard/main.py
```

## Key ideas / best practices

- Define write permissions/rules; unrestricted writes create chaos.
- Record provenance and timestamps for every contribution.
- Detect conflicts (contradictory facts/hypotheses) and resolve explicitly.
- Bound iterations and define completion criteria.
- Blackboard systems fit problems where contributions are opportunistic/parallel; use pipelines or supervisors when order/ownership is clearer.
