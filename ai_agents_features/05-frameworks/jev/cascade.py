"""Cost cascade: cheap model drafts, Jev verifies, escalate only when unconvinced.

The pattern:
  1. cheap model writes a draft                      (cheap LLM call)
  2. one Jev request asks "is this draft grounded in policy and does it resolve
     the customer's ask?" -> two Nouls, no generated text
  3. both clear ACCEPT_AT  -> ship the cheap draft, done
     otherwise             -> strong model rewrites, verify again
     still unconvinced     -> human

The verification is the expensive part of a *judgment*, not a generation, so it
costs a fraction of asking a second LLM judge to grade a draft in prose.

Run offline (canned mocks, no key):   python 05-frameworks/jev/cascade.py
Run with real models and a real check:
    OPENAI_API_KEY=... JEV_API_KEY=... python 05-frameworks/jev/cascade.py --real
"""

from __future__ import annotations

import importlib.util
import os
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

# --- reuse the client layer from main.py without duplicating it ---------------


def _load_jev_main():
    path = Path(__file__).with_name("main.py")
    spec = importlib.util.spec_from_file_location("jev_main", path)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    sys.modules["jev_main"] = module
    spec.loader.exec_module(module)
    return module


jev = _load_jev_main()
JevClient = jev.JevClient
JevError = jev.JevError
JevResult = jev.JevResult

ACCEPT_AT = 0.85  # both propositions must clear this to accept the cheaper draft

SUPPORT_POLICY = """
Refunds go to the original payment method within 5-10 business days.
Late delivery: if the order arrived more than 5 days after the promised date,
refund the shipping fee, or the full order if the customer no longer wants it.
Damaged or defective items: full refund of the item price, no return required.
Change of mind: unopened items can be returned within 30 days for the item price.
Opened items are not refundable.
Digital goods are not refundable once downloaded.
We cannot promise outcomes for chargebacks; those go to the billing team.
""".strip()

# Propositions, not decisions. Both are true/false statements about the draft.
VERIFY_QUESTIONS = {
    "grounded": {
        "type": "noul",
        "instructions": (
            "Every factual claim `draft` makes about what the company will do is supported by "
            "`policy`. Ignore restatements of `customer_message`. `policy` is the only source of "
            "truth; anything `draft` promises beyond it is not grounded."
        ),
    },
    "resolves": {
        "type": "noul",
        "instructions": (
            "`draft` gives the customer in `customer_message` a concrete next step that answers "
            "what they actually asked, and the step is consistent with `policy`."
        ),
    },
}

# Illustrative cost units, not real prices. Cheap: 1 unit, strong: 40 units, verify: 0.2.
COST_CHEAP = 1.0
COST_STRONG = 40.0
COST_VERIFY = 0.2


# --- models (stubs) ----------------------------------------------------------


@dataclass
class ScriptedModel:
    """A stand-in for an LLM. It returns a fixed draft per case; it does not reason."""

    tier: str
    cost: float
    drafts: dict[str, str]

    def draft(self, case: dict[str, Any]) -> str:
        return self.drafts[case["id"]]


SUPPORT_PERSONA = (
    "You are a customer support agent. Reply to the customer in 2-4 sentences. "
    "Use only the supplied policy: never promise a refund, timeline or outcome the "
    "policy does not allow. End with the concrete next step."
)


@dataclass
class LLMModel:
    """A real model behind the same `.draft(case)` interface as ScriptedModel.

    Token counts are tracked instead of inventing a price: the cascade's cost
    argument only holds if you plug in your own per-token rates.
    """

    client: Any
    tier: str
    label: str
    cost: float = 0.0  # unpriced; see prompt_tokens/completion_tokens
    prompt_tokens: int = 0
    completion_tokens: int = 0

    def draft(self, case: dict[str, Any]) -> str:
        response = self.client.complete([
            {"role": "system", "content": SUPPORT_PERSONA},
            {"role": "user", "content": f"POLICY:\n{SUPPORT_POLICY}\n\n"
                                          f"CUSTOMER MESSAGE:\n{case['customer_message']}"},
        ])
        usage = (response.raw or {}).get("usage") or {}
        self.prompt_tokens += int(usage.get("prompt_tokens") or 0)
        self.completion_tokens += int(usage.get("completion_tokens") or 0)
        return response.text.strip()


def build_real_models() -> tuple[LLMModel, LLMModel]:
    """Cheap and strong tiers from the shared adapter. Never silently falls back
    to the mock: a --real run that quietly used canned text would be a lie."""
    import shared.llm_adapter as adapter

    cheap_client = adapter.from_env()
    if isinstance(cheap_client, adapter.MockLLMClient):
        raise SystemExit("error: --real needs OPENAI_API_KEY (the adapter would otherwise return a mock)")
    strong_name = os.environ.get("OPENAI_STRONG_MODEL", "gpt-4.1")
    strong_client = adapter.OpenAICompatibleClient(
        api_key=cheap_client.api_key,
        base_url=cheap_client.base_url,
        model=strong_name,
    )
    return (
        LLMModel(cheap_client, "cheap", cheap_client.model),
        LLMModel(strong_client, "strong", strong_name),
    )


def build_real_verifier() -> JevClient:
    try:
        return jev.HttpJevClient.from_env()
    except JevError as exc:
        raise SystemExit(f"error: --real needs a Jev key too ({exc})") from exc


class TableJev(JevClient):
    """Offline Jev stand-in keyed by (case id, draft tier).

    `broken` lets a scenario simulate a failing check (missing answer or raise),
    which the cascade must treat as *unverified*, never as *verified*.
    """

    def __init__(self, table: dict[tuple[str, str], dict[str, dict[str, Any]]],
                 broken: dict[tuple[str, str], str] | None = None) -> None:
        self.table = table
        self.broken = broken or {}
        self.calls = 0

    def system_one(self, state: Any, questions: dict[str, dict[str, Any]]) -> JevResult:
        self.calls += 1
        key = (state.get("case_id"), state.get("tier"))
        mode = self.broken.get(key)
        if mode == "raise":
            raise JevError("simulated upstream 503")
        if mode == "empty":
            return JevResult(model="mock-jev", answers={})
        answers = self.table.get(key)
        if answers is None:
            raise JevError(f"mock has no verdict for {key}")
        return JevResult(model="mock-jev", answers={k: v for k, v in answers.items() if k in questions})


# --- the cascade -------------------------------------------------------------


@dataclass
class CascadeOutcome:
    case_id: str
    handled_by: str  # cheap | strong | human
    accepted: bool
    probabilities: dict[str, float] = field(default_factory=dict)
    reason: str = ""
    cost: float = 0.0
    llm_calls: int = 0
    verify_calls: int = 0


def verify(client: JevClient, case: dict[str, Any], draft: str, tier: str) -> tuple[dict[str, float] | None, str]:
    """One Jev request. Returns (probabilities, note); None means unverified."""
    state = {
        "case_id": case["id"],
        "tier": tier,
        "policy": SUPPORT_POLICY,
        "customer_message": case["customer_message"],
        "draft": draft,
    }
    try:
        result = client.system_one(state, VERIFY_QUESTIONS)
        probs = {key: result.noul(key) for key in VERIFY_QUESTIONS}
    except JevError as exc:
        return None, f"check unavailable: {exc}"
    return probs, ""


def run_cascade(client: JevClient, case: dict[str, Any],
                cheap: ScriptedModel, strong: ScriptedModel) -> CascadeOutcome:
    outcome = CascadeOutcome(case_id=case["id"], handled_by="human", accepted=False)
    notes: list[str] = []

    for model in (cheap, strong):
        outcome.cost += model.cost
        outcome.llm_calls += 1
        draft = model.draft(case)

        outcome.cost += COST_VERIFY
        outcome.verify_calls += 1
        probs, note = verify(client, case, draft, model.tier)

        if probs is None:
            # Unverifiable is not verified. In a cascade the safe direction is
            # *up*: try the next tier, never accept on a broken check.
            notes.append(f"{model.tier}: {note} -> escalate")
            continue

        worst = min(probs, key=probs.get)
        if probs[worst] >= ACCEPT_AT:
            outcome.handled_by = model.tier
            outcome.accepted = True
            outcome.probabilities = probs
            outcome.reason = "; ".join(notes + [f"{model.tier} draft clears {ACCEPT_AT}: {worst}={probs[worst]:.2f}"])
            return outcome

        notes.append(f"{model.tier}: weak ({worst}={probs[worst]:.2f}) -> escalate")
        outcome.probabilities = probs

    # Distinguish "both drafts were judged and rejected" from "nothing could be
    # judged at all" -- a human reading the log needs to know which happened.
    tail = ("no draft cleared the threshold -> human" if outcome.probabilities
            else "no draft could be verified -> human")
    outcome.reason = "; ".join(notes + [tail])
    return outcome


# --- demo data ---------------------------------------------------------------

CASES: list[dict[str, Any]] = [
    {
        "id": "C-01",
        "customer_message": "Order arrived 9 days late and I no longer need it. Please refund.",
        "note": "cheap draft is already correct",
        "cheap_draft": "Sorry about the delay. Under our late-delivery policy you qualify for a full refund "
                       "because the order arrived more than 5 days after the promised date. I have started "
                       "the refund to your original payment method; it lands in 5-10 business days.",
        "strong_draft": "Same answer, longer: the refund has been started...",
    },
    {
        "id": "C-02",
        "customer_message": "I opened the box, changed my mind, and want my money back.",
        "note": "cheap draft invents a refund the policy forbids",
        "cheap_draft": "No problem at all - I have approved a full refund for your opened item, "
                       "you will see it in 3 business days.",
        "strong_draft": "Opened items are not refundable under our policy, so I cannot process a refund here. "
                        "If the item is unopened or faulty, reply and I will start a return; otherwise a "
                        "supervisor can review your case.",
    },
    {
        "id": "C-03",
        "customer_message": "I downloaded the course and want a refund, or I will file a chargeback.",
        "note": "neither tier handles the threat well -> human",
        "cheap_draft": "Sure, a chargeback will not be necessary - refund approved, money back shortly.",
        "strong_draft": "Digital goods are not refundable once downloaded, so I cannot refund this. "
                        "A chargeback decision is out of my hands.",
    },
    {
        "id": "C-04",
        "customer_message": "The mug arrived cracked. What now?",
        "note": "cheap draft is grounded but never says what happens next",
        "cheap_draft": "That is frustrating. Damaged items are covered by our refund policy.",
        "strong_draft": "Sorry it arrived cracked. Damaged items get a full refund of the item price with no "
                        "return required - I have started it to your original payment method, 5-10 business days.",
    },
    {
        "id": "C-05",
        "customer_message": "My subscription renewed and I want the charge reversed today.",
        "note": "first check breaks -> must escalate, not accept",
        "cheap_draft": "Reversed! You will see the money back today.",
        "strong_draft": "I cannot promise same-day reversal. I have opened a billing case for the renewal "
                        "charge; you will get an update within one business day and eligible refunds return "
                        "to the original payment method in 5-10 business days.",
    },
]

MOCK_VERDICTS: dict[tuple[str, str], dict[str, dict[str, Any]]] = {
    ("C-01", "cheap"): {"grounded": {"type": "noul", "noul": 0.95}, "resolves": {"type": "noul", "noul": 0.90}},
    ("C-02", "cheap"): {"grounded": {"type": "noul", "noul": 0.18}, "resolves": {"type": "noul", "noul": 0.86}},
    ("C-02", "strong"): {"grounded": {"type": "noul", "noul": 0.96}, "resolves": {"type": "noul", "noul": 0.93}},
    ("C-03", "cheap"): {"grounded": {"type": "noul", "noul": 0.11}, "resolves": {"type": "noul", "noul": 0.72}},
    ("C-03", "strong"): {"grounded": {"type": "noul", "noul": 0.90}, "resolves": {"type": "noul", "noul": 0.58}},
    ("C-04", "cheap"): {"grounded": {"type": "noul", "noul": 0.94}, "resolves": {"type": "noul", "noul": 0.41}},
    ("C-04", "strong"): {"grounded": {"type": "noul", "noul": 0.97}, "resolves": {"type": "noul", "noul": 0.92}},
    # ("C-05", "cheap") is absent on purpose below: broken[("C-05","cheap")] = "empty".
    ("C-05", "strong"): {"grounded": {"type": "noul", "noul": 0.93}, "resolves": {"type": "noul", "noul": 0.88}},
}


def run_demo(client: JevClient, cheap: Any, strong: Any, live: bool = False) -> int:
    if live:
        print(f"models: cheap={cheap.label}  strong={strong.label}")
        print(f"verify: {client.__class__.__name__} -> {getattr(client, 'base_url', '?')}")
    else:
        print("client: TableJev (offline stub - probabilities are canned, no reasoning)\n")
    print(f"accept when both Nouls >= {ACCEPT_AT:.2f}; otherwise escalate one tier, then hand to a human\n")

    outcomes = [run_cascade(client, case, cheap, strong) for case in CASES]
    for case, out in zip(CASES, outcomes):
        probs = " ".join(f"{k}={v:.2f}" for k, v in out.probabilities.items()) or "-"
        print(f"  {out.case_id}  {out.handled_by.upper():6} {probs}")
        print(f"        {case['note']}")
        print(f"        {out.reason}")

    cheap_only = sum(1 for o in outcomes if o.handled_by == "cheap")
    strong_only = sum(1 for o in outcomes if o.handled_by == "strong")
    human = sum(1 for o in outcomes if o.handled_by == "human")
    verify_calls = sum(o.verify_calls for o in outcomes)

    print("\n=== outcome ===")
    print(f"  handled by cheap draft : {cheap_only}/{len(outcomes)}")
    print(f"  escalated to strong    : {strong_only}/{len(outcomes)}")
    print(f"  escalated to human     : {human}/{len(outcomes)}")
    print(f"  verification calls     : {verify_calls}")

    if live:
        print("\n=== tokens (no price assumed) ===")
        total_in = getattr(cheap, "prompt_tokens", 0) + getattr(strong, "prompt_tokens", 0)
        total_out = getattr(cheap, "completion_tokens", 0) + getattr(strong, "completion_tokens", 0)
        print(f"  cheap : in={getattr(cheap, 'prompt_tokens', 0)} out={getattr(cheap, 'completion_tokens', 0)}")
        print(f"  strong: in={getattr(strong, 'prompt_tokens', 0)} out={getattr(strong, 'completion_tokens', 0)}")
        print(f"  total : in={total_in} out={total_out}")
        print("  multiply by your own per-token rates to compare against always-strong")
    else:
        cost = sum(o.cost for o in outcomes)
        baseline = len(outcomes) * (COST_STRONG + COST_VERIFY)
        saved = 1 - cost / baseline if baseline else 0.0
        print("\n=== cost (illustrative units, not real pricing) ===")
        print(f"  cascade cost           : {cost:.1f}")
        print(f"  always-strong baseline : {baseline:.1f}")
        print(f"  saved                  : {saved:.0%}")

    print("\n[mapping] accepted -> send draft | unconvinced -> next tier | all tiers failed -> human")
    return 0


def main(argv: list[str] | None = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description="Jev-verified cost cascade")
    parser.add_argument("--real", action="store_true",
                        help="use real models (OPENAI_API_KEY) and a real Jev check (JEV_API_KEY)")
    args = parser.parse_args(argv)

    if args.real:
        cheap, strong = build_real_models()
        client = build_real_verifier()
        return run_demo(client, cheap, strong, live=True)

    cheap = ScriptedModel("cheap", COST_CHEAP, {c["id"]: c["cheap_draft"] for c in CASES})
    strong = ScriptedModel("strong", COST_STRONG, {c["id"]: c["strong_draft"] for c in CASES})
    client = TableJev(MOCK_VERDICTS, broken={("C-05", "cheap"): "empty"})
    return run_demo(client, cheap, strong)


if __name__ == "__main__":
    raise SystemExit(main())
