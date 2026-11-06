"""AutoGen mapping: conversable agents, routed collaboration, human approval gate.

Run: python 05-frameworks/autogen/main.py
Mock demo runs without deps/keys.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Message:
    sender: str
    to: str
    content: str


class ConversableAgent:
    def __init__(self, name: str, system: str) -> None:
        self.name = name
        self.system = system

    def receive(self, msg: Message) -> Message | None:
        raise NotImplementedError


class RouterAgent(ConversableAgent):
    def receive(self, msg: Message) -> Message | None:
        route = "billing_agent" if "refund" in msg.content.lower() else "support_agent"
        return Message(self.name, route, f"Route request: {msg.content}")


class SpecialistAgent(ConversableAgent):
    def receive(self, msg: Message) -> Message | None:
        proposal = f"[proposal] {self.name} suggests: handle '{msg.content}' with verification."
        return Message(self.name, "human_proxy", proposal)


class HumanProxyAgent(ConversableAgent):
    def __init__(self) -> None:
        super().__init__("human_proxy", "Approve or reject high-impact actions.")

    def receive(self, msg: Message) -> Message | None:
        high_impact = "refund" in msg.content.lower()
        approved = not high_impact  # mock: refunds require approval
        decision = "APPROVED" if approved else "REJECTED - needs human approval"
        print(f"[hitl] {decision} for: {msg.content}")
        return Message(self.name, msg.sender, decision)


def run_mock() -> None:
    agents = {
        "router": RouterAgent("router", "Route requests."),
        "billing_agent": SpecialistAgent("billing_agent", "Handle billing."),
        "support_agent": SpecialistAgent("support_agent", "Handle support."),
        "human_proxy": HumanProxyAgent(),
    }
    msg = Message("user", "router", "Customer requests a refund for invoice #42")
    print(f"[msg] {msg.sender} -> {msg.to}: {msg.content}")
    routed = agents["router"].receive(msg)
    assert routed
    print(f"[msg] {routed.sender} -> {routed.to}: {routed.content}")
    proposal = agents[routed.to].receive(routed)
    assert proposal
    print(f"[msg] {proposal.sender} -> {proposal.to}: {proposal.content}")
    agents["human_proxy"].receive(proposal)


def run_autogen_note() -> None:
    # Real AutoGen structure depends on version/package. Conceptual mapping:
    # - AssistantAgent / ConversableAgent for specialists
    # - UserProxyAgent/HumanProxyAgent for HITL
    # - GroupChat/GroupChatManager for routing/consensus
    # - register_for_llm / register_for_execution for tools
    print("AutoGen installed-path placeholder: configure model_client and agent classes per current docs.")


if __name__ == "__main__":
    try:
        import autogen_agentchat  # type: ignore  # noqa: F401

        run_autogen_note()
    except Exception:
        print("[mode] mock fallback\n")
        run_mock()
