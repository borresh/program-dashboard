import { existsSync } from 'node:fs';

// The API client is generated from the backend's OpenAPI specification and is committed
// to neither the repository nor an image. Without this check the failure surfaces as a
// pile of unresolved-import errors that say nothing about the cause.
if (!existsSync(new URL('../src/app/api/index.ts', import.meta.url))) {
  console.error(`
  The generated API client is missing: frontend/src/app/api

  It is produced by the backend build, from the OpenAPI specification the backend
  owns. Run the backend build first, or use the wrapper that does both:

      ./up.sh                                  (from the project root)
      mvn -f backend/pom.xml verify            (backend only)
`);
  process.exit(1);
}
