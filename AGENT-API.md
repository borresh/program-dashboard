# AGENT-API.md

How an AI agent uses the Program Dashboard. Written to be pasted into an agent's
context; you should not need the OpenAPI specification to work from this.

Base URL: `http://localhost:8080` by default. Every request and response is JSON.

## Rules that apply to everything

- **Every write carries the id of a registered agent.** Register first.
- **An optional value is absent from a response, never `null`.** In particular
  `progress` is missing when a program has no milestones — that means *unknown*, not
  zero.
- **Every error is RFC 7807 Problem Details.** The `detail` names the offending
  input and what was expected, so read it instead of guessing:

  ```json
  {"type":"about:blank","title":"Clarification already answered","status":409,
   "detail":"Clarification 3a947b10-... was answered by 'harald' at 2026-08-31T09:14:03Z"}
  ```

- **A program is never deleted.** Set its status to `ABANDONED` instead.

## 1. Register yourself

Idempotent by name, so send it at the start of every session without checking. A new
name returns `201`, an existing one returns `200` with the record unchanged.

```sh
curl -s -X POST localhost:8080/api/agents \
  -H 'Content-Type: application/json' \
  -d '{"name":"opencode-implementer","role":"IMPLEMENTER"}'
```

```json
{"id":"7d1f8c2e-...","name":"opencode-implementer","role":"IMPLEMENTER",
 "registeredAt":"2026-09-01T09:00:00Z","lastSeenAt":"2026-09-01T09:00:00Z"}
```

`role` is `IMPLEMENTER`, `REVIEWER` or `HUMAN`. Keep the returned `id`; every write
below needs it. An unknown agent id on any write is a `404` naming the id.

## 2. Register a program from its prompt

Do this before any code exists. `slug` must match `^[a-z0-9]+(-[a-z0-9]+)*$` and is
how you address the program afterwards. The `milestones` list is optional and keeps
the order you give it — a "Build order" section from the prompt goes straight in.

```sh
curl -s -X POST localhost:8080/api/programs \
  -H 'Content-Type: application/json' -d '{
    "slug":"program-dashboard",
    "name":"Program Dashboard",
    "description":"Tracks the programs I build with AI agents",
    "initialPrompt":"# program-dashboard\n\nA browser dashboard and REST API...",
    "createdByAgentId":"7d1f8c2e-...",
    "milestones":[
      {"title":"Skeleton","description":"Compose, schema, health"},
      {"title":"Agents, programs and milestones"}]}'
```

Returns `201` with a `Location` header. The program starts as `IDEA`. A duplicate
slug is a `409` naming the program that already has it.

## 3. Ask a clarification question

This is the point of the dashboard. When you hit an ambiguity, ask instead of
guessing. Propose options when you have them — it is much faster for me to pick one
than to write an answer from scratch, and your rationale tells me what you were
weighing.

```sh
curl -s -X POST localhost:8080/api/programs/program-dashboard/clarifications \
  -H 'Content-Type: application/json' -d '{
    "askedByAgentId":"7d1f8c2e-...",
    "question":"Can any registered agent complete a milestone, or only the creating agent?",
    "context":"R10 records completedByAgentId but never says who is allowed to set it.",
    "blocking":true,
    "proposedOptions":[
      {"label":"Any registered agent",
       "rationale":"A second agent often picks up work mid-project, and completedByAgentId is already an audit trail."},
      {"label":"Only the creating agent",
       "rationale":"Prevents a stray script from marking work done."}]}'
```

Returns `201` with the clarification id and the ids of the options. Set
`blocking: true` when you genuinely cannot continue: the dashboard makes those
impossible to miss. It never prevents any other operation — you can keep working on
something else while you wait.

## 4. Poll for the answer

```sh
curl -s localhost:8080/api/clarifications/3a947b10-...
```

```json
{"id":"3a947b10-...","status":"ANSWERED",
 "answerText":"Option 1. completedByAgentId is enough of an audit trail.",
 "chosenOption":{"id":"...","position":0,"label":"Any registered agent","rationale":"..."},
 "answeredBy":{"name":"harald","role":"HUMAN"},
 "answeredAt":"2026-09-01T09:14:03Z"}
```

`status` is `OPEN` until it is answered. Poll every 30 seconds or so; there are no
notifications. To find everything still waiting on a human:

```sh
curl -s 'localhost:8080/api/clarifications?status=OPEN&program=program-dashboard'
```

Both query parameters are optional.

## 5. Report a milestone

Add one:

```sh
curl -s -X POST localhost:8080/api/programs/program-dashboard/milestones \
  -H 'Content-Type: application/json' \
  -d '{"actorAgentId":"7d1f8c2e-...","title":"Clarifications end to end","position":2}'
```

Complete one. Any registered agent may complete any milestone; who did it is
recorded:

```sh
curl -s -X PATCH localhost:8080/api/milestones/<id> \
  -H 'Content-Type: application/json' \
  -d '{"actorAgentId":"7d1f8c2e-...","complete":true}'
```

Completing an already-complete milestone is a `409` naming who completed it and
when. Send `"complete": false` to reopen one — that is allowed, because undoing a
mistake has to stay possible.

## 6. Change a program's status

```sh
curl -s -X PATCH localhost:8080/api/programs/program-dashboard \
  -H 'Content-Type: application/json' \
  -d '{"actorAgentId":"7d1f8c2e-...","status":"ACTIVE"}'
```

`IDEA`, `ACTIVE`, `PAUSED`, `DONE`, `ABANDONED`. Only the fields you send change.

## Answering questions about a program

If a human asks "what's the status of X?", one call answers it:

```sh
curl -s localhost:8080/api/programs/program-dashboard
```

That returns the prompt, the milestones with progress, and the clarifications with
open ones first. For the history, `GET /api/programs/program-dashboard/activity`
returns entries newest first.

## Full endpoint list

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
GET    /api/clarifications/{id}
POST   /api/clarifications/{id}/answer

GET    /api/programs/{idOrSlug}/activity?page=&size=
GET    /actuator/health
```

---

## Snippet for a tracked project's own AGENTS.md

Paste this into the `AGENTS.md` of any project you want reported to the dashboard,
filling in the two placeholders.

````markdown
## Reporting to the Program Dashboard

This project is tracked at `http://localhost:8080`. Keep it up to date; it is how
the human sees where this project stands, and how you ask them questions without
losing the answer when the session ends.

**Once per session,** register yourself and keep the returned `id`:

```sh
curl -s -X POST localhost:8080/api/agents -H 'Content-Type: application/json' \
  -d '{"name":"<your-agent-name>","role":"IMPLEMENTER"}'
```

**When you hit an ambiguity, ask instead of guessing.** Propose options with the
reasoning behind each, and mark it blocking if you cannot continue:

```sh
curl -s -X POST localhost:8080/api/programs/<program-slug>/clarifications \
  -H 'Content-Type: application/json' -d '{
    "askedByAgentId":"<your-agent-id>",
    "question":"...",
    "context":"which requirement is ambiguous, and why",
    "blocking":true,
    "proposedOptions":[{"label":"...","rationale":"..."}]}'
```

Then poll `GET /api/clarifications/{id}` until `status` is `ANSWERED`. Carry on with
unrelated work while you wait.

**When you finish a milestone,** say so:

```sh
curl -s -X PATCH localhost:8080/api/milestones/<milestone-id> \
  -H 'Content-Type: application/json' \
  -d '{"actorAgentId":"<your-agent-id>","complete":true}'
```

Every error is RFC 7807: read `detail`, it names what was wrong and what was
expected. Full guide: `AGENT-API.md` in the program-dashboard repository.
````
