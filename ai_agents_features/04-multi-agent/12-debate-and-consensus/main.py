"""12 - Debate & Consensus: proposer, critic, arbiter with bounded rounds.

Run: python 04-multi-agent/12-debate-and-consensus/main.py
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Critique:
    approved: bool
    issues: list[str]
    rationale: str


class Proposer:
    name = "proposer"

    def propose(self, question: str, prior_critique: Critique | None = None) -> str:
        if prior_critique is None:
            return "Use retries for every failure to improve reliability."
        return (
            "Use retries with exponential backoff only for transient, idempotent operations; "
            "fail fast on permanent errors and add tracing for attempt counts."
        )


class Critic:
    name = "critic"

    def review(self, proposal: str) -> Critique:
        issues = []
        lower = proposal.lower()
        if "every failure" in lower:
            issues.append("retries on permanent/non-idempotent failures can cause harm")
        if "idempotent" not in lower:
            issues.append("missing idempotency constraint")
        if "trac" not in lower and "observ" not in lower:
            issues.append("missing observability")
        return Critique(
            approved=not issues,
            issues=issues,
            rationale="Approved" if not issues else "Needs safety and operability constraints",
        )


class Arbiter:
    name = "arbiter"

    def decide(self, proposal: str, critique: Critique) -> str:
        if critique.approved:
            return f"Accepted: {critique.rationale}. Final: {proposal}"
        return f"Not accepted: {critique.rationale}; issues={critique.issues}"


def run_debate(question: str, max_rounds: int = 3) -> str:
    proposer, critic, arbiter = Proposer(), Critic(), Arbiter()
    critique: Critique | None = None
    proposal = ""
    for round_num in range(1, max_rounds + 1):
        proposal = proposer.propose(question, critique)
        print(f"[round {round_num} proposer] {proposal}")
        critique = critic.review(proposal)
        print(f"[round {round_num} critic] approved={critique.approved} issues={critique.issues}")
        if critique.approved:
            break
    decision = arbiter.decide(proposal, critique)  # type: ignore[arg-type]
    print(f"[arbiter] {decision}")
    return decision


if __name__ == "__main__":
    run_debate("How should our agent handle API failures?")
