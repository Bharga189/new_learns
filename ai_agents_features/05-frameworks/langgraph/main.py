"""LangGraph mapping: stateful graph with routing + human-in-the-loop gate.

Run: python 05-frameworks/langgraph/main.py
Mock demo runs without deps/keys; real path requires langgraph (+ model config).
"""

from __future__ import annotations

from typing import Any, TypedDict


class AgentState(TypedDict, total=False):
    request: str
    route: str
    draft: str
    approved: bool
    result: str


def classify(state: AgentState) -> AgentState:
    text = state["request"].lower()
    route = "refund" if "refund" in text or "invoice" in text else "support"
    print(f"[classify] route={route}")
    return {"route": route}


def draft_action(state: AgentState) -> AgentState:
    draft = f"Proposed {state['route']} action for: {state['request']}"
    print(f"[draft] {draft}")
    return {"draft": draft}


def human_approval_gate(state: AgentState) -> AgentState:
    # High-impact actions require explicit approval. In real LangGraph, use an interrupt/checkpoint.
    high_impact = state["route"] == "refund"
    if not high_impact:
        print("[hitl] low-impact: auto-approved")
        return {"approved": True}
    # Mock approval decision; production resumes with an authenticated approval payload.
    approved = False
    print(f"[hitl] HIGH IMPACT: requires approval. approved={approved}")
    return {"approved": approved}


def execute_or_hold(state: AgentState) -> AgentState:
    if state.get("approved"):
        return {"result": f"EXECUTED: {state['draft']}"}
    return {"result": f"HELD FOR APPROVAL: {state['draft']}"}


def run_mock() -> AgentState:
    state: AgentState = {"request": "Please refund invoice #42"}
    state.update(classify(state))
    state.update(draft_action(state))
    state.update(human_approval_gate(state))
    state.update(execute_or_hold(state))
    return state


def run_langgraph() -> Any:
    # Real structure (APIs may evolve). This shows the graph mapping explicitly.
    from langgraph.graph import END, StateGraph

    g = StateGraph(AgentState)
    g.add_node("classify", classify)
    g.add_node("draft", draft_action)
    g.add_node("hitl", human_approval_gate)
    g.add_node("execute", execute_or_hold)
    g.set_entry_point("classify")
    g.add_edge("classify", "draft")
    g.add_edge("draft", "hitl")
    g.add_edge("hitl", "execute")
    g.add_edge("execute", END)
    app = g.compile()
    # For true HITL interrupts/checkpoints, configure checkpointer + interrupt_before=["execute"].
    return app.invoke({"request": "Please refund invoice #42"})


if __name__ == "__main__":
    try:
        import langgraph  # noqa: F401

        print("[mode] langgraph available - running graph\n")
        out = run_langgraph()
    except Exception as exc:
        print(f"[mode] mock fallback ({exc})\n")
        out = run_mock()
    print(f"\n[result] {out['result']}")
