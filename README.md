# program-dashboard

One place to see where each of my programs stands, and a persistent channel for
the AI agents working on them to report progress and ask me questions.

The browser and the agents use the same REST API. There is no separate
backend-for-frontend.

> **Status: phase 1 of 4.** The skeleton runs end to end — database, schema,
> health endpoint, application shell — but no `/api` endpoints exist yet. See
> *Build order* below.

## Requirements

- **JDK 25 or later.** The backend targets Java 25. If your default JVM is older,
  `./up.sh` will find a suitable JDK for you; `mvn` on its own will not, so set
  `JAVA_HOME` before running Maven directly.
- **Docker** with Compose v2.
- **Node 22 or later**, only if you want to run the frontend outside Docker.

## Running it

```sh
cp .env.example .env      # then edit it; no secret has a default
./up.sh
```

`./up.sh` builds the backend, brings up Postgres, backend and frontend, and waits
until every container reports healthy. Then open the dashboard on the port in
your `.env` (`FRONTEND_PORT`, default 4200).

Once the backend is built, `docker compose up -d --wait` on its own is enough.
The extra step exists because the backend build produces the OpenAPI specification
that the frontend's API client is generated from, and that file is committed to
neither the repository nor an image.

```sh
docker compose logs -f backend    # follow the backend
docker compose down               # stop, keeping the database volume
docker compose down -v            # stop and discard the database
```

## Checks

```sh
export JAVA_HOME=/path/to/jdk-25
mvn -f backend/pom.xml verify                  # tests + Checkstyle
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run build
```

## Layout

```
backend/     Spring Boot 4.1, Java 25, PostgreSQL via Spring Data JPA and Flyway
frontend/    Angular 22, Angular Material for behaviour, Tailwind for layout
.mvn/        project-local Maven settings; see the comment in settings.xml
up.sh        build the backend, then bring the stack up
```

Everything the application knows, it was told through the API. It reads nothing
from disk: no git integration, no scanning of existing projects. It starts empty.

## Configuration

Every key lives in `.env.example`. `.env` is git-ignored. There is no
authentication: all ports are published to `127.0.0.1` only, and anything that can
reach them has full write access. That is accepted for a single-user local tool.

The only seed data in the project is one `HUMAN` agent, created by
`V1__init.sql` under the fixed id `00000000-0000-0000-0000-000000000001`. Answers
submitted from the browser are attributed to it. The frontend receives that id
through `PROGRAM_DASHBOARD_HUMAN_AGENT_ID`, written into `config.json` by the
container entrypoint at start time — an Angular bundle is static, so the value
cannot be baked in without rebuilding the image.

### This machine

Two local deviations, neither of which is in git:

- **Ports.** `.env.example` documents 8080 and 4200. The `devbox-hvdc` container
  has reserved 8080 and 4200–4519 with Docker, so the local `.env` uses **8081**
  and **4520** instead. Substitute those ports in any documented `curl`.
- **Maven resolution.** The machine-wide `~/.m2/settings.xml` mirrors Maven
  Central to a corporate Artifactory whose token expired on 2026-08-11. This
  project resolves straight from Central through `.mvn/settings.xml` instead, and
  does not touch the machine-wide file.

If you run commands from inside the devbox container, published ports live on the
WSL host and are not reachable from there. Use `docker compose up -d --wait`,
which relies on the containers' own healthchecks, rather than curling
`localhost`.

## Build order

1. **Skeleton** — compose, schema, health, quality gates, Angular shell. *Done.*
2. Agents, programs and milestones: entities, services, controllers, OpenAPI
   spec, generated client.
3. Clarifications end to end — asking, answering from the browser, polling.
4. Activity log, overview badges, `AGENTS.md`, `AGENT-API.md`.
