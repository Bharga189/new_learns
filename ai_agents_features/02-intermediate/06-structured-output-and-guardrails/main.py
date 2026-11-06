"""06 - Structured Output & Guardrails: validate, retry, and gate actions.

Run: python 02-intermediate/06-structured-output-and-guardrails/main.py
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any

ALLOWED_ACTIONS = {"create_ticket", "send_summary"}


def validate_action(payload: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if not isinstance(payload, dict):
        return ["payload must be an object"]
    action = payload.get("action")
    if action not in ALLOWED_ACTIONS:
        errors.append(f"action must be one of {sorted(ALLOWED_ACTIONS)}")
    title = payload.get("title")
    if not isinstance(title, str) or not title.strip():
        errors.append("title must be a non-empty string")
    priority = payload.get("priority")
    if priority not in {"low", "medium", "high"}:
        errors.append("priority must be low|medium|high")
    return errors


@dataclass
class MockLLM:
    attempt: int = 0

    def generate(self, instruction: str) -> str:
        self.attempt += 1
        # First attempt returns invalid JSON-ish shape; retry returns valid output.
        if self.attempt == 1:
            return json.dumps({"action": "delete_production_db", "title": ""})
        return json.dumps(
            {"action": "create_ticket", "title": "Investigate login errors", "priority": "high"}
        )


def get_structured_action(user_request: str, max_retries: int = 2) -> dict[str, Any]:
    llm = MockLLM()
    feedback = ""
    for attempt in range(1, max_retries + 2):
        raw = llm.generate(f"Request: {user_request}\n{feedback}")
        print(f"[attempt {attempt}] raw={raw}")
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError as exc:
            feedback = f"Output was not valid JSON: {exc}. Return JSON only."
            continue
        errors = validate_action(payload)
        if not errors:
            return payload
        feedback = "Validation errors: " + "; ".join(errors) + ". Fix and return JSON only."
        print(f"[guardrail] {feedback}")
    raise ValueError("Could not produce a valid, allowed action.")


def execute_action(payload: dict[str, Any]) -> str:
    # Final gate: even validated payloads pass through policy before side effects.
    if payload["action"] not in ALLOWED_ACTIONS:
        raise PermissionError("action blocked by policy")
    return f"executed {payload['action']}: {payload['title']} ({payload['priority']})"


if __name__ == "__main__":
    action = get_structured_action("Create a high priority ticket for login errors.")
    print("[result]", execute_action(action))
