"""Mediator as policy enforcement point: authenticate -> authorize -> redact -> rate-limit -> route.

Run: python 06-design-patterns/08-mediator/policy_demo.py
"""

from __future__ import annotations

import time
from typing import Any, Callable

from main import Mediator, Message, Peer

# --- policy configuration ----------------------------------------------------

SENSITIVE_KEYS = {"ssn", "card_number", "password", "api_key", "token", "secret"}
REDACTED = "[redacted]"

# Who may send what. A type missing here is NOT authorized: it is logged and
# passed (see PolicyMediator.send), so new types can't slip by unnoticed.
ROLE_RULES: dict[str, set[str]] = {
    "research.request": {"researcher"},
    "draft.request": {"writer"},
    "customer.record": {"writer", "backoffice"},
}


def redact(value: Any) -> Any:
    """Return a copy with sensitive keys masked — never mutate the caller's payload."""
    if isinstance(value, dict):
        return {k: (REDACTED if k.lower() in SENSITIVE_KEYS else redact(v)) for k, v in value.items()}
    if isinstance(value, list):
        return [redact(v) for v in value]
    return value


class PolicyMediator(Mediator):
    def __init__(
        self,
        roles: dict[str, str],
        limit: int = 3,
        window_seconds: float = 10.0,
        clock: Callable[[], float] | None = None,
        redaction: bool = True,
        strict: bool = True,
    ) -> None:
        super().__init__()
        self.roles = roles
        self.strict = strict
        self.limit = limit
        self.window = window_seconds
        self.clock = clock or time.monotonic
        self.redaction = redaction
        self._hits: dict[str, list[float]] = {}
        self.audit: list[dict[str, Any]] = []
        self.denied = 0
        self.unknown = 0

    def send(self, msg: Message) -> None:
        role = self._authenticate(msg)
        if role is None:
            return self._deny(msg, "unauthenticated sender", stage="auth")

        allowed_roles = ROLE_RULES.get(msg.type)
        unknown = allowed_roles is None
        if not unknown and role not in allowed_roles:
            return self._deny(msg, f"role '{role}' may not send '{msg.type}'", stage="authz")

        # Rate limit applies to unknown types too, otherwise an unregistered type
        # becomes a rate-limit bypass.
        if not self._rate_limit_ok(msg.sender):
            return self._deny(msg, f"rate limit {self.limit}/{self.window:g}s exceeded", stage="rate")

        if unknown:
            if self.strict:
                # Default-deny: no rule means no permission, not "allowed".
                return self._deny(msg, f"unregistered message type '{msg.type}' (strict mode)", stage="type")
            # Fail-open is opt-in, for rolling out a new type before its rule exists.
            # Even then it is loud and auditable: tolerated, never "authorized".
            self.unknown += 1
            print(f"[mediator] WARN unregistered type '{msg.type}' from {msg.sender}: "
                  f"no ROLE_RULES entry, passing (audited as 'unknown')")

        outbound = msg
        if self.redaction:
            clean = redact(msg.payload)
            if clean != msg.payload:
                print(f"[mediator] redacted {msg.id}: {sorted(SENSITIVE_KEYS & set(_keys(msg.payload)))}")
                # New message: the sender's original payload is left untouched.
                outbound = Message(sender=msg.sender, to=msg.to, type=msg.type, payload=clean,
                                   id=msg.id, reply_to=msg.reply_to, correlation_id=msg.correlation_id)

        if unknown:
            self._audit(msg, "unknown", f"no ROLE_RULES entry for '{msg.type}'; passed by {msg.sender}({role})")
        else:
            self._audit(msg, "allow", f"by {msg.sender}({role})")
        super().send(outbound)

    # --- policy stages -------------------------------------------------------

    def _authenticate(self, msg: Message) -> str | None:
        if msg.sender not in self._peers:
            return None
        return self.roles.get(msg.sender)

    def _rate_limit_ok(self, sender: str) -> bool:
        now = self.clock()
        hits = [t for t in self._hits.get(sender, []) if now - t < self.window]
        if len(hits) >= self.limit:
            self._hits[sender] = hits
            return False
        hits.append(now)
        self._hits[sender] = hits
        return True

    def _deny(self, msg: Message, reason: str, stage: str) -> None:
        self.denied += 1
        self._audit(msg, "deny", f"[{stage}] {reason}")
        print(f"[mediator] DENY {msg.id} {msg.sender}->{msg.to} ({msg.type}): {reason}")
        # Denials are answered, not dropped.
        if msg.reply_to and msg.reply_to in self._peers:
            self._peers[msg.reply_to](Message(
                sender="mediator", to=msg.reply_to, type="policy.denied",
                payload={"reason": reason, "stage": stage, "original_id": msg.id},
                correlation_id=msg.correlation_id,
            ))

    def _audit(self, msg: Message, decision: str, detail: str) -> None:
        self.audit.append({"id": msg.id, "from": msg.sender, "to": msg.to,
                           "type": msg.type, "decision": decision, "detail": detail})


def _keys(obj: Any) -> list[str]:
    out: list[str] = []
    if isinstance(obj, dict):
        for k, v in obj.items():
            out.append(k)
            out.extend(_keys(v))
    elif isinstance(obj, list):
        for v in obj:
            out.extend(_keys(v))
    return out


# --- demo peers --------------------------------------------------------------

class RolePeer(Peer):
    """Peer that records what it actually receives."""

    def __init__(self, name: str, mediator: Mediator) -> None:
        super().__init__(name, mediator)
        self.inbox: list[Message] = []

    def receive(self, msg: Message) -> None:
        self.inbox.append(msg)
        if msg.type in {"policy.denied", "delivery.error"}:
            print(f"[{self.name}] {msg.type}: {msg.payload['reason']}")
        else:
            print(f"[{self.name}] got {msg.type} {msg.payload}")


if __name__ == "__main__":
    CLOCK = [0.0]  # fake clock so rate-limit behavior is deterministic

    roles = {"alice": "researcher", "bob": "writer", "carol": "backoffice", "mallory": "intern"}
    bus = PolicyMediator(roles, limit=3, window_seconds=10.0, clock=lambda: CLOCK[0])
    alice, bob = RolePeer("alice", bus), RolePeer("bob", bus)
    carol, mallory = RolePeer("carol", bus), RolePeer("mallory", bus)

    print("\n=== 1. authorized: allowed and routed ===")
    alice.send("bob", "research.request", {"topic": "retries"})

    print("\n=== 2. redaction: sensitive fields masked in transit ===")
    record = {"customer": "dana", "ssn": "123-45-6789", "card_number": "4111111111111111",
              "notes": "renewal", "nested": {"api_key": "sk-live-abc"}}
    bob.send("carol", "customer.record", record)
    print(f"[sender] original payload unchanged: "
          f"ssn={record['ssn']} card={record['card_number']} nested.api_key={record['nested']['api_key']}")

    print("\n=== 3. authorization: wrong role denied ===")
    mallory.send("carol", "customer.record", {"ssn": "999-99-9999"})

    print("\n=== 4. rate limit: 3 per 10s, then recovery ===")
    for i in range(5):
        print(f"-- attempt {i + 1} at t={CLOCK[0]:.1f}s")
        alice.send("bob", "research.request", {"topic": f"q{i}"})

    CLOCK[0] = 11.0
    print(f"-- after window, attempt at t={CLOCK[0]:.1f}s")
    alice.send("bob", "research.request", {"topic": "q-after-window"})

    print("\n=== 5. unregistered type under strict mode (default): denied ===")
    bob.send("carol", "cache.invalidate", {"scope": "pricing", "api_key": "sk-live-xyz"})
    print(f"[check] unknown_passed={bus.unknown} carol_only_saw_authorized={len(carol.inbox) == 1}")

    print("\n=== 6. unregistered type with strict=False: logged and passed ===")
    lenient = PolicyMediator(roles, limit=3, window_seconds=10.0, clock=lambda: CLOCK[0], strict=False)
    bob_lenient, carol_lenient = RolePeer("bob", lenient), RolePeer("carol", lenient)
    bob_lenient.send("carol", "cache.invalidate", {"scope": "pricing", "api_key": "sk-live-xyz"})
    print(f"[check] unknown={lenient.unknown} passed_to_carol={len(carol_lenient.inbox) == 1}")

    print("\n[audit]")
    for row in bus.audit:
        print(f"  {row['decision']:5} {row['type']:16} {row['from']:7} -> {row['to']:6} {row['detail']}")
    print(f"\n[stats] delivered={bus.delivered} denied={bus.denied} unknown={bus.unknown} "
          f"bob_inbox={len(bob.inbox)} carol_inbox={len(carol.inbox)}")
    print(f"[check] carol never saw an unmasked ssn: "
          f"{all('ssn' not in m.payload or m.payload['ssn'] == REDACTED for m in carol.inbox)}")
