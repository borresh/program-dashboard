# program-dashboard

A browser dashboard and REST API for tracking the programs I build with AI
agents. I use the browser to see where every program stands and to answer
questions; agents use the REST API to register programs, report progress and ask
me for clarification.

## Objective

Give me one place that shows where each of my programs stands, and give the AI
agents working on them a persistent shared channel for status and clarification
requests. Today I track nothing formally — I rely on memory and re-read code or
chat scrollback, and a clarification question an agent asks me is lost the moment
I close that session.

**Success:** an implementing agent posts a clarification question against a
program through the REST API; I see it in the browser and answer it there; the
agent reads my answer back through the API and continues. No copy-paste, nothing
lost.

## Users and scenarios

Two kinds of user, both first-class.

- **Me** — single human, in a browser on my own machine. No login.
- **AI agents** — each registered by name with a role, talking only to the REST
  API.

1. An implementing agent hits an ambiguity, posts a clarification question
   against a program, and continues or waits. I see it on the dashboard, answer
   it, and the agent reads the answer on its next poll.
2. I open the dashboard and see every program in one list: status, progress,
   open questions.
3. I register a new program by pasting its initial prompt, before any code
   exists.
4. I ask an agent in an unrelated chat "what's the status of program-dashboard?"
   and it answers by calling `GET /api/programs/program-dashboard`.

## Scope

### In scope (v1)

Registering a program from its initial prompt; a program overview page;
milestones and progress; clarification questions with proposed answers; a
per-program activity log; and a REST API covering all of the above so agents can
do everything I can.

### Out of scope

- **Review handoff between agents.** Requesting a review, claiming it, and
  posting a verdict with findings is deliberately deferred to v1.1. Do not build
  it, do not add `ReviewRequest` or `ReviewFinding` tables, and do not add
  columns in anticipation of it. Adding it later is a new migration, which is
  cheaper than carrying unused schema now.
- **Starting and stopping programs**, and anything that executes commands or
  controls containers. Deferred to v2.
- **Deleting a program**, through the UI or the API. `ABANDONED` status plus a
  hidden-by-default filter on the overview covers it; if a row genuinely has to
  go, I remove it in the database myself.
- Live log streaming.
- Authentication, authorisation and multi-user support.
- Notifications of any kind.
- Editing code or prompt files in the browser.
- Metrics, charts and time-series analytics.
- Storing chat transcripts.
- Mobile and responsive layout — desktop browser only.
- CI, builds and deployment of the programs being tracked.
- Free-form agent-to-agent messaging.
- Reading anything from disk: no git integration, no scanning or importing of
  existing projects. The dashboard starts empty and only knows what it was told
  through the API.

## Functional requirements

**Agents**

- **R1.** `POST /api/agents` with `{name, role, description?}` registers an
  agent. It is idempotent by name: an existing name returns `200` with the
  existing record, a new name returns `201`. `role` is `IMPLEMENTER`, `REVIEWER`
  or `HUMAN`.
- **R2.** `GET /api/agents` lists all registered agents.
- **R3.** Any write that carries an `actorAgentId` updates that agent's
  `lastSeenAt`. An unknown agent id on any write → `404` naming the id.

**Programs**

- **R4.** `POST /api/programs` with
  `{slug, name, description?, initialPrompt, createdByAgentId, milestones?}`
  creates a program with status `IDEA`. Returns `201` and a `Location` header.
- **R5.** A duplicate slug → `409` naming the slug and the existing program.
- **R6.** A missing or invalid field → `400` listing every offending field and
  the expected shape. `slug` must match `^[a-z0-9]+(-[a-z0-9]+)*$`.
- **R7.** `GET /api/programs` returns every program with `id`, `slug`, `name`,
  `status`, `completedMilestones`, `totalMilestones`, `openClarificationCount`,
  `blockingClarificationCount`, `updatedAt`.
- **R8.** `GET /api/programs/{idOrSlug}` resolves by either UUID or slug and
  returns the full program including prompt, milestones and clarifications.
  Unknown identifier → `404` naming it.
- **R9.** `PATCH /api/programs/{idOrSlug}` changes `name`, `description`,
  `initialPrompt` or `status`. Status is one of `IDEA`, `ACTIVE`, `PAUSED`,
  `DONE`, `ABANDONED`. Every status change appends an activity entry. There is no
  delete endpoint.

**Milestones**

- **R10.** `POST /api/programs/{idOrSlug}/milestones` with
  `{title, description?, position}` adds a milestone. `PATCH
  /api/milestones/{id}` edits it or marks it complete, recording `completedAt`
  and `completedByAgentId`.
- **R11.** Completing an already-complete milestone → `409` naming who completed
  it and when.
- **R12.** Progress is `completedMilestones / totalMilestones`. A program with
  zero milestones reports progress as `null`, never `0%`.

**Clarifications**

- **R13.** `POST /api/programs/{idOrSlug}/clarifications` with
  `{askedByAgentId, question, context?, blocking, proposedOptions?}` creates one
  with status `OPEN`. Each proposed option is `{label, rationale}` and keeps its
  submitted order.
- **R14.** `POST /api/clarifications/{id}/answer` with
  `{answeredByAgentId, answerText?, chosenOptionId?}` sets status `ANSWERED`,
  `answeredAt` and `answeredByAgentId`. At least one of `answerText` and
  `chosenOptionId` is required. When I answer in the browser, the frontend sends
  the seeded `HUMAN` agent id (see *Data model*), read from
  `PROGRAM_DASHBOARD_HUMAN_AGENT_ID`.
- **R15.** Answering an already-answered clarification → `409` naming the
  answering agent and the timestamp. The check and the write are atomic — a
  conditional update guarded on `status = 'OPEN'` inside one transaction, not
  read-then-write.
- **R16.** `GET /api/clarifications/{id}` returns status, answer, chosen option
  and answering agent. This is the endpoint an implementing agent polls.
- **R17.** `GET /api/clarifications?status=OPEN&program={idOrSlug}` lists
  clarifications; both parameters are optional.
- **R18.** `blocking` is informational. The dashboard surfaces it prominently
  but never prevents any other operation.

**Activity and UI**

- **R19.** Every write in R4–R18 appends an `ActivityEntry` with
  `{programId, type, actorAgentId, summary, occurredAt}`. The log is append-only;
  nothing edits or deletes an entry.
- **R20.** `GET /api/programs/{idOrSlug}/activity?page=&size=` returns entries
  newest-first, default size 50.
- **R21.** The overview page at `/` lists all programs with the R7 fields and
  visibly flags programs that have a blocking clarification. Programs with status
  `ABANDONED` are hidden by default and revealed by a toggle. The page
  auto-refreshes every 15 seconds and has a manual refresh control.
- **R22.** The detail page at `/programs/:slug` shows: the initial prompt
  rendered as read-only markdown; the milestone checklist with progress; open
  clarifications first, each rendering its proposed options as selectable choices
  plus a free-text field and an Answer button; and the activity timeline.
- **R23.** Every error response is RFC 7807 Problem Details, produced by one
  `@RestControllerAdvice`. Every `detail` names the offending input and what was
  expected. No stack trace ever reaches a response body.

## Interface contract

```
POST   /api/agents                                    register (idempotent by name)
GET    /api/agents

POST   /api/programs
GET    /api/programs
GET    /api/programs/{idOrSlug}
PATCH  /api/programs/{idOrSlug}

POST   /api/programs/{idOrSlug}/milestones
PATCH  /api/milestones/{id}
DELETE /api/milestones/{id}

POST   /api/programs/{idOrSlug}/clarifications
GET    /api/clarifications?status=&program=
GET    /api/clarifications/{id}
POST   /api/clarifications/{id}/answer

GET    /api/programs/{idOrSlug}/activity?page=&size=
GET    /actuator/health
```

Screens: `/` overview table, `/programs/:slug` detail. No other routes in v1.

**Worked example — the success check, end to end**

```
# 1. The implementing agent registers itself. Idempotent, so it can do this every session.
$ curl -s -X POST localhost:8080/api/agents -H 'Content-Type: application/json' \
  -d '{"name":"opencode-implementer","role":"IMPLEMENTER"}'
{"id":"7d1f8c2e-...","name":"opencode-implementer","role":"IMPLEMENTER","registeredAt":"2026-08-31T09:00:00Z"}

# 2. It hits an ambiguity and asks, with two proposed answers.
$ curl -s -X POST localhost:8080/api/programs/program-dashboard/clarifications \
  -H 'Content-Type: application/json' -d '{
    "askedByAgentId":"7d1f8c2e-...",
    "question":"Can any registered agent complete a milestone, or only the agent that created the program?",
    "context":"R10 records completedByAgentId but never says who is allowed to set it.",
    "blocking":true,
    "proposedOptions":[
      {"label":"Any registered agent",
       "rationale":"A second agent often picks up work mid-project, and completedByAgentId is already an audit trail."},
      {"label":"Only the creating agent",
       "rationale":"Prevents a stray script from marking work done."}]}'
HTTP/1.1 201 Created
Location: /api/clarifications/3a947b10-...
{"id":"3a947b10-...","status":"OPEN","blocking":true,"askedAt":"2026-08-31T09:02:11Z"}

# 3. I open /programs/program-dashboard, pick option 1 and submit. The browser sends:
POST /api/clarifications/3a947b10-.../answer
{"answeredByAgentId":"<harald HUMAN agent id>","chosenOptionId":"<option-1 id>",
 "answerText":"Option 1. completedByAgentId is enough of an audit trail."}

# 4. The agent polls and gets its answer.
$ curl -s localhost:8080/api/clarifications/3a947b10-...
{"id":"3a947b10-...","status":"ANSWERED",
 "answerText":"Option 1. completedByAgentId is enough of an audit trail.",
 "chosenOption":{"label":"Any registered agent"},
 "answeredBy":{"name":"harald","role":"HUMAN"},"answeredAt":"2026-08-31T09:14:03Z"}

# 5. A second answer is rejected.
$ curl -s -X POST localhost:8080/api/clarifications/3a947b10-.../answer -d '{...}'
HTTP/1.1 409 Conflict
{"type":"about:blank","title":"Clarification already answered","status":409,
 "detail":"Clarification 3a947b10-... was answered by 'harald' at 2026-08-31T09:14:03Z"}
```

## Data model

```
Agent            { id UUID pk, name unique, role: IMPLEMENTER|REVIEWER|HUMAN,
                   description?, registeredAt, lastSeenAt }
Program          { id UUID pk, slug unique, name, description?, initialPrompt text,
                   status: IDEA|ACTIVE|PAUSED|DONE|ABANDONED,
                   createdByAgentId fk, createdAt, updatedAt }
Milestone        { id, programId fk, title, description?, position int,
                   completedAt?, completedByAgentId? fk }
Clarification    { id, programId fk, question, context?, blocking bool,
                   status: OPEN|ANSWERED, askedByAgentId fk, askedAt,
                   answerText?, chosenOptionId? fk, answeredByAgentId? fk, answeredAt? }
ClarificationOption { id, clarificationId fk, position int, label, rationale }
ActivityEntry    { id, programId fk, type enum, actorAgentId? fk, summary text, occurredAt }
```

Five tables. `REVIEWER` stays in the role enum even though nothing consumes it in
v1 — an agent describing itself as a reviewer is meaningful today, and an enum
value costs nothing. That is the only concession to the deferred review feature.

`V1__init.sql` seeds exactly one row so the browser always has an actor to
attribute answers to:

```sql
INSERT INTO agent (id, name, role, description, registered_at, last_seen_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'harald', 'HUMAN',
        'The human operator answering from the dashboard', now(), now());
```

That UUID is fixed and referenced by the frontend through
`PROGRAM_DASHBOARD_HUMAN_AGENT_ID`. It is the only seed data in the project.

PostgreSQL 16 in Docker, published to `localhost:5432`. No devbox network and no
container-DNS hostnames — connection strings point at `localhost`. Schema in
`backend/src/main/resources/db/migration/V1__init.sql`, applied by Flyway. All
timestamps stored UTC.

UUIDs are generated in Java, not by `gen_random_uuid()`, and the schema uses no
`JSONB`, arrays or `ON CONFLICT`. That is deliberate: it keeps repository tests
on H2 rather than forcing Testcontainers. If a migration later needs
PostgreSQL-specific SQL, move the affected repository tests to Testcontainers
rather than maintaining a second H2 schema.

## Integrations and configuration

No external services. Configuration binds to typed Spring configuration records
and is overridden by environment variables. `.env.example` lists every key with a
placeholder:

```
POSTGRES_DB=program_dashboard
POSTGRES_USER=program_dashboard
POSTGRES_PASSWORD=change-me
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/program_dashboard
SPRING_DATASOURCE_USERNAME=program_dashboard
SPRING_DATASOURCE_PASSWORD=change-me
SERVER_PORT=8080
FRONTEND_PORT=4200
PROGRAM_DASHBOARD_CORS_ALLOWED_ORIGINS=http://localhost:4200
PROGRAM_DASHBOARD_HUMAN_AGENT_ID=00000000-0000-0000-0000-000000000001
```

No secret has a hardcoded default. There is no authentication in v1, so anything
that can reach the port has full write access — the app binds to localhost only.

## Tech stack

### Decided — do not deviate

- **Backend:** Java 25, Spring Boot, Maven. Base package
  `no.borresh.programdashboard`. Three-layer: `@RestController` → `@Service` →
  repository. Constructor injection only.
- **Frontend:** Angular 20+, standalone components, TypeScript. Follow the
  `angular-components`, `angular-services` and `angular-utils` skills rather than
  any pre-v17 idiom.
- **UI:** Angular Material for anything with behaviour (tables, forms, dialogs,
  checkboxes, tabs); Tailwind for layout and spacing only, with preflight
  disabled so Material owns the baseline. Do not rebuild a Material component out
  of Tailwind utilities.
- **Persistence:** PostgreSQL, Spring Data JPA, Flyway. The backend owns all
  database access.
- **API contract:** REST. The backend owns the OpenAPI specification; the Angular
  client is generated from it as a build step and is never hand-written.
- **Errors:** RFC 7807 Problem Details on every error response, from one
  `@RestControllerAdvice`.
- **Quality gates:** ESLint + Prettier on the frontend, Checkstyle on the
  backend. No Java auto-formatter — that asymmetry is intentional.
- **Repository:** monorepo with `backend/` and `frontend/`.
- **Runtime:** Docker Compose brings up Postgres, backend and frontend with one
  command. Must work on Linux and on Windows with WSL. No devbox, no shared
  Docker network, no container DNS names.
- **Language:** English everywhere — UI text, API field names, enum values, error
  messages, comments, commit messages and documentation.
- **Not used:** Lombok. Java records and compact constructors cover the need.

### Open — propose and ask

Everything under "Open questions" below. Pin major versions only and verify
current patch versions at project start.

## Quality bar

No performance targets. The dataset is tens of programs and thousands of activity
rows; a latency target here would be theatre.

- **Errors:** fail fast. Validate at the HTTP boundary with Bean Validation on
  request records, then trust the data inward. Every `detail` names the offending
  input and what was expected — "Invalid request" is not an error message. Never
  swallow an exception to keep going.
- **State transitions:** every illegal transition returns `409` with a `detail`
  explaining the current state and who put it there. Answering a clarification is
  an atomic conditional update, not read-then-write.
- **Logging:** INFO to stdout. WARN on every rejected state transition. No
  telemetry.
- **Security:** localhost only, no authentication, no secrets in the repository,
  no stack traces in responses.

## Project structure and conventions

```
program-dashboard/
  docker-compose.yml            postgres + backend + frontend
  .env.example
  README.md
  AGENTS.md
  AGENT-API.md
  backend/
    pom.xml
    Dockerfile
    src/main/java/no/borresh/programdashboard/
      ProgramDashboardApplication.java
      config/           typed configuration records, CORS, OpenAPI bean
      common/           ProblemDetail advice, shared enums
      agent/            controller, service, repository, entity, DTOs
      program/          controller, service, repository, Program, Milestone
      clarification/    controller, service, repository, Clarification, ClarificationOption
      activity/         ActivityEntry, ActivityService, ActivityController
    src/main/resources/db/migration/V1__init.sql
    src/test/java/...   mirrors the main package layout
  frontend/
    package.json
    Dockerfile
    src/app/
      programs/         overview page, detail page
      clarifications/   answer form and list components
      shared/
      api/              generated OpenAPI client, git-ignored
```

Organise by feature, not by technical layer. A JPA entity is never a request body
and never a response body — request and response records are separate and mapped
explicitly. Controllers bind and delegate; no rules, no queries. No `any` in
TypeScript. Introduce an abstraction at the second caller, not the first.

## Testing and definition of done

Test the state rules that would hurt to get wrong, plus one end-to-end pass over
the success check. No frontend tests in v1.

Done when all of the following hold:

- `mvn -f backend/pom.xml verify` is green, including Checkstyle.
- Unit tests cover every rejected transition: answering an answered clarification
  (R15), completing a completed milestone (R11), duplicate slug (R5), invalid
  slug format (R6), unknown agent id (R3), and an answer carrying neither
  `answerText` nor `chosenOptionId` (R14).
- One integration test walks the success check: register agent → create program →
  ask a blocking clarification → answer it as the seeded `harald` agent → poll
  and read the answer, confirming `answeredBy.name` is `harald`.
- `npm --prefix frontend ci && npm --prefix frontend run lint && npm --prefix
  frontend run build` is green.
- `docker compose up -d` from a clean checkout brings all three services up, and
  `curl localhost:8080/actuator/health` returns `UP`.
- The worked example in "Interface contract" runs verbatim against that fresh
  stack and produces the shown status codes.

## Build order

1. Monorepo skeleton, `docker-compose.yml`, Flyway `V1__init.sql`, health
   endpoint, Checkstyle and ESLint wired, empty Angular shell.
   **Stop for review.**
2. Agents, programs and milestones: entities, repositories, services,
   controllers, OpenAPI spec, generated client. Tests for R1–R12.
   **Stop for review.**
3. Clarifications end to end — this is the success check. Backend plus the
   Angular overview and detail pages with the answer form. Tests for R13–R18,
   R21, R22. **Stop for review.**
4. Activity log, overview badges and the `ABANDONED` toggle, `README.md`,
   `AGENTS.md`, `AGENT-API.md`, `.env.example`. **Stop for review.**

## Deliverables

- Working source and tests as described above.
- `README.md` — what it is, how to start it with one compose command, how to
  register the first program.
- `AGENTS.md` in the project root — build, lint and test commands, stack,
  structure, and the non-negotiable rules: constructor injection only; API types
  separate from persistence types; controllers delegate; no `any`; abstractions
  at the second caller; patterns on the third occurrence; when two designs are
  equal, pick the one easier to delete. Plus the anti-pattern table: Lombok,
  field injection, JPA entities as DTOs, logic in controllers, `any`, pre-v17
  Angular idioms, speculative abstraction. This makes the project self-contained
  if it later becomes its own repository.
- `AGENT-API.md` — a usage guide written for AI agents: how to register, how to
  register a program from a prompt, how to ask a clarification with proposed
  options, how to poll for the answer, and how to report a milestone. Written to
  be pasted into an agent's context without reading the OpenAPI spec.
- A reusable snippet inside `AGENT-API.md` that I can drop into any tracked
  project's own `AGENTS.md`, instructing that project's agent to report to this
  dashboard.
- `.gitignore` covering Java, Node, IDE and env artifacts, including
  `frontend/src/app/api/`.
- `.env.example` with every key listed above.

## Assumptions made by the author

- Single human user; the app binds to localhost only, so no authentication is
  acceptable and there is no meaningful CSRF or CORS threat model beyond dev
  convenience.
- Anything that can reach the port has full write access. This is accepted.
- Programs tracked by the dashboard do not need to live on the same machine as
  the dashboard, and the dashboard never touches their files.
- Agent count is single digits; program count is tens; activity entries are
  thousands.
- The initial prompt is markdown produced by the `project-prompt-builder` skill,
  and its "Build order" section is the natural source of the milestone list.
- Both the browser and the agents use the same REST API. There is no separate
  backend-for-frontend.
- The activity log is append-only. Clarification options are immutable once
  posted.
- `lastSeenAt` updates on any write carrying that agent's id, and on nothing
  else.
- Existing projects in this workspace are irrelevant — the dashboard starts empty
  and nothing is imported.
- Review handoff is coming in v1.1, but nothing in v1 may be shaped around it
  beyond the `REVIEWER` enum value.

## Open questions — answer or ask before implementing

1. **Frontend polling interval.** *Proposed:* 15 seconds on the overview page
   only; the detail page refreshes on navigation and after any write. Confirm, or
   make it configurable.
2. **Milestone completion authority.** *Proposed:* any registered agent may
   complete any milestone, recorded in `completedByAgentId`. Confirm, or restrict
   to the program's creating agent.
3. **Future start/stop fields.** v2 will add container control. *Proposed:* do
   **not** add `repoPath` or `startCommand` columns now — an unused column is
   speculative, and adding one later is a two-line migration. Confirm.
4. **Repository test engine.** The schema deliberately avoids PostgreSQL-specific
   SQL so H2 can serve repository tests. *Proposed:* keep H2 for v1.
   Alternative: Testcontainers from the start for full fidelity, at the cost of
   Docker being required to run the test suite on the Windows/WSL machine.
5. **OpenAPI client generation.** *Proposed:* `openapi-generator` invoked from an
   npm script that reads a spec file the backend writes to
   `backend/target/openapi.json` during its build, committed to neither. Confirm
   the mechanism, since it must work identically on Linux and WSL.

## How to work with me

Before writing any code:

1. Restate in 5–10 lines what you understand you are building.
2. List every assumption you would have to make that is not settled above.
3. Ask me the blocking questions — the ones where a wrong guess costs rework.
   Batch them; do not ask one at a time.
4. Propose an answer to each item under "Open questions" with a one-line
   rationale.
5. Wait for my reply. Do not start implementing until I confirm.

While implementing:

- Anything under "Decided" is fixed. If you believe it is wrong, say so and
  stop — do not silently substitute something else.
- Anything under "Out of scope" stays unbuilt, even if it is easy.
- Stop at each milestone marked "Stop for review" and show me what you have.
- If a requirement turns out to be contradictory or impossible, stop and ask
  rather than picking an interpretation.
- If you need a decision I have not covered at all, ask. Guessing is the one
  failure mode this prompt exists to prevent.
