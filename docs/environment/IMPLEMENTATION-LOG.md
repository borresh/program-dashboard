# Implementation Log

## Current state
- **Last completed phase:** Phase 3 (completed)
- **Last updated:** 2026-09-09
- **Blocked on:** (nothing)
- **Next phase:** Phase 4 (sbx network policy + credentials)

## Phase 1 — Prereq verification + repo seeding
- **Status:** completed
- **Started:** 2026-09-09
- **Completed:** 2026-09-09
- **What was done:**
  - Renamed `master` → `main` (branch + HEAD ref)
  - Created GitHub repo `borresh/program-dashboard` via `gh repo create`
  - Added `origin` remote (`git@github.com:borresh/program-dashboard.git`)
  - Pushed `main` to GitHub
  - Created `agent/a` and `agent/b` branches locally
  - Installed `gh` CLI to `~/.local/bin/gh` (v2.73.0)
  - Authenticated `gh` as `borresh` (SSH protocol, full repo scope)
- **Verification results:**
  - `git remote -v` → `origin` → GitHub ✓
  - `git branch` → `main`, `agent/a`, `agent/b` ✓
  - `gh auth status` → authenticated as `borresh` ✓
  - GitHub repo accessible at `github.com/borresh/program-dashboard` ✓
- **Issues encountered:**
  - `gh repo create` with `--source` and `--push` failed due to path expansion issue — used absolute path
  - First attempt returned HTTP 503 but repo was created anyway (GitHub GraphQL race)
  - `powershell.exe` needs full sbx path (not on Windows PATH)
- **Plan changes:**
  - Updated PLAN.md Section 3: added WSL-to-Windows bridging, gh CLI, SSH tools rows
  - Updated PLAN.md Section 6 Phase 1: added gh auth, repo creation, SSH verify as explicit pre-requisite steps
  - Updated PLAN.md Section 8: marked 4 items as [RESOLVED], added details

## Phase 2 — Committed infra additions
- **Status:** completed
- **Started:** 2026-09-09
- **Completed:** 2026-09-09
- **What was done:**
  - Created `.sbx/agent-a.md` — task brief for Agent A
  - Created `.sbx/agent-b.md` — task brief for Agent B
  - Created `.sbx/README.md` — explains task brief system
  - Created `docs/environment/` with 15 stub files:
    - architecture.md, prerequisites.md, directory-layout.md, git-workflow.md
    - personal-stack.md, sandbox-lifecycle.md, sandbox-compose.md, database.md
    - secrets.md, ports.md, network-policy.md, backup-recovery.md
    - cleanup.md, troubleshooting.md, known-limitations.md
  - Created `docs/environment/IMPLEMENTATION-LOG.md` (this file)
  - Committed to `main` as 9521a62, pushed to `origin`
- **Verification results:**
  - All files created and readable ✓
  - Directory structure matches PLAN.md Section 2 spec ✓
  - `git log` shows commit 9521a62 "Add sandbox task briefs and environment documentation" ✓
  - Pushed successfully to `origin/main` ✓
- **Issues encountered:**
  - `gh repo create` with `--source` returned HTTP 503 but created the repo anyway (GitHub GraphQL race) — retried and got "Name already exists", confirming creation
  - Remote wasn't auto-added by `gh repo create` — added manually
- **Plan changes:**
  - (none)

## Phase 3 — Personal Docker Desktop stack
- **Status:** completed
- **Started:** 2026-09-09
- **Completed:** 2026-09-09
- **What was done:**
  - Verified ports 8080/4200/5432 are free
  - Verified Docker Desktop running (Name: docker-desktop)
  - Created `.env` from `.env.example`
  - Installed Maven 3.9.9 to `~/.local/apache-maven-3.9.9` (linked to `~/.local/bin/mvn`) — no Maven wrapper in project
  - Used `JAVA_HOME=/home/borre/.jdks/openjdk-26.0.2.1` (JDK 26, backward-compatible with project's Java 25 target)
  - Ran `./up.sh` — backend build, tests (44 unit + 4 IT), OpenAPI export, client generation, compose up
  - Verified stack: 3 containers healthy, health endpoint `{"status":"UP"}`
  - Ran `./e2e.sh` — 5 Playwright tests passed
  - Restored normal compose config with `./up.sh` after e2e
  - Fixed `frontend/package-lock.json` and `e2e/package-lock.json` — regenerated with public npm registry (were pointing at corporate Artifactory)
- **Verification results:**
  - `docker compose ps` → postgres/backend/frontend all healthy ✓
  - `curl -fsS http://localhost:8080/actuator/health` → `{"groups":["liveness","readiness"],"status":"UP"}` ✓
  - `./e2e.sh` → 5/5 tests passed ✓
  - `docker info | grep Name` → `docker-desktop` ✓
- **Issues encountered:**
  - **npm corporate registry leak**: `frontend/package-lock.json` and `e2e/package-lock.json` contained `resolved` URLs pointing to `artifactory.statnett.no` (a corporate Artifactory). This caused `npm ci` to fail with ENETUNREACH during Docker build. Fixed by deleting both lockfiles and regenerating with `npm install --registry=https://registry.npmjs.org/`. All URLs now point to `registry.npmjs.org`.
  - **No Maven installed on host**: `up.sh` failed at `mvn: not found`. Downloaded and installed Maven 3.9.9 to `~/.local/apache-maven-3.9.9`.
  - **No JDK 25 on host**: `up.sh` couldn't find a JDK 25+ because it only searches `/usr/lib/jvm/*`, sdkman, etc. Set `JAVA_HOME=/home/borre/.jdks/openjdk-26.0.2.1` manually. Could update `up.sh` search paths later.
- **Plan changes:**
  - (none)
