"""Mediator: peers communicate only through a central router, never directly.

Run: python 06-design-patterns/08-mediator/main.py
"""

from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Callable


@dataclass
class Message:
    sender: str
    to: str                      # peer name, or "*" for broadcast
    type: str
    payload: dict[str, Any] = field(default_factory=dict)
    id: str = field(default_factory=lambda: f"msg_{uuid.uuid4().hex[:6]}")
    reply_to: str | None = None
    correlation_id: str | None = None


class Mediator:
    """Central router. Owns the address book; peers only know this object."""

    def __init__(self) -> None:
        self._peers: dict[str, Callable[[Message], None]] = {}
        self.delivered = 0
        self.errors = 0

    def register(self, name: str, handler: Callable[[Message], None]) -> None:
        if name in self._peers:
            raise ValueError(f"peer '{name}' already registered")
        self._peers[name] = handler
        print(f"[mediator] registered '{name}' (peers={sorted(self._peers)})")

    def unregister(self, name: str) -> None:
        self._peers.pop(name, None)
        print(f"[mediator] unregistered '{name}'")

    def send(self, msg: Message) -> None:
        if msg.to == "*":
            targets = [n for n in self._peers if n != msg.sender]
            print(f"[mediator] broadcast {msg.type} from {msg.sender} -> {targets}")
            for name in targets:
                self._deliver(name, msg)
            return
        self._deliver(msg.to, msg)

    def _deliver(self, name: str, msg: Message) -> None:
        handler = self._peers.get(name)
        if handler is None:
            self.errors += 1
            print(f"[mediator] ERROR {msg.id}: no such peer '{name}' (from {msg.sender})")
            self._fail_back(msg, f"unknown recipient '{name}'")
            return
        self.delivered += 1
        print(f"[mediator] route {msg.id} {msg.sender}->{name} ({msg.type})")
        handler(msg)

    def _fail_back(self, msg: Message, reason: str) -> None:
        # Never drop failures silently; tell the sender if it registered a reply address.
        if msg.reply_to and msg.reply_to in self._peers:
            self._peers[msg.reply_to](Message(
                sender="mediator", to=msg.reply_to, type="delivery.error",
                payload={"reason": reason, "original_id": msg.id},
                correlation_id=msg.correlation_id,
            ))


class Peer:
    """Base peer. Note: it has no list of other peers — only the mediator."""

    def __init__(self, name: str, mediator: Mediator) -> None:
        self.name = name
        self.mediator = mediator
        mediator.register(name, self.receive)

    def send(self, to: str, type_: str, payload: dict[str, Any],
             correlation_id: str | None = None) -> str:
        msg = Message(sender=self.name, to=to, type=type_, payload=payload,
                      reply_to=self.name, correlation_id=correlation_id)
        self.mediator.send(msg)
        return msg.id

    def receive(self, msg: Message) -> None:
        raise NotImplementedError


class Researcher(Peer):
    def receive(self, msg: Message) -> None:
        if msg.type == "research.request":
            topic = msg.payload["topic"]
            print(f"[{self.name}] researching '{topic}'")
            self.send(msg.sender, "research.result",
                      {"topic": topic, "finding": f"{topic}: retries must be idempotent"},
                      correlation_id=msg.correlation_id or msg.id)
        elif msg.type == "research.result":
            print(f"[{self.name}] got result corr={msg.correlation_id}: {msg.payload['finding']}")
        elif msg.type == "delivery.error":
            print(f"[{self.name}] saw delivery error: {msg.payload['reason']}")
        else:
            print(f"[{self.name}] ignored {msg.type}")


class Writer(Peer):
    def receive(self, msg: Message) -> None:
        if msg.type == "draft.request":
            print(f"[{self.name}] drafting from {len(msg.payload['findings'])} finding(s)")
            self.send(msg.sender, "draft.result",
                      {"draft": "Retries: use idempotency keys and bounded backoff."},
                      correlation_id=msg.correlation_id or msg.id)
        elif msg.type == "delivery.error":
            print(f"[{self.name}] delivery failed: {msg.payload['reason']} "
                  f"(original={msg.payload['original_id']})")
        else:
            print(f"[{self.name}] ignored {msg.type}")


class Editor(Peer):
    def __init__(self, name: str, mediator: Mediator) -> None:
        super().__init__(name, mediator)
        self.received: list[Message] = []

    def receive(self, msg: Message) -> None:
        self.received.append(msg)
        print(f"[{self.name}] received {msg.type} from {msg.sender}: {msg.payload}")


if __name__ == "__main__":
    bus = Mediator()
    r1, r2 = Researcher("researcher-1", bus), Researcher("researcher-2", bus)
    writer = Writer("writer", bus)

    print("\n=== 1. point-to-point with correlated reply ===")
    corr = "job-42"
    r1.send("researcher-2", "research.request", {"topic": "retry policy"}, correlation_id=corr)

    print("\n=== 2. error path: unknown recipient ===")
    writer.send("publisher", "draft.request", {"findings": []})

    print("\n=== 3. new peer joins at runtime, no peer edits ===")
    editor = Editor("editor", bus)
    bus.send(Message(sender="writer", to="*", type="draft.published", payload={"draft": "v1"}))

    print(f"\n[stats] delivered={bus.delivered} errors={bus.errors} editor_saw={len(editor.received)}")
