"""Semantic Kernel mapping: plugins/functions, invocation, HITL approval filter.

Run: python 05-frameworks/semantic-kernel/main.py
Mock demo runs without deps/keys.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Callable


HIGH_IMPACT = {"issue_refund", "delete_record", "send_external_message"}


@dataclass
class FunctionCall:
    plugin: str
    name: str
    arguments: dict[str, Any]


def approval_filter(call: FunctionCall, approve: Callable[[FunctionCall], bool]) -> None:
    if call.name in HIGH_IMPACT and not approve(call):
        raise PermissionError(f"HITL approval required for {call.plugin}.{call.name}")


class OpsPlugin:
    def get_order(self, order_id: str) -> dict[str, Any]:
        return {"order_id": order_id, "status": "paid"}

    def issue_refund(self, order_id: str, amount: float) -> dict[str, Any]:
        # Idempotency key should be used in production.
        return {"order_id": order_id, "amount": amount, "status": "refunded"}


def invoke(plugin: OpsPlugin, call: FunctionCall) -> Any:
    def approve(c: FunctionCall) -> bool:
        print(f"[hitl] review {c.plugin}.{c.name} args={c.arguments}")
        return False  # mock: require human approval

    approval_filter(call, approve)
    func = getattr(plugin, call.name)
    return func(**call.arguments)


def run_mock() -> None:
    plugin = OpsPlugin()
    safe = FunctionCall("ops", "get_order", {"order_id": "42"})
    print("[call]", invoke(plugin, safe))
    try:
        risky = FunctionCall("ops", "issue_refund", {"order_id": "42", "amount": 19.99})
        print("[call]", invoke(plugin, risky))
    except PermissionError as exc:
        print(f"[blocked] {exc}")


def run_sk_note() -> None:
    # Real SK structure (APIs may evolve):
    # from semantic_kernel import Kernel
    # kernel = Kernel(); add service + plugin; use function calling / filters.
    print("Semantic Kernel installed-path placeholder: configure Kernel, service, and plugins per current docs.")


if __name__ == "__main__":
    try:
        import semantic_kernel  # noqa: F401

        run_sk_note()
    except Exception:
        print("[mode] mock fallback\n")
        run_mock()
