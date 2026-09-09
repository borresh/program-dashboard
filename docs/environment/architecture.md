# Architecture

Two isolated opencode agents run in parallel in Docker Sandboxes on Windows 11 + WSL2.

## Host layers

- **Windows 11** — Docker Desktop, sbx (Docker Sandbox CLI)
- **WSL2 Ubuntu 24.04** — development workspace, git, SSH agent
- **Docker Desktop** — personal dev stack (Postgres + backend + frontend)

## Sandbox layers

Each sandbox (`pd-agent-a`, `pd-agent-b`) is a microVM with:
- Private Docker daemon (20 GB sparse block volume)
- In-sandbox clone of the project (from host repo via `--clone`)
- Compose stack: Postgres 16 + Spring Boot backend + Angular frontend + Playwright e2e
- OpenCode agent (`opencode/big-pickle`) with shared config

## Integration

Git is the only integration mechanism:
- Agents commit on `agent/<x>` inside their sandbox
- Agents push to `origin` (GitHub) via SSH forwarding
- Human integrates into `main` via fetch + merge

## Isolation boundaries

| Boundary | Agent A | Agent B | Host |
|----------|---------|---------|------|
| Working tree | own clone | own clone | main worktree |
| Database | own Postgres | own Postgres | Desktop Postgres |
| Build artifacts | own target/dist | own target/dist | own target/dist |
| Docker images | own daemon | own daemon | Desktop daemon |
