# AGENTS.md — program-dashboard

Instructions for agents working on **this project**. Composes with the workspace
baseline; where the two disagree, this file wins.

See `DECISIONS.md` for every place this project deliberately differs from the
original prompt at `prompts/program-dashboard.md`, and why. Where the prompt and
`DECISIONS.md` disagree, `DECISIONS.md` is what was actually built.

## What this is

A browser dashboard and REST API for tracking programs built with AI agents. Both
the browser and the agents use the same REST API — there is no separate
backend-for-frontend. The application reads nothing from disk: no git integration,
no scanning of projects. It starts empty and only knows what it was told through
the API.

## Commands

Set `JAVA_HOME` to a JDK 25 first. `./up.sh` finds one itself; Maven does not.

```sh
mvn -f backend/pom.xml verify     # tests, Checkstyle, OpenAPI export, client generation
mvn -f backend/pom.xml test       # unit tests only, no Docker needed

npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run build
npm --prefix frontend run format

./up.sh                           # build the backend, then bring the stack up
./e2e.sh                          # browser checks against the running containers
docker compose down               # stop; add -v to discard the database
```

`mvn verify` needs Docker: the `*IT` tests run against a real PostgreSQL through
Testcontainers. Inside a dev container, export
`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` first.

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Maven |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway |
| Frontend | Angular 22, standalone, signals, zoneless |
| Components | Angular Material for anything with behaviour |
| Layout | Tailwind 4, utilities only, preflight disabled |
| API contract | REST; the backend owns the OpenAPI spec, the Angular client is generated from it |
| Errors | RFC 7807 Problem Details from one `@RestControllerAdvice` |
| Quality gates | Checkstyle on the backend, ESLint + Prettier on the frontend |
| Browser checks | Playwright, in `e2e/`, run in a container |
| Not used | Lombok. Java records and compact constructors cover the need |

There is no Java auto-formatter. Checkstyle enforces rules, not formatting. That
asymmetry with Prettier is deliberate — do not "fix" it.

## Structure

Organise by feature, not by technical layer.

```
backend/src/main/java/no/borresh/programdashboard/
  config/           typed configuration records, CORS, clock, OpenAPI bean
  common/           ProblemDetail advice, ResourceNotFoundException, ConflictException
  agent/            controller, service, repository, entity, DTOs
  program/          Program, Milestone, ProgramLookup, services, controllers
  clarification/    Clarification, ClarificationOption, service, controller
  activity/         ActivityEntry, ActivityService, ActivityController
frontend/src/app/
  programs/         overview page, detail page, activity timeline
  clarifications/   clarification card and answer form
  shared/           markdown rendering, problem-detail formatting
  core/             runtime configuration token
  api/              generated OpenAPI client, git-ignored, never hand-written
e2e/                Playwright browser checks
```

## Non-negotiable rules

1. **Constructor injection only.** Never field injection. A class must be
   constructible in a test without starting Spring. Tests use constructor injection
   too, via `spring.test.constructor.autowire.mode=all`. Checkstyle bans the
   `@Autowired` import outright.
2. **API types are separate from persistence types.** A JPA entity is never a
   request body and never a response body. Request and response records are mapped
   explicitly.
3. **Controllers bind and delegate.** No rules, no queries.
4. **No `any` in TypeScript**, and no casts that route around the type system. Use
   `unknown` and narrow it.
5. **Abstractions appear at the second caller, not the first.** One implementation
   needs no interface.
6. **Introduce a pattern on the third occurrence** of the same problem, not before.
7. **When two designs are equally good, pick the one that is easier to delete.**
8. **Every error message names the offending input and what was expected.**
   "Invalid request" is not an error message.
9. **Every write carries an actor.** `actorAgentId`, or the named equivalent, is
   required on every mutating endpoint; it fills the activity log and updates
   `lastSeenAt`.
10. **Optional values are absent from responses, never null.** One rule for the
    whole API — see the note below.

## Anti-patterns

| Anti-pattern | Why it hurts | Instead |
|---|---|---|
| Lombok | Build-time generation for something the language does natively | Records, compact constructors, explicit accessors |
| Field injection | Hides dependencies; the class cannot be built in a test without Spring | Constructor injection, `final` fields |
| JPA entities as DTOs | Leaks the schema into the public contract; a column rename becomes a breaking API change | Separate request and response records |
| Logic in controllers | Untestable without the web layer, duplicated at the second entry point | Controller delegates to a service |
| `any` in TypeScript | Disables the one guarantee the language gives, and spreads to callers | `unknown` plus narrowing |
| Pre-v17 Angular (`NgModule`, `*ngIf`, `*ngFor`, `@Input`) | Superseded by standalone components, built-in control flow and signal inputs | `input()`, `output()`, `@if`, `@for … track` |
| Speculative abstraction | Interfaces and seams with one caller cost indirection now and guess wrong about the future | Wait for the second caller |
| Read-then-write on a state transition | Two actors both pass the check and the second write wins silently | One conditional `UPDATE … WHERE status = …`, then read only to explain a rejection |

## Things that will bite you

- **Spring Boot 4 uses Jackson 3.** The bean is `tools.jackson.databind.json.JsonMapper`;
  `com.fasterxml.jackson.databind.ObjectMapper` is not registered. Jackson 2 is on the
  classpath only because springdoc drags it in.
- **Spring Boot 4 splits auto-configuration into per-technology modules.**
  `flyway-core` alone does nothing; `spring-boot-flyway` is what activates it. Test
  slices are likewise separate — `@AutoConfigureMockMvc` needs
  `spring-boot-starter-webmvc-test`.
- **Declare `produces = application/json` on every controller.** Without it the
  OpenAPI document says the response is a wildcard media type, the generated client
  cannot tell it is JSON, and it reads every body as a `Blob`. No backend test can
  see this; the browser checks exist partly because of it.
- **H2 and PostgreSQL return different Java types for the same column.** A
  `timestamp with time zone` arrives as `OffsetDateTime` from H2 and `Instant` from
  PostgreSQL; `uuid` comes back from H2 as `byte[]` in a native query. Keep native
  queries to identifiers and counts, and cover them with a `*IT` test.
- **The migration must run on both engines.** No `JSONB`, no arrays, no
  `ON CONFLICT`, no `gen_random_uuid()`. UUIDs are generated in Java.
- **`position` and `type` are function names in HQL.** The columns are `sort_order`
  and `entry_type`; the API field names are unaffected because the DTOs map
  explicitly.

## The nulls rule

An optional value is **absent** from a response, never `null`
(`spring.jackson.default-property-inclusion: non_null`). This exists because
springdoc cannot express a nullable object reference correctly in OpenAPI 3.1, so a
response carrying nulls produces a generated client that claims fields are
`undefined` while the wire sends `null`.

The one that matters: `progress` is **absent** when a program has no milestones. It
is never `0`. A program nobody has broken down yet must not look like one nobody has
started.

## Testing

Test the state rules that would hurt to get wrong.

- `*Test` — H2 in PostgreSQL mode, no Docker, fast. Schema, state rules, error shapes.
- `*IT` — real PostgreSQL through Testcontainers. Anything touching native SQL or a
  driver-decided Java type, plus `SuccessCheckIT`, which walks the whole product.
- `e2e/` — Playwright against the built bundle and the real containers. The only
  level that sees the generated client.

If `SuccessCheckIT` fails, the product does not work, whatever else passes.
