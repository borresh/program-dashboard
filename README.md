# program-dashboard

One place to see where each of my programs stands, and a persistent channel for
the AI agents working on them to report progress and ask me questions.

The browser and the agents use the same REST API. There is no separate
backend-for-frontend.

**Status: v1 complete.** An agent posts a clarification question through the API, I
answer it in the browser, and the agent reads the answer back on its next poll. No
copy-paste, nothing lost.

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
mvn -f backend/pom.xml verify     # tests, Checkstyle, OpenAPI export, client generation
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run build
./e2e.sh                          # browser checks against the running containers
```

`mvn verify` needs Docker for the Testcontainers `*IT` tests; `mvn test` does not.

## The API contract

The backend owns the OpenAPI specification. `mvn -f backend/pom.xml verify` exports
it to `backend/target/openapi.json` and generates the Angular client into
`frontend/src/app/api`. Neither is committed, and the client is never hand-written —
`npm run build` refuses to start if it is missing rather than using a stale copy.

Available today:

```
POST   /api/agents                                    register (idempotent by name)
GET    /api/agents

POST   /api/programs
GET    /api/programs
GET    /api/programs/{idOrSlug}                       resolves by UUID or slug
PATCH  /api/programs/{idOrSlug}

GET    /api/programs/{idOrSlug}/milestones
POST   /api/programs/{idOrSlug}/milestones
PATCH  /api/milestones/{id}
DELETE /api/milestones/{id}?actorAgentId=...

POST   /api/programs/{idOrSlug}/clarifications
GET    /api/clarifications?status=&program=
GET    /api/clarifications/{id}                       the endpoint an agent polls
POST   /api/clarifications/{id}/answer

GET    /api/programs/{idOrSlug}/activity?page=&size=
GET    /actuator/health
```

Every error is RFC 7807 Problem Details from one `@RestControllerAdvice`, and every
`detail` names the offending input and what was expected. Every write carries the id
of a registered agent, which is what fills the append-only activity log and keeps
`lastSeenAt` meaningful.

## Layout

```
backend/     Spring Boot 4.1, Java 25, PostgreSQL via Spring Data JPA and Flyway
frontend/    Angular 22, Angular Material for behaviour, Tailwind for layout
.mvn/        project-local Maven settings; see the comment in settings.xml
e2e/         Playwright browser checks, run in a container
up.sh        build the backend, then bring the stack up
e2e.sh       run the browser checks
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

1. **Skeleton** — compose, schema, health, quality gates, Angular shell.
2. **Agents, programs and milestones** — entities, services, controllers, OpenAPI
   spec, generated client.
3. **Clarifications end to end** — asking, answering from the browser, polling, plus
   the overview and detail screens.
4. **Activity log, `ABANDONED` toggle and the documentation.**

Deliberately not built, and not to be added without asking: review handoff between
agents (v1.1), starting and stopping programs (v2), deleting a program, live log
streaming, authentication, notifications, and anything that reads from disk.

## For the agents

`AGENT-API.md` is the usage guide, written to be pasted into an agent's context
without reading the OpenAPI specification. It ends with a snippet to drop into a
tracked project's own `AGENTS.md` so that project's agent reports here.

`AGENTS.md` in this directory is for agents working *on* the dashboard itself.

## A note on the test database

Two levels, deliberately.

- **H2 in PostgreSQL mode** for the `*Test` suite: schema, state rules, error shapes.
  Runs against the same single set of Flyway migrations, needs no Docker, finishes in
  seconds. `mvn test` stays Docker-free.
- **A real PostgreSQL through Testcontainers** for the `*IT` suite. `mvn verify` needs
  Docker.

The second level exists because of a specific bug. The two drivers return different
Java types for the same column — `timestamp with time zone` arrives as
`OffsetDateTime` from H2 and as `Instant` from PostgreSQL, and H2 hands back `uuid`
as `byte[]`. A native query selecting a timestamp passed every H2 test and returned
500 in production. `ProgramOverviewPostgresIT` is the test that catches that class of
failure, and it is why `ProgramRepository.findAllCounts()` now selects nothing but an
identifier and four counts.

Running `mvn verify` **from inside a dev container** needs one variable, because
containers started through a mounted Docker socket publish their ports on the host:

```sh
export TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal
```

`./up.sh` sets this for you when it detects it is running in a container.

## The screens

Two, as specified. `/` lists every program with its status, progress and open
questions, flags anything with a blocking question, and refreshes every 15 seconds
with a manual refresh alongside. `/programs/:slug` shows the open questions first —
they are the only thing on the page waiting on you — then the milestone checklist,
then answered questions, then the initial prompt rendered as read-only markdown.

The detail page deliberately has no timer. It reloads on navigation and after every
write, so nothing pulls the ground out from under a half-written answer.

Markdown is sanitised with DOMPurify before it reaches the page. A prompt is written
by an agent and stored verbatim, so it is untrusted input being rendered as HTML.

## A note on nulls

An optional value is **absent** from a response, never `null`. `progress` is missing
when a program has no milestones; it is never `0`. That single rule keeps the
generated TypeScript truthful, because springdoc cannot express a nullable object
reference correctly in OpenAPI 3.1 and a response carrying nulls would produce a
client claiming fields are `undefined` while the wire sent `null`.
