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
(forwarder Linux container can't reach `\\.\pipe\openssh-ssh-agent`), so agents
push over **HTTPS + PAT**. Components and how durable they are across sandbox
sessions:

| Piece | Where | Durable? |
|---|---|---|
| Credential helper (`username x-access-token`, password from token file) | global `~/.gitconfig` | Yes |
| Git identity (`user.name`/`user.email`) | clone + global | Yes (global) |
| `remote.origin.url` = `https://github.com/...` | clone `.git/config` | **No — re-provisioned back to `ssh://` each session** |

The origin URL is re-provisioned to SSH on every session, so **set it in the
launch script, right before the agent runs** (see Headless autonomous runs
below). Do not rely on `url.<base>.insteadOf`: it does not match the scp-style
`git@github.com:` form (only the canonical `ssh://git@github.com/` URL).

One-time bootstrap per VM (token is never echoed):

```sh
# needs --user root or sudo on /home
sudo mkdir -p /home/borre/dev/projects \
  && sudo ln -sfn \
     '//wsl.localhost/Ubuntu/home/borre/dev/projects/program-dashboard' \
     /home/borre/dev/projects/program-dashboard

gh auth token > /tmp/opencode/gh_token
cat /tmp/opencode/gh_token \
  | powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' \
      exec pd-agent-a -- bash -c 'umask 077; cat > /home/agent/.github-token'"
cat /tmp/opencode/git_auth.sh \
  | powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' \
      exec pd-agent-a -- bash -s"
rm /tmp/opencode/gh_token
```

where `git_auth.sh` is:

```sh
set -e
git config --global url."https://github.com/".insteadOf "ssh://git@github.com/"
git config --global credential.https://github.com.helper '!f(){ echo username=x-access-token; echo password="$(cat /home/agent/.github-token)"; };f'
git config --global user.name  "pd-agent-a"
git config --global user.email "pd-agent-a@program-dashboard.local"
```

Test before trusting it (creates then deletes a throwaway branch):

```sh
powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' exec pd-agent-a -- bash -lc \
  'cd //wsl.localhost/Ubuntu/home/borre/dev/projects/program-dashboard \
   && git remote set-url origin https://github.com/borresh/program-dashboard.git \
   && git push -f origin HEAD:refs/heads/agent/__authtest__ \
   && git push origin --delete agent/__authtest__ && echo PUSH_AUTH_OK'" 2>&1
```

## Headless autonomous runs

`opencode run` in a non-interactive exec **auto-rejects** any tool call whose
permission is not pre-approved — there is no human to prompt. The shared config
must allow (all baked into `~/dev/sbx_opencode_configuration/opencode.json`):
`git` commit/push, the build commands (`mvn`, `npm`, `./up.sh`, `./e2e.sh`,
`docker`), and `external_directory` for the workspace paths.

Keep the sandbox alive: a background (`nohup ... &`) agent dies when the exec
session detaches and auto-stop fires (~30 s). Run `opencode run` in the
**foreground** of a long-lived exec instead:

```sh
# launch_a.sh — piped to:  sbx exec pd-agent-a -- bash -s
#!/usr/bin/env bash
set -e
export GITHUB_TOKEN="$(cat /home/agent/.github-token)"
cd //wsl.localhost/Ubuntu/home/borre/dev/projects/program-dashboard || exit 1
git remote set-url origin https://github.com/borresh/program-dashboard.git   # re-provisioned each session
cat > /home/agent/task-a.txt <<'TASK'
<the task prompt ...>
TASK
opencode run "$(cat /home/agent/task-a.txt)" 2>&1 | tee /home/agent/opencode-a.log
echo "AGENT_EXIT=${PIPESTATUS[0]}"
```

Caveats learned in Phase 7:
- **Node floor:** VM node is `v22.22.1`; Angular CLI 22 needs `≥ v22.22.3`
  (lint/build fail with a clear message). Bootstrap it up-front:
  `curl -sL https://nodejs.org/dist/v22.22.3/node-v22.22.3-linux-x64.tar.gz -o /tmp/n.tgz
  && tar -xzf /tmp/n.tgz -C /tmp --strip-components=1 ...` then
  `export PATH=/tmp/node-v22.22.3/bin:$PATH`. Use the `.tar.gz` — no `xz` in
  the sandbox.
- Agents write their standalone report to `verification/agent-a.md` /
  `verification/agent-b.md` at the repo root (docs/ and .sbx/ are off-limits to
  them).
- Task prompts should tolerate already-existing lanes (branch exists, report
  already committed) so re-runs end at the push step instead of failing on
  `git checkout -b` or "nothing to commit".

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
  resets connections. Agents push over HTTPS instead — see *GitHub auth* above.
- **Secrets require sandbox creation**: `sbx secret set` only injects into
  sandboxes created AFTER the secret was stored. Existing sandboxes need
  `sbx rm` + `sbx create` to pick up new secrets.
- **Clone path mismatch**: The template creates an empty `/home/agent/workspace`
  dir, but the actual writable clone lives at the guest-mirrored UNC path.
  Agent briefs should reference the correct path (cwd = workspace).
