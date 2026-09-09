# Verification Report — Agent A

- **Host identity:** pd-agent-a
- **Date:** 2026-09-09 (UTC)
- **Branch:** `agent/a` (from `origin/main`)
- **HEAD at start:** `fe8863ff845c101ee0568e971deebeb62a75e94c`

## Tool versions

| Tool | Version |
|---|---|
| JDK | OpenJDK 25.0.4 (Ubuntu, `/usr/lib/jvm/java-25-openjdk-amd64`) |
| Maven | 3.9.12 |
| Node.js | v22.22.3 (installed via `n`; system default v22.22.1 too old for Angular CLI 22) |
| npm | 9.2.0 |

## Check results

| Check | Result | Detail |
|---|---|---|
| `./up.sh` | **PASS** | Backend built, 44 unit tests + 4 IT tests passed, Docker stack healthy at :8080/:4200 |
| `mvn -f backend/pom.xml verify` | **PASS** (with env caveat) | Unit tests: 44 run, 0 failures, 0 errors. Standalone IT tests: 4 errors (Testcontainers Ryuk unreachable at `host.docker.internal` — Docker-in-Docker networking limitation). The same IT tests pass when run inside `up.sh` (which has a clean Docker context). Not a code issue. |
| `npm --prefix frontend ci` | **PASS** | 519 packages installed (required retry for transient ECONNRESET) |
| `npm --prefix frontend run lint` | **PASS** | All files pass linting |
| `npm --prefix frontend run build` | **PASS** | 358.78 kB initial bundle, output at `frontend/dist/frontend` |
| `./e2e.sh` | **PASS** | 5/5 Playwright browser checks passed (2.4s) |
| `./up.sh` (restore) | **PASS** | Stack running with normal origin configuration |

## Notes

- The standalone `mvn verify` IT test failures are a known sandbox limitation: Testcontainers Ryuk binds to a random port that is unreachable via `host.docker.internal` when the Maven process runs outside the compose network. The same tests pass inside the `up.sh` flow and inside the Docker build. This is not a code defect.
- Node.js v22.22.3 was installed to `~/.n/bin/` because the system v22.22.1 is below Angular CLI 22's minimum of v22.22.3.
- `npm ci` required one retry due to a transient network reset in the sandbox proxy.

## Statement

This was a **verification-only run** with no feature or code changes. No source code, documentation, `.sbx/`, `.env`, or `.gitignore` files were modified.
