# CrewAI (framework mapping)

## Description

**CrewAI** is a framework for orchestrating role-playing AI agents as a **crew**. You define **agents** (role, goal, backstory/tools), **tasks** (description, expected output, agent), and a **process** (e.g., sequential or hierarchical). It fits workflows where specialized roles collaborate on a shared objective with clear task outputs.

## Pattern mapping (this repo → CrewAI)

| Repo pattern | CrewAI concept |
|---|---|
| Sequential handoff (10) | Sequential process: task outputs feed downstream tasks/agents |
| Router/supervisor (11) | Hierarchical process with a manager agent delegating to specialists |
| Planning/task decomposition (05) | Task breakdown and ordered/hierarchical execution |
| Tool calling (02) | Agent tools; validate tool args/results in wrappers |
| Human-in-the-loop | Human review task/step before finalizing or executing high-impact actions |

## Real-time usage cases

- **Market/research brief:** researcher gathers evidence → analyst synthesizes → writer drafts brief; editor/human approves publication.
- **Customer onboarding workflow:** data collection agent → compliance/checklist agent → welcome-communication drafter; human approves external communications or account activation.
- **Content operations:** SEO/brief agent → creator agent → QA/compliance agent; human approves publishing.
- **RFP/proposal response:** requirements extractor → solution mapper → pricing/risk reviewer; human approves submission.
- **Operational runbook assistant:** diagnostics agent → remediation planner → risk reviewer; human approves production changes.

## Human-in-the-loop (HITL) for high-impact actions

- Insert a **review/approval task** (or explicit human input step) before actions with external or irreversible effects.
- Treat agent output as a *proposal*: include action, scope, evidence, and risk.
- Require approval for sending emails/messages, modifying records, triggering deployments, or financial operations.
- Keep an audit trail of task outputs and approvals; bound crew iterations and retries.

## Run

```bash
pip install -r 05-frameworks/crewai/requirements.txt
python 05-frameworks/crewai/main.py
```

`main.py` runs a **mock crew** without deps/keys and shows the CrewAI role/task/process mapping. Configure model/provider settings per current CrewAI docs for live runs.

## Notes

CrewAI APIs and package extras change over time; verify current docs for agent/task/crew parameters, tool integration, and HITL hooks.
