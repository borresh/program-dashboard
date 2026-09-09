# Known Limitations

## sbx version

- Current: **v0.42.1** (upgraded from v0.39.0 in Phase 5; fixes Windows mount-path issue).
- Upgrade via MSI (winget had no upgrade available):
  `https://github.com/docker/sbx-releases/releases/download/v0.42.1/DockerSandboxes.msi`

## SSH agent forwarding (Windows)

- The per-sandbox forwarder container cannot reach the Windows
  `openssh-ssh-agent` named pipe; the relay resets connections.
- Agents must push over HTTPS with a PAT credential helper. See
  `sandbox-lifecycle.md` → GitHub auth.
- `sbx secret set` only injects into sandboxes created after the secret is stored.

## node.js floor for Angular 22

- Sandbox default Node is `v22.22.1`; Angular CLI 22 requires `≥ v22.22.3`
  (`npm run lint`/`build` exit 3 with a clear message).
- Workaround: fetch `node-v22.22.3-linux-x64.tar.gz` into `/tmp` and prepend
  to `PATH` (no `xz` in the sandbox, so use the `.tar.gz`). See
  `sandbox-lifecycle.md` → Headless autonomous runs.

## Repo-local git config is not durable

- sbx re-provisions each clone's `remote.origin.url` back to the SSH URL on
  every session. Global `~/.gitconfig` persists; clone `.git/config` does not.
- Bake `git remote set-url origin https://…` into the launch script before each
  agent run. `url.<base>.insteadOf` does not match scp-style `git@github.com:`
  URLs.

## Big Pickle model

- `opencode/big-pickle` is free for a limited time.
- Free-period traffic may be used to improve the model (per Zen docs).
- No SLA guarantees during free period.

## JDK mismatch

- Project needs JDK 25. Host has JDK 26 (`~/.jdks/openjdk-26.0.2.1`).
- JDK 26 is backward-compatible — works fine. Sandboxes may have different JDK versions.

## Port collisions

- Personal stack and sandboxes use separate Docker daemons — no port collisions.
- If running personal stack simultaneously with both sandboxes during heavy builds, expect high resource usage (32 CPUs, 31 GiB RAM available).

## `gh` CLI

- Installed to `~/.local/bin/gh` (not on default PATH).
- Requires `export PATH="$HOME/.local/bin:$PATH"` or full path usage.
- Requires `gh auth login` before first use.

## Headless opencode permission auto-reject

- `opencode run` in a non-interactive exec auto-rejects any tool call whose
  permission is not pre-approved (no human to prompt).
- The shared config (`~/dev/sbx_opencode_configuration/opencode.json`) must
  allow `git` commit/push, build commands, and the workspace paths under
  `external_directory`. It is pre-fixed in the source; only per-session
  overrides in a VM's copy can reintroduce it.
