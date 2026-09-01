#!/bin/sh
# Writes the runtime configuration that main.ts fetches before bootstrap.
#
# An Angular build is static, so these two values cannot be baked in without
# rebuilding the image. Generating the file here means one image serves any
# environment, and .env stays the single source of truth.
set -eu

: "${PROGRAM_DASHBOARD_API_BASE_URL:?must be set, for example http://localhost:8080}"
: "${PROGRAM_DASHBOARD_HUMAN_AGENT_ID:?must be set to the seeded HUMAN agent id}"

cat > /usr/share/nginx/html/config.json <<JSON
{
  "apiBaseUrl": "${PROGRAM_DASHBOARD_API_BASE_URL}",
  "humanAgentId": "${PROGRAM_DASHBOARD_HUMAN_AGENT_ID}"
}
JSON

echo "config.json written: apiBaseUrl=${PROGRAM_DASHBOARD_API_BASE_URL} humanAgentId=${PROGRAM_DASHBOARD_HUMAN_AGENT_ID}"
