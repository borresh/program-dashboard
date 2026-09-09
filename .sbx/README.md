# .sbx/ — Sandbox Task Briefs

This directory contains task briefs for agents working in Docker Sandboxes.

## How it works

- Each agent (`agent-a.md`, `agent-b.md`) gets a copy of this project via `sbx create --clone`.
- The in-sandbox clone is mounted read-only at `/run/sandbox/source` — agents can read these briefs from there.
- The agents' opencode configuration (`AGENTS.md`, `opencode.json`, `.opencode/`) is copied into each agent's home at sandbox setup, pre-configured with `opencode/big-pickle`. That config is shared across programs and lives in the agent home, not in this directory.
- These briefs hold only this program's task instructions — they complement the shared opencode config.

## Files

| File | Purpose |
|------|---------|
| `agent-a.md` | Task brief for Agent A (branch `agent/a`) |
| `agent-b.md` | Task brief for Agent B (branch `agent/b`) |

## Rules

- Agents must not modify files in `.sbx/` or `docs/`.
- Agents work only on their designated branch.
- Agents push to `origin` (GitHub) when work is complete.
