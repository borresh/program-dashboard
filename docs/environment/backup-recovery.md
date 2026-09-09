# Backup & Recovery

## What's durable

- **GitHub** (`origin`) — the authoritative source after pushes
- **Host worktree** — can be restored from `origin/main`
- **`~/dev/sbx_opencode_configuration/`** — unversioned, outside any repo

## What's ephemeral

- Sandboxes (`sbx rm` destroys everything)
- In-sandbox clones, databases, build artifacts
- Agent `.env` files (recreated from `.env.example`)

## Recovery scenarios

| Scenario | Recovery |
|----------|----------|
| Sandbox corrupted | `sbx rm` + recreate from `main` |
| Host worktree lost | `git clone git@github.com:borresh/program-dashboard.git` |
| Agent work not pushed | `git fetch sandbox-pd-agent-a` (if VM still running) |
| Complete reset | Remove all sandboxes, re-clone from GitHub |

## No backup needed for

- Sandbox state (disposable by design)
- Docker images (rebuildable)
- Dependencies (reinstallable)
