# Human Approval Gate (HITL)

A **human approval gate** pauses execution before a **high-impact action**, presents a structured action summary, and resumes only with an explicit, auditable approval decision.

## What it is

An explicit **pause point** in the agent's control flow that separates "the agent proposed an action" from "the action is executed". The gate owns: which actions need approval, how the proposal is presented, how the decision is recorded, and how the execution proceeds afterwards. It is the agentic equivalent of a `confirm()` or a production deploy gate.

## When to use

- Payments, refunds, deletions, external sends, access/permission changes.
- Production deployments/changes.
- Regulated workflows where you must be able to point to "who approved what, when".

## How it works

1. Define the set of **high-impact** actions (`refund`, `delete`, `send_external`, `deploy_prod`, `change_access`).
2. Build an `ActionProposal` with structured fields: `action`, `target`, `args`, `reason`, `risk`, `reversible`.
3. `requires_approval(proposal)` returns True only for high-impact actions; low-risk actions skip the gate entirely.
4. `request_approval(proposal, decide)` presents the full structured summary and asks the approver (the `decide` callback).
5. `run()` returns `EXECUTED ...` on approval, or `HELD: ...` on rejection, never executing on a held action.
6. Audit log records proposal + decision + execution result (documented, not shown in code).

## Code walkthrough (from `main.py`)

- `HIGH_IMPACT` set drives `requires_approval()`.
- `ActionProposal` = the contract passed to the gate.
- `ApprovalDecision` = the outcome: `approved`, `approver`, `ts`, `note`.
- `run(proposal, decide)` — the gate function: no-approval-needed paths execute straight through; high-impact paths route to `request_approval(proposal, decide)`.
- `execute(proposal)` — the side-effect; the comment notes to use **idempotency keys** in production.

## Run

```bash
python 06-design-patterns/06-human-approval-gate/main.py
```

Expected output:

```text
[hitl] approval required
  action=refund target=invoice:42 args={'amount': 19.99}
  reason=customer request risk=financial reversible=True
[hitl] approved=False approver=alice note=needs manager sign-off
HELD: refund not approved (needs manager sign-off)

EXECUTED lookup on invoice:42
```

A high-impact action (refund) is **held** when the approval decision is reject; a low-risk action (lookup) executes directly.

## Tradeoffs

- Adds latency and requires an approval UX/channel.
- Must handle timeouts, rejection, modification, and audit logging.

## Production rules

- **Fail closed**: a timeout or ambiguous decision is a hold, never an auto-approve.
- **Idempotency keys** on every executed action so retries are safe.
- Record **who/when/what/which** (proposal + decision + result) for audit.
- Keep the approval UX/channel separate from the action-execution path.
- Prefer **least privilege**: only genuinely high-impact actions sit behind the gate.

## Anti-patterns

- Asking for approval on low-risk actions (reduces velocity and trust).
- Auto-approving on timeout or missing decision.
- No audit trail → "who approved this?" is unanswerable.

## Related patterns

- Jev threshold gate ([05-frameworks/jev/README.md])
- Mediator policy enforcement ([08-mediator/policy_demo.py])
- Guardrails / policy checks ([02-intermediate/06-structured-output-and-guardrails/])
