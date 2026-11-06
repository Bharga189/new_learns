# AutoGen (framework mapping)

## Description

**AutoGen** (Microsoft ecosystem) is a framework for building applications with multiple **conversable agents** that exchange messages. It supports patterns like two-agent chats, **group chat** with a manager/selector, tool/function calling, code execution workflows (with safeguards), and human proxy agents. It maps well to multi-agent collaboration patterns where agents interact through messages.

## Pattern mapping (this repo → AutoGen)

| Repo pattern | AutoGen concept |
|---|---|
| Sequential handoff (10) | Chained chats / explicit message handoff between specialized agents |
| Router/supervisor (11) | GroupChat + speaker selection/manager that routes to the right agent |
| Debate/consensus (12) | Multi-agent group discussion with a critic/reviewer and termination/approval rules |
| Tool calling (02) | Agent tool/function registration; tool execution with result messages |
| Human-in-the-loop | `HumanProxyAgent` or approval gate before executing high-impact tool/code |

## Real-time usage cases

- **Collaborative drafting:** researcher + writer + editor agents iterate on a live brief; human approves final publish/send.
- **Data analysis assistant:** planner agent requests queries, analysis agent runs approved code/tools, reviewer checks results; human gates any data export or mutation.
- **IT/helpdesk triage:** classifier routes to specialists; agents gather context and propose actions; human approves password resets, access changes, or customer-facing responses.
- **Software delivery workflow:** coding agent proposes patch, test agent reports results, security reviewer flags risks; human approves merge/release.
- **Meeting/action orchestration:** agents extract decisions/tasks and propose calendar/ticket updates; human confirms before external writes.

## Human-in-the-loop (HITL) for high-impact actions

- Use a **human proxy/approval step** before tools that send messages, change access, modify data, spend money, or affect production.
- Present a structured action summary (tool, args, target, risk, reversibility) and require explicit confirmation.
- For code execution, run in sandboxed environments with allowlists; require approval for network/file/system side effects.
- Set termination conditions and max rounds to prevent runaway group chats.

## Run

```bash
pip install -r 05-frameworks/autogen/requirements.txt
python 05-frameworks/autogen/main.py
```

`main.py` runs a **mock multi-agent conversation** without deps/keys and prints the real AutoGen-style structure. Configure your model provider per current AutoGen docs to run live.

## Notes

AutoGen has evolved across versions/packages; check current Microsoft AutoGen documentation for the exact API (`autogen-agentchat`, `autogen-core`, etc.) and recommended HITL/code-execution safeguards.
