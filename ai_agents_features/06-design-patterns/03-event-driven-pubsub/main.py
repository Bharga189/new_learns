"""Event-driven pub/sub: agents communicate via topic events.

Run: python 06-design-patterns/03-event-driven-pubsub/main.py
"""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Callable


@dataclass
class Event:
    topic: str
    type: str
    payload: dict
    id: str
    ts: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class EventBus:
    def __init__(self) -> None:
        self._subs: dict[str, list[Callable[[Event], None]]] = defaultdict(list)

    def subscribe(self, topic: str, handler: Callable[[Event], None]) -> None:
        self._subs[topic].append(handler)
        print(f"[bus] subscribed handler to '{topic}'")

    def publish(self, event: Event) -> None:
        print(f"[bus] publish {event.topic}/{event.type} id={event.id}")
        for handler in self._subs.get(event.topic, []):
            handler(event)  # sync demo; real systems use queues/async delivery


def make_triage_agent(bus: EventBus) -> None:
    seen: set[str] = set()

    def on_alert(event: Event) -> None:
        if event.id in seen:  # idempotent handler
            return
        seen.add(event.id)
        severity = event.payload.get("severity")
        action = "page_oncall" if severity == "high" else "create_ticket"
        bus.publish(Event("actions", "action.proposed", {"action": action, "source": event.id}, id=f"act-{event.id}"))

    bus.subscribe("alerts", on_alert)


def make_audit_agent(bus: EventBus) -> None:
    def on_any_action(event: Event) -> None:
        print(f"[audit] recorded {event.type}: {event.payload}")

    bus.subscribe("actions", on_any_action)


if __name__ == "__main__":
    bus = EventBus()
    make_triage_agent(bus)
    make_audit_agent(bus)
    bus.publish(Event("alerts", "alert.raised", {"service": "api", "severity": "high"}, id="e1"))
    bus.publish(Event("alerts", "alert.raised", {"service": "api", "severity": "high"}, id="e1"))  # duplicate
