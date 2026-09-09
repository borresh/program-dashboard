# Agent A — Task Brief

You are Agent A working on the `program-dashboard` project inside an isolated Docker Sandbox.

## Scope

- Work **only** in this clone (the in-sandbox workspace at `.`).
- Your branch is `agent/a`. Commit all work there.
- Never touch `main`, `agent/b`, or any other branch.
- Never modify `.sbx/` or `docs/` directories.

## Setup

1. Create `.env` from `.env.example` with safe dev defaults (no real secrets):
   ```sh
   cp .env.example .env
   # Edit .env: set POSTGRES_PASSWORD=change-me (or any safe value)
   # All other defaults are fine for development
   ```
2. The OpenCode API key is injected via environment variable — never store it in `.env`.

## Build and verify

Run these in order. All must pass before you consider your work complete:

```sh
./up.sh                                    # builds backend, brings up stack, health checks
mvn -f backend/pom.xml verify              # tests, Checkstyle, OpenAPI export, client generation
npm --prefix frontend ci                   # install frontend deps
npm --prefix frontend run lint             # lint frontend
npm --prefix frontend run build            # build frontend
./e2e.sh                                   # Playwright browser checks against running containers
```

## Git workflow

When your work is complete and all checks pass:

```sh
git add -A
git commit -m "描述你的改动"
git push -u origin agent/a
```

Push to `origin` (GitHub). The human will integrate your work into `main`.

## Important

- Read `AGENTS.md` for project-specific rules (non-negotiable, anti-patterns, testing).
- Read `AGENT-API.md` if you need to interact with the Program Dashboard REST API.
- The stack is Java 25 / Spring Boot 4.1 / PostgreSQL 16 / Angular 22.
- `./up.sh` finds JDK 25+ automatically; if it fails, install via `sdk install java 25-tem`.
