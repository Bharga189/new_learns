"""LangChain mapping: composable runnable flow (LCEL-style), tool call, HITL gate.

Run: python 05-frameworks/langchain/main.py
Mock demo runs without deps/keys. See README for real LangChain package/API notes.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable


# --- Mock "runnables" to illustrate LCEL-style composition: prompt | model | parser ---

@dataclass
class Prompt:
    template: str

    def invoke(self, inputs: dict[str, Any]) -> str:
        text = self.template
        for k, v in inputs.items():
            text = text.replace("{" + k + "}", str(v))
        print(f"[prompt] {text}")
        return text


@dataclass
class MockChatModel:
    def invoke(self, prompt_text: str) -> dict[str, Any]:
        # Real LangChain: model.bind_tools(...).invoke(...) may return tool calls.
        if "refund" in prompt_text.lower():
            return {"type": "tool_call", "name": "issue_refund", "args": {"invoice_id": "42", "amount": 19.99}}
        return {"type": "final", "text": "I can help with that support request."}


@dataclass
class Parser:
    def invoke(self, model_output: dict[str, Any]) -> dict[str, Any]:
        # Real LangChain: structured output parsers / Pydantic validation.
        if model_output.get("type") not in {"tool_call", "final"}:
            raise ValueError("invalid model output")
        print(f"[parsed] {model_output}")
        return model_output


HIGH_IMPACT_TOOLS = {"issue_refund"}


def hitl_gate(tool_name: str, args: dict[str, Any], approve: Callable[[], bool]) -> bool:
    if tool_name not in HIGH_IMPACT_TOOLS:
        return True
    print(f"[hitl] high-impact tool={tool_name} args={args} requires approval")
    return approve()


def run_mock() -> str:
    chain = [Prompt("You are a support agent. User request: {request}"), MockChatModel(), Parser()]
    value: Any = {"request": "Please issue a refund for invoice #42"}
    for step in chain:
        value = step.invoke(value)

    if value["type"] == "final":
        return value["text"]

    approved = hitl_gate(value["name"], value["args"], approve=lambda: False)  # mock decision
    if not approved:
        return f"HELD FOR APPROVAL: {value['name']}({value['args']})"
    return f"EXECUTED: {value['name']}({value['args']})"


def run_langchain_real_note() -> str:
    # Real LangChain path (APIs evolve; verify current docs). Conceptual example:
    #
    # from langchain_core.prompts import ChatPromptTemplate
    # from langchain_openai import ChatOpenAI
    # from langchain_core.tools import tool
    #
    # @tool
    # def issue_refund(invoice_id: str, amount: float) -> str:
    #     \"\"\"Issue a refund (high impact - gate behind HITL in production).\"\"\"
    #     return f"refunded {invoice_id} {amount}"
    #
    # prompt = ChatPromptTemplate.from_messages([("system","..."), ("human","{request}")])
    # model = ChatOpenAI(model="gpt-4.1-mini").bind_tools([issue_refund])
    # chain = prompt | model  # LCEL
    # result = chain.invoke({"request": "..."})
    # if result.tool_calls: ... validate args, HITL gate, execute tool, continue ...
    #
    # For robust loops, state, checkpoints, or HITL interrupts, prefer LangGraph.
    return "LangChain installed: see run_langchain_real_note() / README for the real LCEL+tools pattern."


if __name__ == "__main__":
    try:
        import langchain_core  # noqa: F401

        print(f"[mode] {run_langchain_real_note()}\n")
    except Exception:
        print("[mode] mock fallback (LangChain packages not installed)\n")
    print(f"[result] {run_mock()}")
