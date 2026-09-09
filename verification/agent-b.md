# Agent B — Verification Report

- **Host identity**: pd-agent-b
- **Date (UTC)**: 2026-09-09 19:37 UTC
- **Branch**: agent/b, created from origin/main
- **HEAD commit before verification**: `fe8863ff845c101ee0568e971deebeb62a75e94c`

## Toolchain

| Tool | Version used |
|---|---|
| JDK | OpenJDK 25.0.4 (`/usr/lib/jvm/java-25-openjdk-amd64`) |
| Maven | Apache Maven 3.9.12 |
| Node | v22.22.3 (see note below) |
| npm | 9.2.0 |

Note on Node: the sandbox default is v22.22.1, but Angular CLI 22.1.6 requires
`^22.22.3 || ^24.15.0 || >=26.0.0`. A fresh Node v22.22.3 binary was staged in
`/tmp/node-v22.22.3` (not in the repo) and used for the frontend steps. No code
change was involved.

## Checks

| # | Check | Result |
|---|---|---|
| 1 | `./up.sh` | **PASS** — `mvn verify` inside (see #2), Docker images built, postgres/backend/frontend all healthy |
| 2 | `mvn -f backend/pom.xml verify` | **PASS** — unit tests 44, failures 0, errors 0; IT tests 4 (incl. `SuccessCheckIT`), failures 0; Checkstyle 0 violations; OpenAPI spec exported; Angular client generated into `frontend/src/app/api`; BUILD SUCCESS (exit 0) |
| 3 | `npm --prefix frontend ci` | **PASS** (after one retry) — 519 packages added; first attempt aborted with `ECONNRESET` (transient network), retry succeeded |
| 4 | `npm --prefix frontend run lint` | **PASS** — "All files pass linting." (exit 0) with Node v22.22.3; fails under default v22.22.1 because the Angular CLI refuses to start |
| 5 | `npm --prefix frontend run build` | **PASS** — application bundle generation complete (main + 3 lazy chunks, 358.78 kB initial total), output at `frontend/dist/frontend` (exit 0) |
| 6 | `./e2e.sh` | **PASS** — 5 Playwright browser checks passed in 2.5s (overview, detail, activity timeline, abandoned toggle, blocking-question round trip) |
| 7 | `./up.sh` (restore normal origin config) | **PASS** — stack restarted under the normal compose origin config; all containers healthy |

## Environment notes

- No `.env`/`.sbx/`/`docs/`/`.gitignore` files were touched, and no source code
  was changed. The only artefact besides this report is the git-ignored,
  regenerated API client under `frontend/src/app/api`.
- `mvn verify` needs Docker (Testcontainers); the `*IT` tests ran against a real
  PostgreSQL 16.`./up.sh` and `./e2e.sh` detect the dev-container case and set
  `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` as needed.
- Second `./up.sh` run re-ran `mvn verify` internally as part of the restore;
  that pass also succeeded.

## Verdict

This was a **verification-only run**. No feature or code changes were made.
The full build and test pipeline is **green**: backend unit + integration tests,
Checkstyle, OpenAPI export and client generation, frontend dependency install,
lint, production build, and browser checks all passed.