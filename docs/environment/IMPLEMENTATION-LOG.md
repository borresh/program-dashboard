# Implementation Log

## Current state
- **Last completed phase:** Phase 7 (Autonomous verification agents — git integration demo)
- **Last updated:** 2026-09-09
- **Blocked on:** (nothing — agent pushes work over HTTPS via credential helper; SSH agent forwarding remains broken/inapplicable)
- **Next phase:** Review `agent/a` + `agent/b` reports, merge if wanted; then real per-agent feature tasks

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

## Phase 4 — sbx network policy + credentials
- **Status:** completed
- **Started:** 2026-09-09
- **Completed:** 2026-09-09
- **What was done:**
  - Initialized sbx global network policy with `balanced` preset
  - Added network allow rules:
    - `opencode.ai:443` (AI service) — new rule
    - `repo.maven.apache.org:443` — already covered by `default-package-managers`
    - `repo1.maven.org:443` — new rule
    - `registry.npmjs.org:443` — already covered by `default-package-managers`
    - `registry-1.docker.io:443` — new rule
    - `auth.docker.io:443` — new rule
    - `production.cloudflare.docker.com:443` — already covered by `default-code-and-containers`
    - `github.com:443` — already covered by `default-code-and-containers`
    - `ssh.github.com:443` — new rule
    - `api.github.com:443` — new rule
  - Verified all network access with `sbx policy check network` commands
- **Verification results:**
  - `sbx policy ls` → 199 network rules allowed ✓
  - `sbx policy check network opencode.ai:443` → Allowed ✓
  - `sbx policy check network registry.npmjs.org:443` → Allowed ✓
  - `sbx policy check network github.com:443` → Allowed ✓
  - `sbx policy check network registry-1.docker.io:443` → Allowed ✓
- **Issues encountered:**
  - (none)
- **Plan changes:**
  - Decided to leave `.env` as-is (agents create their own from `.env.example` inside sandbox) — matches credential isolation design

## Phase 5 — Create + validate sandboxes
- **Status:** completed
- **Started:** 2026-09-09
- **Completed:** 2026-09-09
- **What was done:**
  - Upgraded sbx v0.39.0 → v0.42.1 (MSI from GitHub release, verified via `sbx version`)
  - Added network allow rule: `github.com:22` (id `2d2db5b2`)
  - Enabled Windows ssh-agent service (Running/Automatic), loaded registered key
  - Created both sandboxes: `pd-agent-a`, `pd-agent-b` (opencode, 4 CPUs, 8 GB RAM)
  - Installed Maven 3.9.12 via apt in both VMs
  - Copied shared opencode config (opencode.json + AGENTS.md + .opencode/) into both VMs
  - P4 validation: up.sh ✓, mvn verify ✓ (4/4 tests), e2e ✓ (5/5 Playwright) — both VMs
  - P5 isolation: git branch isolation ✓, no API key in .env ✓
  - Set up `sbx secret set github` (stored GitHub PAT for future sandboxes)
  - docker compose down in both VMs (clean state for agents)
- **Verification results:**
  - `sbx ls` → both sandboxes running ✓
  - Private daemon: `Name: pd-agent-a` / `pd-agent-b` ✓
  - JDK 25.0.4 at `/usr/bin/java` (matches up.sh globs) ✓
  - Clone workspace: 49 GB volume at guest-mirrored UNC path ✓
  - Git daemon serves clone (not RO source) at port 9418 ✓
  - Task briefs readable from `/run/sandbox/source/.sbx/` ✓
  - Peer remotes wired (sandbox-pd-agent-b inside pd-agent-a) ✓
  - `.env` contains 0 `sk-` secrets ✓ (OPENCODE_API_KEY via sbx `-e` only)
  - Both VMs: up.sh + mvn verify + e2e all pass ✓
- **Issues encountered:**
  - **SSH agent forwarding broken on Windows**: The per-sandbox forwarder
    (Linux container at `gateway.docker.internal:3129`) resets connections.
    Cannot reach `\\.\pipe\openssh-ssh-agent` named pipe. Workaround:
    `sbx secret set github --command 'gh auth token'` + per-session
    `-e GITHUB_TOKEN` injection. Secrets only inject at sandbox creation.
  - **Clone path**: The writable clone lives at the guest-mirrored UNC path
    (`//wsl.localhost/Ubuntu/home/borre/dev/projects/program-dashboard`),
    not `/home/agent/workspace` (empty template dir). Updated docs.
  - **v0.42.1 MSI upgrade**: winget had no upgrade available; downloaded
    MSI from `https://github.com/docker/sbx-releases/releases/download/v0.42.1/DockerSandboxes.msi`
  - **Windows git "dubious ownership"**: WSL paths triggered safe.directory
    protection; fixed with `git config --global --add safe.directory`
- **Plan changes:**
  - Updated `docs/environment/sandbox-lifecycle.md`: corrected clone path,
    added SSH limitation note, added secret injection docs, added known
    limitations section
  - GitHub push auth: use `sbx secret set github` + `-e GITHUB_TOKEN`
    per-session injection (option A from plan)

## Phase 7 — Autonomous verification agents (git integration demo)
- **Status:** completed
- **Started:** 2026-09-09
- **Completed:** 2026-09-09
- **What was done:**
  - **Plan change:** user chose to SKIP Phase 6 (reset/disposability
    validation on existing sandboxes) and start agents directly.
  - **Task:** user chose "verification-only demo" — each agent runs the full
    verification suite, writes a per-agent report, commits it on its lane
    (`agent/<x>`), and pushes to GitHub from inside the sandbox.
  - Prepared push auth in both VMs (SSH forwarding is broken; everything is HTTPS):
    - Stored `gh auth token` (41 bytes) as `chmod 600 /home/agent/.github-token`
    - Global credential helper: `username=x-access-token` + password read from
      the token file (global `~/.gitconfig` persists across sessions)
    - Set git identity `pd-agent-a` / `pd-agent-b` in each clone
    - Tested push auth with a throwaway branch (`agent/__authtest__`), then deleted it
  - Enabled headless autonomy: patched the shared opencode.json in both VMs
    (`build` agent: `git*commit*`/`git*push*` `ask`→`allow`, plus
    `external_directory` allow for the workspace paths), because `opencode run`
    auto-rejects any tool call whose permission is not pre-approved.
  - Ran both agents headlessly: `sbx exec <name> -- bash -s < launch script>`
    with `opencode run` in the foreground (foreground keeps the exec session
    alive; a `nohup` background process would be killed by the 30 s auto-stop).
  - Both agents ran the suite green, wrote reports, committed, and pushed.
- **Verification results:**
  - `origin/agent/a` → `3ea6930` "verification: agent-a full suite green at fe8863f" ✓
  - `origin/agent/b` → `0dc2146` "verification: agent-b full suite green at fe8863f" ✓
  - `git ls-remote` on host shows `agent/a`, `agent/b`, `main` ✓
  - Agent-a suite: up.sh ✓, mvn verify ✓ (44 unit + 4 IT), npm ci ✓, lint ✓,
    build ✓, e2e ✓ (5/5), restore up.sh ✓ — all green in-sandbox
  - Agent-b suite: same, all green (self-resolved the Node version issue; see below)
- **Issues encountered:**
  - **Headless permission auto-reject**: a bare `opencode run` cannot grant
    permissions interactively, so anything not pre-approved is auto-rejected.
    This first blocked `git commit` on agent-a. Fixed in the shared config
    source (`~/dev/sbx_opencode_configuration/opencode.json`) and resynced to
    both VMs so future sandboxes inherit the fixes at creation time.
  - **origin re-provisioned to SSH every session**: repo-local
    `git remote set-url origin https://…` did NOT persist across sandbox
    sessions. Workaround baked into the launch script: `git remote set-url`
    runs before every `opencode run`. Global `~/.gitconfig` (credential helper,
    identity) DID persist. Note: `url.<base>.insteadOf` does not match the
    scp-style `git@github.com:` URL (only the canonical ssh:// form), so the
    rewrite approach was abandoned in favour of set-url.
  - **Node.js too old for Angular 22**: VM default is `v22.22.1` but Angular CLI
    needs `≥ v22.22.3`; `lint`/`build` fail with a clear message. Agent-b
    self-resolved by fetching the `v22.22.3` tarball (`.tar.gz`; no xz in the
    sandbox) to `/tmp/node-v22.22.3`, prepending to `PATH`. Documented so
    future runs bootstrap it up-front.
  - Agent-a's second run inherited a stale staged report from run one; the
    commit message references `fe8863f` (the clone's `origin/main` baseline at
    clone time). Cosmetic for the demo.
- **Plan changes:**
  - Phase 6 reset/disposability skipped per user decision (existing sandboxes
    reused)
  - Updated `~/dev/sbx_opencode_configuration/opencode.json` (shared source) for
    headless-safe permissions
  - Updated `docs/environment/sandbox-lifecycle.md` with the working headless
    run pattern and push auth
  - Updated `docs/environment/known-limitations.md` (sbx v0.42.1, Node floor,
    origin re-provisioning, permission auto-reject)
