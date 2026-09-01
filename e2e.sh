#!/bin/sh
# Run the browser checks against the real containers.
#
# Everything runs inside the compose network, including the browser, so this behaves the
# same on a host and inside a dev container — where published ports are not reachable.
#
# The stack is left running afterwards, but with the test-only origin configuration from
# docker-compose.e2e.yml. Run ./up.sh to put it back.
set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$PROJECT_ROOT"

[ -f .env ] || { echo "Error: .env is missing. Copy .env.example to .env." >&2; exit 1; }

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.e2e.yml"

printf '\n==> Starting the stack with the test origin configuration\n'
$COMPOSE up -d --build --wait postgres backend frontend

printf '\n==> Running the browser checks\n'
$COMPOSE run --rm --build e2e

printf '\nBrowser checks passed. Run ./up.sh to restore the normal configuration.\n'
