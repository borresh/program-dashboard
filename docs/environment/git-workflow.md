# Git Workflow

## Branches

| Branch | Owner | Purpose |
|--------|-------|---------|
| `main` | Human | Integration, review, production-ready |
| `agent/a` | Agent A | Work branch inside sandbox A |
| `agent/b` | Agent B | Work branch inside sandbox B |

## Remotes

| Remote | URL | Purpose |
|--------|-----|---------|
| `origin` | `git@github.com:borresh/program-dashboard.git` | Durable authoritative source |
| `sandbox-pd-agent-a` | Auto-created by sbx | Fetch agent A's work from VM |
| `sandbox-pd-agent-b` | Auto-created by sbx | Fetch agent B's work from VM |

## Integration flow

1. Agent commits on `agent/<x>` inside sandbox
2. Agent pushes to `origin` (GitHub) via SSH forwarding
3. Human fetches, reviews, merges into `main`

```sh
git fetch origin
git diff main..origin/agent/a
git merge --no-ff origin/agent/a
git push origin main
```

## Fallback (sandbox-remote)

If agent didn't push to GitHub:
```sh
git fetch sandbox-pd-agent-a
git diff main..sandbox-pd-agent-a/agent/a
git merge --no-ff sandbox-pd-agent-a/agent/a
git push origin main
```
