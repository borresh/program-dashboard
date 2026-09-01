#!/bin/sh
# Bring up the whole stack from a clean checkout.
#
# Two steps have to happen in order, which is why this script exists rather than
# a bare `docker compose up -d`: the backend build produces the OpenAPI spec that
# the frontend's client generator reads, and that spec is committed to neither
# repository nor image.
#
# POSIX sh on purpose: this has to behave the same on Linux and on Windows/WSL.
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_ROOT"

log() { printf '\n==> %s\n' "$1"; }
fail() { printf '\nError: %s\n' "$1" >&2; exit 1; }

# --- .env -------------------------------------------------------------------
[ -f .env ] || fail ".env is missing. Copy .env.example to .env and fill it in:
    cp .env.example .env"

# --- JDK 25 -----------------------------------------------------------------
# The default JVM on a machine that also does Java work is frequently older, so
# locate a suitable one rather than failing on something the user cannot see.
java_spec_version() {
    "$1" -XshowSettings:properties -version 2>&1 \
        | sed -n 's/.*java\.specification\.version = \([0-9][0-9]*\).*/\1/p' \
        | head -1
}

find_jdk25() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        version=$(java_spec_version "${JAVA_HOME}/bin/java")
        if [ -n "$version" ] && [ "$version" -ge 25 ]; then
            printf '%s' "$JAVA_HOME"
            return 0
        fi
    fi
    for candidate in /usr/lib/jvm/java-25-openjdk-* /usr/lib/jvm/jdk-25* \
                     /usr/lib/jvm/temurin-25* "$HOME"/.sdkman/candidates/java/25*; do
        if [ -x "${candidate}/bin/java" ]; then
            version=$(java_spec_version "${candidate}/bin/java")
            if [ -n "$version" ] && [ "$version" -ge 25 ]; then
                printf '%s' "$candidate"
                return 0
            fi
        fi
    done
    return 1
}

JAVA_HOME=$(find_jdk25) || fail "no JDK 25 or later found.
The backend targets Java 25. Install one, then either set JAVA_HOME or make it
the default JVM:
    export JAVA_HOME=/path/to/jdk-25"
export JAVA_HOME
log "Using JDK at $JAVA_HOME"

# --- Testcontainers ---------------------------------------------------------
# The *IT tests run against a real PostgreSQL. Inside a dev container the Docker
# socket is the host's, so those containers publish their ports on the host rather
# than on this container's loopback; Testcontainers has to be told where to reach
# them. Outside a container the default of localhost is already correct.
if [ -f /.dockerenv ] && [ -z "${TESTCONTAINERS_HOST_OVERRIDE:-}" ]; then
    TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal
    export TESTCONTAINERS_HOST_OVERRIDE
    log "Running inside a container; Testcontainers will use $TESTCONTAINERS_HOST_OVERRIDE"
fi

# --- Backend ----------------------------------------------------------------
# `verify` does three things in order that later steps depend on: it runs the tests,
# it exports the OpenAPI specification the backend owns, and it generates the Angular
# API client into frontend/src/app/api from that specification. Neither the spec nor
# the client is committed, so this cannot be skipped on a fresh checkout.
#
# Resolution goes straight to Maven Central through .mvn/settings.xml; see the
# comment in that file for why the machine-wide settings are bypassed.
log "Building backend, exporting the API contract and generating the client"
mvn -f backend/pom.xml verify

# --- Stack ------------------------------------------------------------------
# --wait blocks until every service with a healthcheck reports healthy, using the
# checks that run inside the containers. Polling the published port from here
# instead would assume this script runs on the same host the ports are published
# to, which is not true when it is run from inside a dev container.
log "Starting containers"
docker compose up -d --build --wait

SERVER_PORT=$(sed -n 's/^SERVER_PORT=\(.*\)$/\1/p' .env | tail -1)
SERVER_PORT=${SERVER_PORT:-8080}
FRONTEND_PORT=$(sed -n 's/^FRONTEND_PORT=\(.*\)$/\1/p' .env | tail -1)
FRONTEND_PORT=${FRONTEND_PORT:-4200}

printf '\nProgram Dashboard is up.\n'
printf '  Dashboard: http://localhost:%s\n' "$FRONTEND_PORT"
printf '  API:       http://localhost:%s/api\n' "$SERVER_PORT"
printf '  Health:    http://localhost:%s/actuator/health\n\n' "$SERVER_PORT"
