"""02 - Tool Calling (wired end-to-end): shared LLM adapter + trace spans.

Run (deterministic mock, no keys):  python 01-basic/02-tool-calling/main.py
Run (real OpenAI-compatible model): set OPENAI_API_KEY (and optional OPENAI_BASE_URL,
  OPENAI_MODEL) then run the same command. The model receives tool schemas and may
  call `get_weather`; this program dispatches locally and returns the final answer.
"""

from __future__ import annotations

import json
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

# Allow `from shared...` when running this file directly.
ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from shared.llm_adapter import LLMClient, LLMResponse, from_env  # noqa: E402
from shared.tracing import TRACE_LOG, trace_span  # noqa: E402


# --- Tools (the actions the agent is allowed to take) ---

def get_weather(city: str, unit: str = "celsius") -> dict[str, Any]:
    # In production this would call a real API. Keep tools deterministic where possible.
    data = {
        "Paris": {"temp": 18, "condition": "cloudy"},
        "London": {"temp": 15, "condition": "rain"},
    }
    if city not in data:
        return {"ok": False, "error": f"Unknown city: {city}"}
    temp = data[city]["temp"]
    if unit == "fahrenheit":
        temp = temp * 9 / 5 + 32
    return {"ok": True, "city": city, "temp": temp, "unit": unit, **data[city]}


TOOL_REGISTRY: dict[str, Callable[..., dict[str, Any]]] = {"get_weather": get_weather}

# OpenAI-style tool schema for real providers.
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "Get current weather for a supported city.",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string"},
                    "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
                },
                "required": ["city"],
            },
        },
    }
]


def dispatch_tool(name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    with trace_span("tool.dispatch", tool=name) as span:
        # Validate before execution: unknown tools and bad args are explicit errors.
        tool = TOOL_REGISTRY.get(name)
        if tool is None:
            result = {"ok": False, "error": f"Tool not allowed: {name}"}
        else:
            try:
                result = tool(**arguments)
            except TypeError as exc:
                result = {"ok": False, "error": f"Invalid arguments: {exc}"}
        span.set(ok=result.get("ok"))
        return result


def _normalize_tool_calls(resp: LLMResponse) -> list[dict[str, Any]]:
    calls = []
    for tc in resp.tool_calls or []:
        # OpenAI format: {id, type, function:{name, arguments}}
        if "function" in tc:
            fn = tc["function"]
            args = fn.get("arguments", "{}")
            if isinstance(args, str):
                try:
                    args = json.loads(args)
                except json.JSONDecodeError:
                    args = {"_raw": args}
            calls.append({"id": tc.get("id"), "name": fn.get("name"), "arguments": args})
        elif "name" in tc:
            calls.append({"id": tc.get("id"), "name": tc["name"], "arguments": tc.get("arguments", {})})
    return calls


@dataclass
class ScriptedToolClient(LLMClient):
    """Deterministic demo client: tool call, then final answer from tool result."""

    step: int = 0

    def complete(self, messages: list[dict[str, Any]], **kwargs: Any) -> LLMResponse:
        with trace_span("llm.complete", provider="scripted-demo", message_count=len(messages)) as span:
            self.step += 1
            if self.step == 1:
                span.set(kind="tool_call")
                return LLMResponse(
                    text="",
                    tool_calls=[{
                        "id": "call_demo_1",
                        "type": "function",
                        "function": {
                            "name": "get_weather",
                            "arguments": json.dumps({"city": "Paris", "unit": "celsius"}),
                        },
                    }],
                )
            tool_msg = next((m for m in reversed(messages) if m.get("role") == "tool"), None)
            result = json.loads(tool_msg["content"]) if tool_msg else {}
            text = (
                f"In {result.get('city', '?')} it's {result.get('temp', '?')}°"
                f"{str(result.get('unit', 'celsius'))[0].upper()} and {result.get('condition', '?')}."
            )
            span.set(kind="final", output_chars=len(text))
            return LLMResponse(text=text)


def build_client() -> LLMClient:
    # Deterministic demo by default; real provider when an API key is configured.
    if os.environ.get("OPENAI_API_KEY"):
        return from_env()
    return ScriptedToolClient()


def run_agent(user_text: str, max_tool_calls: int = 5) -> str:
    client = build_client()
    messages: list[dict[str, Any]] = [
        {"role": "system", "content": "Use tools when needed. Answer from tool results."},
        {"role": "user", "content": user_text},
    ]
    with trace_span("agent.run", user_text=user_text, client=type(client).__name__) as span:
        for _ in range(max_tool_calls):
            resp = client.complete(messages, tools=TOOLS)
            calls = _normalize_tool_calls(resp)
            if not calls:
                span.set(outcome="final")
                return resp.text

            for call in calls:
                name, args = call["name"], call["arguments"]
                print(f"[tool_call] {name}({json.dumps(args)})")
                result = dispatch_tool(name, args)
                print(f"[tool_result] {json.dumps(result)}")
                messages.append({
                    "role": "assistant",
                    "content": resp.text or None,
                    "tool_calls": [{"id": call.get("id"), "type": "function",
                                    "function": {"name": name, "arguments": json.dumps(args)}}],
                })
                messages.append({
                    "role": "tool",
                    "tool_call_id": call.get("id"),
                    "name": name,
                    "content": json.dumps(result),
                })
        span.set(outcome="budget_exceeded")
        return "Stopped: exceeded tool-call budget."


if __name__ == "__main__":
    answer = run_agent("What's the weather in Paris?")
    print(f"\n[answer] {answer}\n")
    print("[trace]")
    for e in TRACE_LOG:
        print(f"  - {e['name']} {e['duration_ms']}ms {e['attributes']} err={e['error']}")
