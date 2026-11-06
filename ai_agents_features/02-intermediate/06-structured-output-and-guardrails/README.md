# 06 — Structured Output & Guardrails

Models generate text; systems need **reliable data and safe actions**. Guardrails validate what comes in, what the model produces, and what actions are allowed.

## What you'll learn

- Requesting/validating **structured output** (e.g., JSON matching a schema).
- **Retry with feedback** when output is invalid.
- **Input guardrails** (detect missing/unsafe requests) and **output guardrails** (block unsafe responses/actions).
- Tool/action allowlists and argument checks.

## Pattern

```text
validate input -> call model for structured output
validate output schema/policy -> if invalid, retry with errors (bounded)
validate proposed action -> execute only if allowed
```

## Run

```bash
python 02-intermediate/06-structured-output-and-guardrails/main.py
```

## Key ideas / best practices

- Treat model output as untrusted until validated.
- Bound retries; include the validation error in the retry prompt.
- Use allowlists for tools/actions and sensitive parameters.
- Separate "can't parse/validate" from "policy denied" in logs and responses.
