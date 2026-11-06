# 12 — Debate & Consensus

Multiple agents can propose, critique, and converge on a better answer. **Debate** is useful when a single pass may miss risks or alternatives.

## What you'll learn

- **Roles**: proposer, critic(s), and an optional arbiter.
- Structured critiques (claims, risks, evidence needed).
- **Convergence**: stop when criteria pass or rounds are exhausted.
- **Arbiter decisions** based on evidence/criteria, not verbosity.

## Pattern

```text
proposer -> draft
critic   -> issues/risks
proposer -> revised draft (or arbiter selects/merges)
repeat until consensus or max rounds
```

## Run

```bash
python 04-multi-agent/12-debate-and-consensus/main.py
```

## Key ideas / best practices

- Give critics concrete criteria; vague critique produces vague revisions.
- Bound rounds and total cost — debates multiply model calls.
- Require evidence for disputed claims; escalate uncertainty.
- Use debate for high-stakes or ambiguous decisions, not every request.
- An arbiter should apply explicit decision rules and record rationale.
