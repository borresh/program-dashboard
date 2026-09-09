# Sandbox Compose

Each sandbox runs its own Docker Compose stack using the same committed files.

## Files (identical in all environments)

- `docker-compose.yml` — production-like stack (postgres + backend + frontend)
- `docker-compose.e2e.yml` — test overrides (adds Playwright e2e service)

## Why no override file

Ports bind to each daemon's own loopback, so nothing collages between sandboxes or with the host. The committed compose files work unchanged in all environments.

## Services inside sandbox

| Service | Image | Purpose |
|---------|-------|---------|
| postgres | `postgres:16-alpine` | Sandbox-local database |
| backend | Built from `./backend` | Spring Boot API |
| frontend | Built from `./frontend` | Angular app |
| e2e | Built from `./e2e` | Playwright (e2e config only) |

## Verification

```sh
sbx exec pd-agent-a bash -c 'cd /home/agent/workspace && docker compose ps'
sbx exec pd-agent-a bash -c 'curl -fsS http://localhost:8080/actuator/health'
```
