"""Contract net / bidding (task auction): announce -> bid -> award -> accept.

Run: python 06-design-patterns/07-contract-net-bidding/main.py
"""

from __future__ import annotations

from dataclasses import dataclass, field


# --- protocol messages -------------------------------------------------------

@dataclass
class CallForProposals:
    task: str
    capability: str
    budget: float      # max acceptable cost
    deadline: float    # max acceptable eta (seconds)
    id: str = "cfp-1"


@dataclass
class Bid:
    contractor: str
    capability: str
    cost: float
    eta: float
    confidence: float  # 0..1, self-reported
    load: float        # 0..1, 1 = fully busy
    notes: str = ""


@dataclass
class Refusal:
    contractor: str
    reason: str


@dataclass
class Award:
    cfp: CallForProposals
    bid: Bid | None
    scores: list[tuple[str, float]] = field(default_factory=list)
    rationale: str = ""


# --- contractors -------------------------------------------------------------

class Contractor:
    """A worker that decides whether to bid, then does the work if awarded."""

    def __init__(self, name: str, capability: str, cost: float, eta: float,
                 confidence: float, load: float, available: float = 1.0) -> None:
        self.name = name
        self.capability = capability
        self.cost = cost
        self.eta = eta
        self.confidence = confidence
        self.load = load
        self.available = available  # capacity multiplier for demo variance

    def receive_cfp(self, cfp: CallForProposals) -> Bid | Refusal:
        if self.capability != cfp.capability:
            return Refusal(self.name, f"lacks capability '{cfp.capability}'")
        if self.cost > cfp.budget:
            return Refusal(self.name, f"cost {self.cost} exceeds budget {cfp.budget}")
        if self.load > 0.8:
            return Refusal(self.name, f"too loaded ({self.load:.0%})")
        if self.eta > cfp.deadline:
            return Refusal(self.name, f"eta {self.eta}s exceeds deadline {cfp.deadline}s")
        return Bid(self.name, self.capability, self.cost, self.eta, self.confidence, self.load)

    def accept(self, cfp: CallForProposals, bid: Bid) -> str:
        print(f"[{self.name}] accepted contract for '{cfp.task}' (cost={bid.cost}, eta={bid.eta}s)")
        return f"{self.name} executed '{cfp.task}'"


# --- award policy (pure, testable) ------------------------------------------

WEIGHTS = {"cost": 0.35, "eta": 0.30, "confidence": 0.20, "load": 0.15}


def score_bid(bid: Bid, cfp: CallForProposals) -> float:
    """Higher is better. Each term normalized to 0..1 against the cfp limits."""
    cost_term = 1.0 - (bid.cost / cfp.budget) if cfp.budget else 1.0
    eta_term = 1.0 - (bid.eta / cfp.deadline) if cfp.deadline else 1.0
    conf_term = max(0.0, min(1.0, bid.confidence))
    load_term = 1.0 - max(0.0, min(1.0, bid.load))
    return (
        WEIGHTS["cost"] * cost_term
        + WEIGHTS["eta"] * eta_term
        + WEIGHTS["confidence"] * conf_term
        + WEIGHTS["load"] * load_term
    )


class Manager:
    def __init__(self, name: str) -> None:
        self.name = name

    def run(self, cfp: CallForProposals, contractors: list[Contractor]) -> str:
        print(f"[{self.name}] cfp for '{cfp.task}' cap={cfp.capability} "
              f"budget={cfp.budget} deadline={cfp.deadline}s")
        bids: list[Bid] = []
        for c in contractors:
            answer = c.receive_cfp(cfp)
            if isinstance(answer, Refusal):
                print(f"[{self.name}] refuse from {answer.contractor}: {answer.reason}")
            else:
                bids.append(answer)
                print(f"[{self.name}] bid from {answer.contractor}: cost={answer.cost} "
                      f"eta={answer.eta}s conf={answer.confidence:.2f} load={answer.load:.2f}")

        if not bids:
            print(f"[{self.name}] no bids -> task fails fast (escalate to human/router)")
            return "NO_AWARD"

        scored = sorted(((b, score_bid(b, cfp)) for b in bids), key=lambda p: p[1], reverse=True)

        # Fallback: try the best bid first, then the next, on rejection.
        for bid, score in scored:
            print(f"[{self.name}] considering {bid.contractor} score={score:.3f}")
            winner = next(c for c in contractors if c.name == bid.contractor)
            if _accepts(winner, bid):
                print(f"[{self.name}] AWARD '{cfp.task}' to {bid.contractor}")
                return winner.accept(cfp, bid)
            print(f"[{self.name}] {bid.contractor} declined the award")

        print(f"[{self.name}] all bids declined -> NO_AWARD")
        return "NO_AWARD"


def _accepts(contractor: Contractor, bid: Bid) -> bool:
    # Demo hook: a contractor may decline an award (e.g. it lost capacity since bidding).
    return contractor.available >= 1.0


if __name__ == "__main__":
    manager = Manager("orchestrator")
    cs = [
        Contractor("doc-writer", "write_docs", cost=8, eta=60, confidence=0.9, load=0.2),
        Contractor("fast-writer", "write_docs", cost=12, eta=20, confidence=0.7, load=0.3),
        Contractor("busy-writer", "write_docs", cost=5, eta=30, confidence=0.95, load=0.9),
        Contractor("ops-bot", "deploy", cost=9, eta=15, confidence=0.8, load=0.1),
    ]
    print("=== scenario: best value wins ===")
    manager.run(CallForProposals("document the retry policy", "write_docs", budget=15, deadline=90), cs)

    print("\n=== scenario: nobody can do it (no award) ===")
    manager.run(CallForProposals("migrate legacy database", "dba", budget=10, deadline=30), cs)
