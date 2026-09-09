# Database

## Engine

PostgreSQL 16 (Alpine image) in all environments.

## Instances

| Instance | Location | Purpose |
|----------|----------|---------|
| Personal | Docker Desktop (host) | Development, debugging |
| Sandbox A | pd-agent-a private daemon | Agent A work |
| Sandbox B | pd-agent-b private daemon | Agent B work |

Each instance is fully isolated — separate data volume, separate credentials, separate state.

## Volumes

- `pgdata` — persistent volume for each Postgres instance
- Created automatically by `docker compose up`
- Destroyed with `docker compose down -v`

## Migrations

Flyway runs on startup. Migrations must work on both H2 (unit tests) and PostgreSQL (integration/production).

## `.env` configuration

```sh
POSTGRES_DB=program_dashboard
POSTGRES_USER=program_dashboard
POSTGRES_PASSWORD=change-me         # safe dev default; agents create own .env
```

No secret has a default — an unset variable stops startup.
