# Secrets

## Principles

1. No production credentials reach sandboxes (credential isolation).
2. `.env` in the main repo contains only non-sensitive defaults (`change-me` for secrets).
3. Real secrets stay on the host.
4. Agents create their own `.env` from `.env.example` with safe dev defaults.

## Credential flow

| Secret | Storage | Injection point |
|--------|---------|-----------------|
| OpenCode Zen API key | Host env (`$env:OPENCODE_API_KEY`) | `-e OPENCODE_API_KEY` at `sbx create` |
| GitHub SSH key | Host `~/.ssh/id_ed25519` | SSH agent forwarding |
| Postgres password | Agent's `.env` (safe dev value) | Compose reads from `.env` |

## What agents see

- `.env.example` via read-only source mount — contains only `change-me` defaults
- Agent creates own `.env` with safe dev values
- API key injected as environment variable — never stored in `.env`
- SSH key forwarded via agent — never copied into VM

## What agents never see

- Host `.env` (real secrets)
- Production credentials
- OpenCode API key as a file (only as env var)
