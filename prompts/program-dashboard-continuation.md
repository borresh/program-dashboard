# program-dashboard — continuation

Picking up a finished v1. This file does not restate the project documentation; read
it.

## Where things stand

`program-dashboard/` in this workspace is its own git repo. v1 is complete: three
commits, clean tree, every requirement R1–R23 of the original prompt implemented.

The success check works and is covered by tests: an agent posts a clarification
question through the REST API, I answer it in the browser, the agent reads the
answer back on its next poll.

- Java 25, Spring Boot 4.1.1, PostgreSQL 16, Flyway, springdoc 3.1.0
- Angular 22, Angular Material 22, Tailwind 4, OpenAPI client generated from the backend
- 48 backend tests (44 on H2, 4 on real PostgreSQL) and 5 Playwright checks, all green

## Read these first, in this order

1. `program-dashboard/README.md` — what it is, how to run it, what is deliberately
   unbuilt
2. `program-dashboard/AGENTS.md` — commands, stack, structure, non-negotiable rules,
   anti-patterns, and a "things that will bite you" section
3. `program-dashboard/DECISIONS.md` — every place the built system differs from the
   original prompt, and why
4. `program-dashboard/AGENT-API.md` — only if you are calling the API

The original specification is `prompts/program-dashboard.md`. Where it and
`DECISIONS.md` disagree, `DECISIONS.md` is what was agreed and built.

## Objective for this session

**Use it, then harden it. No new features.**

The dashboard works but has never been used in anger. In order:

1. **Put real data in it.** Register the programs in this workspace, from whatever
   prompt or README each one has. An agent reading a file and POSTing it does not
   violate the "reads nothing from disk" rule — that rule is about the application,
   not about you.
2. **Have it track itself.** program-dashboard is a program.
3. **Then fix what gets in my way.** Report friction you notice. Do not act on it
   unsolicited — ask me first, and I will tell you what to fix.

## Known gaps — observations, not a to-do list

Do not build any of these without asking.

- **There is no way to register a program from the browser.** The API is the only
  route in. Scenario 3 of the original prompt implies a form, but no requirement
  specified one and R21/R22 describe only the two existing screens.
- No cross-program view of open questions; the overview shows per-program counts
  and answering happens on the detail page.
- Milestones cannot be edited or reordered from the browser, only through the API.
- No search and no sorting controls; overview order is fixed (blocking first, then
  most recently updated).
- The activity timeline exists only on the detail page.

## Environment — this will waste your day otherwise

- **JDK 25 is installed but is not the default.** `java` is Zulu 8. Use
  `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`. `./up.sh` finds one itself; bare
  `mvn` does not, and fails with a clear enforcer message.
- **Maven deliberately ignores the machine-wide settings.** `~/.m2/settings.xml`
  mirrors Central to a corporate Artifactory whose token expired 2026-08-11. The
  project uses `.mvn/settings.xml` (no mirrors) via `.mvn/maven.config`. Do not
  remove it, and do not edit the global file.
- **This shell runs inside the `devbox-hvdc` container.** Containers you start are
  siblings on the WSL host, so:
  - You cannot curl `localhost:8081` or `localhost:4520` from here. Use
    `docker compose up -d --wait`, run checks in a container on the
    `program-dashboard_default` network, or reach the host as
    `host.docker.internal`.
  - Bind mounts resolve on the host, not here. Pipe scripts in on stdin, or
    `docker cp` them in.
  - `mvn verify` needs `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`;
    `up.sh` sets it automatically when it detects `/.dockerenv`.
- **Ports 8080 and 4200–4519 belong to the devbox.** The git-ignored `.env` uses
  8081 and 4520; `.env.example` keeps the documented 8080/4200. Substitute in any
  curl you copy from the docs.
- **The npm registry here has a publish-date cutoff**, roughly 2026-08-28.
  `npm i foo@latest` can fail with `ETARGET` for a package published after that;
  pin to the newest version published before it.
- **HTTPS inside a Docker build goes through a TLS-intercepting proxy.** Build-time
  fetches fail with "self-signed certificate in certificate chain" — which is why
  the fonts are bundled from npm rather than fetched from Google at build time.

## What must stay unbuilt

Review handoff between agents (v1.1; only the `REVIEWER` enum value exists),
starting and stopping programs or anything that executes commands or controls
containers (v2), deleting a program, live log streaming, authentication,
notifications, metrics and charts, chat transcripts, mobile layout, CI, agent-to-
agent messaging, and anything that reads from disk. If one of these looks easy, it
is still out — ask first.

## Verify before calling anything done

```sh
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
mvn -f backend/pom.xml verify
npm --prefix frontend ci && npm --prefix frontend run lint && npm --prefix frontend run build
./up.sh
./e2e.sh        # then ./up.sh again, to restore the normal origin configuration
```

`mvn verify` regenerates the API client into `frontend/src/app/api`. It is
git-ignored, and the frontend build refuses to start without it.

## How to work with me

Before writing code: restate in a few lines what you are about to do, list the
assumptions that are not settled, and batch the blocking questions. Then wait.

While working: anything in `DECISIONS.md` is settled — if you believe one is wrong,
say so and stop rather than quietly changing it. If a requirement turns out to be
contradictory or impossible, stop and ask. If you need a decision I have not
covered, ask. Guessing is the failure mode this prompt exists to prevent.

Never commit unless I ask.
