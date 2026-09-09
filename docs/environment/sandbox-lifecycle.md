# Sandbox Lifecycle

## Creation

```sh
powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' create \
  --clone --name pd-agent-a --cpus 4 -m 8g \
  -e OPENCODE_API_KEY \
  opencode '\\wsl.localhost\Ubuntu\home\borre\dev\projects\program-dashboard'"
```

Each sandbox:
- Clones `main` from host repo into private in-VM clone (49 GB volume)
- Host repo mounted read-only at `/run/sandbox/source`
- Forge remotes (`origin` → GitHub) propagated into clone
- Private Docker daemon (20 GB sparse block volume)
- Peer remotes wired (e.g., `sandbox-pd-agent-b` inside `pd-agent-a`)

## Clone workspace layout

The writable clone is NOT at `/home/agent/workspace` (an empty template dir).
It lives at the guest-mirrored UNC path matching the host workspace:

```
/wsl.localhost/Ubuntu/home/borre/dev/projects/program-dashboard
```

This path is the default working directory for `sbx exec` and the agent session.
The git-daemon serves this clone (not the RO source) at port 9418.

## Agent config injection

```sh
tar -C ~/dev/sbx_opencode_configuration -cf - AGENTS.md opencode.json .opencode \
  | powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' \
    exec pd-agent-a -- bash -c 'tar -C ~/.config/opencode -xf -'"
```

## GitHub auth

SSH agent forwarding is broken on this Windows host in sbx v0.42.1
(forwarder Linux container can't reach `\\.\pipe\openssh-ssh-agent`).

Workaround: `sbx secret set github --command 'gh auth token'`
stores a GitHub PAT that is injected into sandboxes at creation time.

For existing sandboxes created before the secret was set, inject per-session:

```sh
sbx run -e GITHUB_TOKEN="$(gh auth token)" --name pd-agent-a opencode
```

Or configure git credential helper inside the VM:

```sh
sbx exec pd-agent-a -- bash -lc \
  'git config --global credential.helper "!f() { echo username=x-access-token; echo password=\$GITHUB_TOKEN; }; f"'
```

## Validation

```sh
sbx exec pd-agent-a -- bash -lc 'pwd; git status; git branch; git remote -v'
sbx exec pd-agent-a -- bash -lc 'docker info | grep -E "Name|Server Version"'
sbx exec pd-agent-a -- bash -lc 'java -version'
sbx exec pd-agent-a -- bash -lc 'mvn --version | head -1'
sbx exec pd-agent-a -- bash -lc 'cat /run/sandbox/source/.sbx/agent-a.md'
sbx exec pd-agent-a -- bash -lc 'opencode --version'
```

## Disposal

```sh
sbx rm pd-agent-a     # destroys VM, private daemon, all state
sbx rm pd-agent-b
```

Each sandbox is independently disposable. Removing one never affects the other or the host.

## Known limitations

- **SSH agent forwarding on Windows**: The per-sandbox forwarder container
  (Linux) cannot reach the Windows `openssh-ssh-agent` named pipe. The relay
  resets connections. Use `sbx secret set github` or inject `-e GITHUB_TOKEN`
  per-session instead.
- **Secrets require sandbox creation**: `sbx secret set` only injects into
  sandboxes created AFTER the secret was stored. Existing sandboxes need
  `sbx rm` + `sbx create` to pick up new secrets.
- **Clone path mismatch**: The template creates an empty `/home/agent/workspace`
  dir, but the actual writable clone lives at the guest-mirrored UNC path.
  Agent briefs should reference the correct path (cwd = workspace).
