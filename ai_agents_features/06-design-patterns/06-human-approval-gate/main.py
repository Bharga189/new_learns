"""Human Approval Gate (HITL): pause before high-impact actions, resume on approval.

Run: python 06-design-patterns/06-human-approval-gate/main.py
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Callable


HIGH_IMPACT = {"refund", "delete", "send_external", "deploy_prod", "change_access"}


@dataclass
class ActionProposal:
    action: str
    target: str
    args: dict[str, Any]
    reason: str
    risk: str
    reversible: bool


@dataclass
class ApprovalDecision:
    approved: bool
    approver: str
    ts: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    note: str = ""


def requires_approval(proposal: ActionProposal) -> bool:
    return proposal.action in HIGH_IMPACT


def request_approval(proposal: ActionProposal, decide: Callable[[ActionProposal], ApprovalDecision]) -> ApprovalDecision:
    print("[hitl] approval required")
    print(f"  action={proposal.action} target={proposal.target} args={proposal.args}")
    print(f"  reason={proposal.reason} risk={proposal.risk} reversible={proposal.reversible}")
    decision = decide(proposal)
    print(f"[hitl] approved={decision.approved} approver={decision.approver} note={decision.note}")
    return decision


def execute(proposal: ActionProposal) -> str:
    # Use idempotency keys in production.
    return f"EXECUTED {proposal.action} on {proposal.target}"


def run(proposal: ActionProposal, decide: Callable[[ActionProposal], ApprovalDecision]) -> str:
    if not requires_approval(proposal):
        return execute(proposal)
    decision = request_approval(proposal, decide)
    if not decision.approved:
        return f"HELD: {proposal.action} not approved ({decision.note})"
    # Audit log would record proposal + decision + execution result.
    return execute(proposal)


if __name__ == "__main__":
    refund = ActionProposal("refund", "invoice:42", {"amount": 19.99}, "customer request", "financial", True)
    print(run(refund, decide=lambda p: ApprovalDecision(False, "alice", note="needs manager sign-off")))

    lookup = ActionProposal("lookup", "invoice:42", {}, "support context", "low", True)
    print(run(lookup, decide=lambda p: ApprovalDecision(True, "system")))
