"""Provider-agnostic LLM adapter with mock, streaming, and tracing hooks.

Default is MockLLMClient (no keys). Set OPENAI_API_KEY to use an OpenAI-compatible
chat completions endpoint. Swap this module for your official provider SDK.
"""

from __future__ import annotations

import json
import os
import urllib.request
from dataclasses import dataclass, field
from typing import Any, Iterable, Iterator

from .tracing import trace_span


@dataclass
class LLMResponse:
    text: str
    raw: Any = None
    tool_calls: list[dict[str, Any]] = field(default_factory=list)


@dataclass
class StreamChunk:
    delta: str
    done: bool = False
    raw: Any = None


class LLMClient:
    def complete(self, messages: list[dict[str, Any]], **kwargs: Any) -> LLMResponse:
        raise NotImplementedError

    def stream(self, messages: list[dict[str, Any]], **kwargs: Any) -> Iterator[StreamChunk]:
        # Fallback: providers that don't stream can yield one final chunk.
        resp = self.complete(messages, **kwargs)
        yield StreamChunk(delta=resp.text, done=True, raw=resp.raw)


class MockLLMClient(LLMClient):
    """Deterministic mock for local runs/tests; no network, no keys."""

    def __init__(self, scripted: Iterable[str] | None = None) -> None:
        self._scripted = list(scripted or ["(mock response)"])
        self._i = 0

    def complete(self, messages: list[dict[str, Any]], **kwargs: Any) -> LLMResponse:
        with trace_span("llm.complete", provider="mock", message_count=len(messages)) as span:
            text = self._scripted[min(self._i, len(self._scripted) - 1)]
            self._i += 1
            span.set(output_chars=len(text))
            return LLMResponse(text=text)

    def stream(self, messages: list[dict[str, Any]], **kwargs: Any) -> Iterator[StreamChunk]:
        resp = self.complete(messages, **kwargs)
        # Simulate token streaming.
        for part in resp.text.split(" "):
            yield StreamChunk(delta=part + " ")
        yield StreamChunk(delta="", done=True)


class OpenAICompatibleClient(LLMClient):
    """Minimal OpenAI-compatible chat client using stdlib HTTP.

    Production: prefer the official SDK, retries, timeouts, and secret redaction.
    """

    def __init__(
        self,
        api_key: str,
        base_url: str = "https://api.openai.com/v1",
        model: str = "gpt-4.1-mini",
        timeout_s: float = 60.0,
    ) -> None:
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.timeout_s = timeout_s

    def _post(self, payload: dict[str, Any]) -> dict[str, Any]:
        req = urllib.request.Request(
            f"{self.base_url}/chat/completions",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.api_key}",
            },
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=self.timeout_s) as resp:
            return json.loads(resp.read().decode("utf-8"))

    def complete(self, messages: list[dict[str, Any]], **kwargs: Any) -> LLMResponse:
        with trace_span("llm.complete", provider="openai-compatible", model=self.model) as span:
            data = self._post({"model": self.model, "messages": messages, **kwargs})
            choice = (data.get("choices") or [{}])[0]
            msg = choice.get("message") or {}
            text = msg.get("content") or ""
            span.set(output_chars=len(text), usage=data.get("usage"))
            return LLMResponse(text=text, raw=data, tool_calls=msg.get("tool_calls") or [])

    def stream(self, messages: list[dict[str, Any]], **kwargs: Any) -> Iterator[StreamChunk]:
        # Full SSE streaming requires incremental parsing; shown here as a clear hook.
        # Implement SSE parsing or use the official SDK for production streaming.
        with trace_span("llm.stream", provider="openai-compatible", model=self.model):
            resp = self.complete(messages, **kwargs)
            yield StreamChunk(delta=resp.text, done=True, raw=resp.raw)


def from_env() -> LLMClient:
    key = os.environ.get("OPENAI_API_KEY")
    if not key:
        return MockLLMClient()
    return OpenAICompatibleClient(
        api_key=key,
        base_url=os.environ.get("OPENAI_BASE_URL", "https://api.openai.com/v1"),
        model=os.environ.get("OPENAI_MODEL", "gpt-4.1-mini"),
    )
