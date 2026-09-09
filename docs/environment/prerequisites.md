# Prerequisites

## Host (Windows 11 + WSL2)

- Windows 11 Home, build 26200
- WSL2 Ubuntu 24.04, systemd on, user `borre`
- Docker Desktop (CLI 29.7.2, Compose v5.5.0)
- sbx v0.39.0+ (Docker Sandbox CLI)
- Git 2.43.0
- Node v24
- JDK 25 (project requirement) — `~/.jdks/openjdk-26.0.2.1` available (backward-compatible)
- `gh` CLI at `~/.local/bin/gh` (v2.73.0)
- SSH key (`~/.ssh/id_ed25519`) associated with GitHub account `borresh`

## Network

- Ports 8080, 4200, 5432 free on host
- GitHub SSH access verified (`ssh -T git@github.com`)
- sbx network policy allows: opencode.ai, Maven repos, npm registry, Docker registry, GitHub

## Credentials

- OpenCode Zen API key (injected via `-e OPENCODE_API_KEY` at sandbox creation)
- GitHub SSH key (agent forwarding for in-VM push)
- No production credentials or data reach sandboxes
