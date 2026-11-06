# 08 — Reflection & Self-Critique

A single model pass can miss errors. **Reflection** adds a second pass that critiques the draft against criteria, then revises.

## What you'll learn

- **Draft → critique → revise** loop.
- Using explicit criteria (correctness, completeness, safety, clarity).
- A separate **verifier** role/prompt to reduce self-confirmation bias.
- **Bounded iteration**: stop after N revisions or when criteria pass.

## Pattern

```text
draft = model(task)
for i in range(max_revisions):
  critique = verifier(draft, criteria)
  if critique.passed: return draft
  draft = model(task, draft, critique)
return draft with limitations
```

## Run

```bash
python 03-advanced/08-reflection-and-self-critique/main.py
```

## Key ideas / best practices

- Make criteria concrete and checkable where possible.
- Use a verifier with a different prompt/role; external tools/tests are even better.
- Cap revisions; reflection improves quality but increases latency/cost.
- For code, prefer running tests/linters as the verifier over text-only critique.
