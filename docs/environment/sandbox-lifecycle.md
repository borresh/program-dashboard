# Sandbox Lifecycle

## Creation

```sh
powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' create \
  --clone --name pd-agent-a --cpus 4 -m 8g \
  -e OPENCODE_API_KEY \
  opencode '\\wsl.localhost\Ubuntu\home\borre\dev\projects\program-dashboard'"
```

Each sandbox:
- Clones `main` from host repo into private in-VM clone
- Host repo mounted read-only at `/run/sandbox/source`
- Forge remotes (`origin` → GitHub) propagated into clone
- Private Docker daemon (20 GB sparse block volume)

## Agent config injection

```sh
tar -C ~/dev/sbx_opencode_configuration -cf - AGENTS.md opencode.json .opencode \
  | sbx exec pd-agent-a -- bash -c 'mkdir -p ~/.config/opencode && tar -xf - -C ~/.config/opencode'
```

## Validation

```sh
sbx exec pd-agent-a bash -c 'pwd; git status; git branch; git remote -v'
sbx exec pd-agent-a bash -c 'docker info | grep -E "Name|Server Version"'
sbx exec pd-agent-a bash -c 'java -version'
sbx exec pd-agent-a bash -c 'cat /run/sandbox/source/.sbx/agent-a.md'
```

## Disposal

```sh
sbx rm pd-agent-a     # destroys VM, private daemon, all state
sbx rm pd-agent-b
```

Each sandbox is independently disposable. Removing one never affects the other or the host.
