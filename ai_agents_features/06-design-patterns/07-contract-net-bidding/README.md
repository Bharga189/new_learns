# Contract Net / Bidding (Task Auction)

A **manager** announces a task, eligible **contractors bid** on it (capability, cost, confidence, current load, time), and the manager **awards** the contract by an explicit scoring policy. Non-eligible agents must **refuse** rather than bid, and losing/no bids are handled explicitly.

## What it is

A **dynamic capability router**. Instead of a static mapping ("task X always goes to worker Y"), the manager asks the marketplace: "who can do this, at what cost and ETA, and how confident are they". The award decision is a **score**, not a hard-coded rule, which lets you load-balance and adapt without redeploying routing logic.

## When to use

- You don't know in advance which worker can/should do a task.
- You need load balancing across agents or tools.
- Cost/latency/quality tradeoffs should be negotiated, not hard-coded.
- Heterogeneous workers where "who is best" changes per task.

## How it works

1. Manager sends a `CallForProposals` (task, capability, budget, deadline).
2. Each contractor evaluates the CFP in `receive_cfp()`: capability, budget, load, deadline. **Non-eligible contractors refuse** (with a reason) rather than bid.
3. Eligible contractors return a `Bid` (cost, eta, confidence, load).
4. `Manager.run()` gathers bids + refusals, then `score_bid()` computes a deterministic weighted score (cost 0.35, eta 0.30, confidence 0.20, load 0.15), each term normalized to 0..1 against the CFP limits.
5. The manager awards to the best bid; on rejection it falls back to the next-best bid.
6. If no bids come in, it returns `NO_AWARD` (fast fail → human/router).

## Code walkthrough (from `main.py`)

- `Contractor.receive_cfp(cfp)` → bids or refuses based on capability/budget/load/deadline.
- `score_bid(bid, cfp)` — the pure, testable award policy.
- `Manager.run(cfp, contractors)` — the auction: collect answers, score, sort desc, award with fallback.

## Run

```bash
python 06-design-patterns/07-contract-net-bidding/main.py
```

Expected output (best-value scenario):

```text
[orchestrator] cfp for 'document the retry policy' cap=write_docs budget=15 deadline=90s
[orchestrator] bid from doc-writer: cost=8 eta=60s conf=0.90 load=0.20
[orchestrator] bid from fast-writer: cost=12 eta=20s conf=0.70 load=0.30
[orchestrator] refuse from busy-writer: too loaded (90%)
[orchestrator] refuse from ops-bot: lacks capability 'write_docs'
[orchestrator] considering doc-writer score=0.563
[AWARD 'document the retry policy' to doc-writer]
[doc-writer] accepted contract for 'document the retry policy' (cost=8, eta=60s)
```

The `busy-writer` refuses (load 90% > 80%), `ops-bot` refuses (wrong capability), and `doc-writer` wins despite higher cost because its lower cost + higher confidence + lower load score better than `fast-writer`.

## Tradeoffs

- Bidding adds a round trip (latency) and coordination overhead.
- Self-reported bids can be strategic (agents inflate confidence/underbid) — validate or measure.
- Needs a deterministic, inspectable award policy; ties and no-bid cases must be defined.

## Production rules

- Keep the award policy a **pure function** so it's easily tested.
- Record every bid **and** the award rationale for audit.
- A **human approval gate** belongs in front of high-impact awarded actions.
- Treat bids as **untrusted input**: validate capability claims against real tool availability.
- Handle no-bid, ties, and the "winner declines" case explicitly (fallback).

## Anti-patterns

- Letting agents inflate confidence to win.
- Hard-coding who should win instead of scoring.
- No fallback when the top bidder declines.
- No no-bid handling → silent task failure.

## Related patterns

- Router/supervisor (the static special case of contract net)
- Mediator (for spreading the CFP and bids)
- Human approval gate (before high-impact awards)
