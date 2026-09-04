# DECISIONS.md

Every place this project's implementation deliberately differs from
`prompts/program-dashboard.md`, the original specification, with the reason. Each was
either agreed with the human during a review stop or forced by something the
environment made impossible.

**This file wins where it and the original prompt disagree.** The prompt describes
the intent; this file describes what was actually built and why. A future agent
reading only the prompt would undo several of these — don't.

Organised by theme, not by build phase.

## API contract

- **Optional response fields are absent, never `null`.**
  (`spring.jackson.default-property-inclusion: non_null`.) springdoc cannot express a
  nullable object reference correctly in OpenAPI 3.1 — it emits
  `{"type":"null","$ref":...}` — so a response carrying explicit nulls produced a
  generated TypeScript client that claimed a field was `undefined` while the wire sent
  `null`. One rule for the whole API avoids the mismatch. Concretely: `progress` is
  **absent** when a program has no milestones, never `0`.

- **Every controller declares `produces = MediaType.APPLICATION_JSON_VALUE`.**
  Without it springdoc writes the response content type as a wildcard, the generated
  client can't tell the response is JSON, and it falls back to `responseType: 'blob'`
  — every response arrives as an unreadable `Blob`. This shipped once: 44 backend
  tests passed while the dashboard rendered nothing, because MockMvc never goes
  through the generated client. Caught only by the Playwright suite.

- **Response records carry `@Schema(requiredMode = REQUIRED)`.** Without it, every
  field in the generated TypeScript is optional (`field?: T`), which would push `?.`
  and `!` through every component regardless of whether the field can actually be
  missing.

- **Operation IDs are named explicitly** (`@Operation(operationId = "...")`) on every
  endpoint. Without it, springdoc derives names from the Java method name plus an
  overload counter, producing client methods like `findOne1(...)`. Explicit names
  give `getProgram(...)`, `answerClarification(...)`, etc.

- **`DELETE /api/milestones/{id}` takes `?actorAgentId=`, not a request body.**
  DELETE bodies are legal HTTP but are dropped by some intermediaries and generate
  awkwardly in OpenAPI clients. The interface contract in the prompt lists this
  endpoint with no body; a query parameter is the more literal reading anyway.

- **`DELETE /api/milestones/{id}` is implemented**, even though it is in the
  interface contract but in no numbered requirement (R4–R18 don't mention it, and
  R19's activity rule only names R4–R18). Decided to build it: a mistyped milestone
  would otherwise be permanent, and the append-only activity log still records the
  deletion (`MILESTONE_DELETED`).

- **`PATCH /api/programs`, `PATCH`/`DELETE /api/milestones` require
  `actorAgentId`** in the body (or as a query param for the DELETE), even though the
  original prompt's field lists for these endpoints don't include it. R19 requires an
  attributed activity entry for every write; without a mandatory actor these three
  endpoints had no agent to attribute to.

- **Re-registering an existing agent name (R1) only bumps `lastSeenAt`.** It does not
  overwrite the stored `role` or `description` from the second call. A small
  deviation from R1's literal wording ("idempotent by name") in favour of the more
  useful reading: an agent re-registering every session is proving it's alive, not
  correcting its own metadata.

- **Overview sort order**: programs with an open blocking clarification first, then
  `updatedAt` descending. Not specified in the prompt; chosen because a blocked agent
  is the thing most worth seeing first.

- **Activity type vocabulary** (`PROGRAM_CREATED`, `PROGRAM_UPDATED`,
  `PROGRAM_STATUS_CHANGED`, `MILESTONE_ADDED`, `MILESTONE_UPDATED`,
  `MILESTONE_COMPLETED`, `MILESTONE_DELETED`, `CLARIFICATION_ASKED`,
  `CLARIFICATION_ANSWERED`) is invented; the prompt names the field but never
  enumerates values.

- **`ProgramDetailResponse` includes a `clarifications` field**, open questions
  first. Not explicit in R8's field list, but R22 requires the detail page to render
  clarifications, and the alternative (a second round-trip from the frontend) was
  rejected as unnecessary complexity for a "tens of programs" dataset.

## Data model

- **Six tables, not five.** The prompt's "Five tables" line undercounts its own
  schema listing (`Agent`, `Program`, `Milestone`, `Clarification`,
  `ClarificationOption`, `ActivityEntry`). Built as specified; the prose is simply
  wrong.

- **Columns `sort_order` and `entry_type`, not `position` and `type`.** Both
  `position` and `type` are function names in HQL and would be misparsed as such. The
  API field names are unaffected — `position` and `type` still appear on the wire —
  because request/response records map to entities explicitly rather than being the
  entities.

- **Seed timestamp uses `CURRENT_TIMESTAMP`, not `now()`.** H2's `now()` is
  `LOCALTIMESTAMP` with no time zone, which would make the seeded `harald` agent's
  timestamps depend on the server's local clock. `CURRENT_TIMESTAMP` is
  timezone-aware on both engines.

- **The `clarification` ↔ `clarification_option` circular foreign key is added via
  `ALTER TABLE ... ADD CONSTRAINT`** after both tables exist, since neither can
  reference the other at `CREATE TABLE` time. Works identically on PostgreSQL and H2.

- **`ProgramRepository.findAllCounts()` selects nothing but an identifier (cast to
  varchar) and four `COUNT(*)` values** — no timestamp, no other column. Discovered
  the hard way: H2 returns `timestamp with time zone` as `OffsetDateTime` and `uuid`
  as `byte[]` from a native query; PostgreSQL returns `Instant` and a real UUID. A
  native-query projection selecting a timestamp passed every H2 test and threw 500 on
  real PostgreSQL. See `ProgramOverviewPostgresIT` — it exists specifically to
  re-catch this class of bug, and its Javadoc says so.

## Backend structure

- **`ProgramLookup` is its own service**, not a method on `ProgramService`. Once the
  program detail response needed clarifications, folding lookup into
  `ProgramService` would have made it depend on `ClarificationService`, which needs
  to read a program to answer a clarification — a bean cycle Spring refuses to
  construct. `ProgramLookup` is the shared, dependency-free resolver both sides use.

- **`ApiExceptionHandler` extends `ResponseEntityExceptionHandler`**, not a bare
  `@RestControllerAdvice` with only `@ExceptionHandler(Exception.class)` as a
  catch-all. Without the parent class, Spring's own failures (wrong HTTP method,
  unsupported media type) were being flattened by the catch-all into a 500 instead of
  their correct status (405, etc).

- **Checkstyle's `IllegalImport` bans importing
  `org.springframework.beans.factory.annotation.Autowired` and any `lombok.*`
  package**, turning two of the workspace's non-negotiable rules (constructor
  injection, no Lombok) into a build failure rather than a review comment.

- **Checkstyle's `HiddenField` is scoped to local variables only**
  (`VARIABLE_DEF, PATTERN_VARIABLE_DEF`), not constructor or setter parameters. The
  default configuration flagged idiomatic domain mutators like
  `describe(String description) { this.description = description; }` as violations;
  renaming the parameter to satisfy the linter would have made the code worse for no
  safety gained.

- **Tests use constructor injection too**, via
  `spring.test.constructor.autowire.mode=all` in
  `src/test/resources/junit-platform.properties`. Without it, Spring Boot 4's test
  slices require `@Autowired` on the test class constructor, which would have made
  the "constructor injection everywhere" rule inconsistent between main and test
  code.

## Testing

- **Testcontainers was added for a `*IT` layer**, even though the resolved Open
  Question 4 said "keep H2 for v1." Reopened after H2 let through the native-query
  bug described above; H2 cannot catch a class of bug where the two JDBC drivers
  disagree about Java types for the same column. `mvn test` still runs on H2 only and
  needs no Docker; `mvn verify` additionally runs `*IT` against real PostgreSQL and
  does need Docker.

- **A Playwright suite was added in `e2e/`**, overriding the prompt's explicit "No
  frontend tests in v1." Justified the same way as the item above: a real browser
  against the built bundle caught the `produces`/Blob bug that 44 backend tests could
  not see, because MockMvc never touches the generated client. Kept as a separate
  `e2e/` project with its own `package.json`, not inside `frontend/`, so the Angular
  app itself still carries no test dependencies.

## Build and delivery

- **`./up.sh` is the documented entry point, not `docker compose up -d`.** The
  backend build produces the OpenAPI spec, which is what the frontend's client
  generator reads; that file is committed to neither the repo nor an image
  (per the prompt's own instruction). A clean checkout therefore cannot run
  `docker compose up -d` or `npm run build` on their own — both need the backend
  built first. `up.sh` runs `mvn verify` then `docker compose up -d --wait`, in that
  order, and is what the definition of done actually verifies against.

- **The Angular client is generated by the `openapi-generator-maven-plugin` in
  `prepare-package`, not by an npm script.** `openapi-generator-cli` is a Java
  program; the `node:24-alpine` build image has no JVM, and fetching the generator
  jar over npm at container-build time hit the same TLS-intercepting proxy that broke
  the Google Fonts fetch (see Environment below). Maven already has the JVM and
  already owns the spec.

- **The frontend serves from nginx with a runtime `config.json`**, written by the
  container entrypoint from `PROGRAM_DASHBOARD_API_BASE_URL` and
  `PROGRAM_DASHBOARD_HUMAN_AGENT_ID`. An Angular production build is static; without
  this mechanism the API URL and human agent id would have to be baked in at image
  build time, defeating `.env` as the single source of configuration.

- **The backend is reached by the Docker Compose service name `postgres:5432`
  inside the container network**, not literally `localhost`. This resolves a direct
  contradiction in the original prompt between "connection strings point at
  localhost, no container-DNS hostnames" and "Docker Compose brings up Postgres,
  backend and frontend with one command" plus "must work on Linux and on Windows with
  WSL" — `network_mode: host` (the only way to make `localhost` literally true)
  doesn't work on Docker Desktop for WSL. Every port is still published to
  `127.0.0.1` only, which is what actually delivers the "not exposed to the LAN"
  guarantee the prompt cares about.

- **Roboto and the Material icon font are bundled from npm**
  (`@fontsource/roboto`, `material-icons`), not linked from `fonts.googleapis.com`.
  Angular's production build inlines Google Fonts at build time by fetching them
  over HTTPS; that fetch failed inside the Docker build with "self-signed
  certificate in certificate chain" (see Environment below). Bundling also means the
  dashboard works with no internet and makes no third-party requests, which is a
  better fit for a localhost-only tool anyway.

- **Routes are lazy-loaded** (`loadComponent`). Not required by the prompt, but the
  markdown renderer and its HTML sanitiser only belong on the detail page; loading
  them eagerly pushed the initial bundle over Angular's default 500 KB budget.

## Answers to the original prompt's five Open Questions

Recorded here because the prompt lists them as open; they are now closed as follows.

1. **Polling interval**: 15s on the overview only, as proposed. Detail page reloads
   on navigation and after every write — deliberately no timer there, so nothing
   races the answer form.
2. **Milestone completion authority**: any registered agent, as proposed.
   `completedByAgentId` is the audit trail.
3. **Future start/stop fields**: not added, as proposed.
4. **Repository test engine**: amended from the proposal. H2 in PostgreSQL mode for
   `*Test`, **plus Testcontainers for `*IT`** — see Testing above for why the
   original "H2 only" answer was reopened.
5. **OpenAPI client generation mechanism**: amended from the proposal. Not an npm
   script reading a file the backend writes; the `openapi-generator-maven-plugin`
   generates it directly as part of `mvn verify` — see Build and delivery above.

## Environment (not a design decision, but shapes several above)

These aren't project decisions so much as facts about the machine this was built on
that a future session will rediscover if not warned:

- The machine-wide `~/.m2/settings.xml` mirrors Maven Central to a corporate
  Artifactory whose token expired 2026-08-11. The project resolves straight from
  Central instead, via a project-local `.mvn/settings.xml` + `.mvn/maven.config`.
  The global file is untouched.
- The default `java`/`mvn` on this machine run on Java 8, even though JDK 25 is
  installed. `maven-enforcer-plugin` fails the build with an explicit message rather
  than a confusing compile error; `up.sh` locates a JDK 25 automatically.
- The dev shell this was built from runs inside a Docker container itself
  (`devbox-hvdc`), so containers started via the mounted Docker socket are siblings
  on the WSL host, not children reachable at `localhost`. This is why `up.sh` uses
  `docker compose up -d --wait` (container-side healthchecks) instead of curling a
  published port, and why Testcontainers needs
  `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` when run from in there.
- Ports 8080 and 4200–4519 are reserved by that same container. `.env.example`
  keeps the prompt's documented 8080/4200; the git-ignored local `.env` uses 8081
  and 4520 instead.
- The npm registry available here has an apparent publish-date cutoff; installing
  a dependency's unpinned `latest` can fail with `ETARGET` for packages published
  after that date. Pin to the newest version published before it.
