# Personal Stack

The personal dev stack runs on Docker Desktop (WSL2 backend).

## Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| postgres | `postgres:16-alpine` | 5432 | Database |
| backend | Built from `./backend` | 8080 | Spring Boot API |
| frontend | Built from `./frontend` | 4200 | Angular app |

## Commands

```sh
cd ~/dev/projects/program-dashboard
cp .env.example .env           # fill with real dev secrets
./up.sh                        # build + bring up + health check
./e2e.sh                       # Playwright browser checks
docker compose down            # stop (add -v to discard DB)
```

## Ports

All bound to `127.0.0.1` only (off the LAN):
- 8080 — backend API
- 4200 — frontend dev server
- 5432 — PostgreSQL

## Verification

```sh
docker info | grep "Name"     # should show "docker-desktop"
curl -fsS http://localhost:8080/actuator/health
```
