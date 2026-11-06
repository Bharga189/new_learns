"""Lightweight tracing hooks. Replace with OpenTelemetry/LangSmith in production."""

from __future__ import annotations

import time
import uuid
from contextlib import contextmanager
from dataclasses import dataclass, field
from typing import Any, Iterator

TRACE_LOG: list[dict[str, Any]] = []


@dataclass
class Span:
    name: str
    trace_id: str = field(default_factory=lambda: uuid.uuid4().hex)
    span_id: str = field(default_factory=lambda: uuid.uuid4().hex[:16])
    attributes: dict[str, Any] = field(default_factory=dict)
    start_s: float = field(default_factory=time.time)
    error: str | None = None

    def set(self, **attrs: Any) -> None:
        # Redact secrets before setting attributes in real systems.
        self.attributes.update(attrs)

    def end(self) -> dict[str, Any]:
        event = {
            "name": self.name,
            "trace_id": self.trace_id,
            "span_id": self.span_id,
            "duration_ms": round((time.time() - self.start_s) * 1000, 2),
            "attributes": self.attributes,
            "error": self.error,
        }
        TRACE_LOG.append(event)
        return event


@contextmanager
def trace_span(name: str, **attrs: Any) -> Iterator[Span]:
    span = Span(name=name)
    span.set(**attrs)
    try:
        yield span
    except Exception as exc:  # record, then re-raise
        span.error = f"{type(exc).__name__}: {exc}"
        raise
    finally:
        span.end()
