import { defineConfig } from '@playwright/test';

/**
 * Runs against a stack that is already up, so it exercises the built bundle and the real
 * nginx and backend containers rather than a dev server.
 *
 * The two URLs come from the environment because where the stack is reachable depends on
 * where the browser runs: inside the compose network the services answer to their own
 * names, from a host they answer on published ports. `../e2e.sh` sets both.
 */
export default defineConfig({
  testDir: '.',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  reporter: [['list']],
  // A shared dashboard has shared state; parallel runs would fight over the same programs.
  workers: 1,
  use: {
    baseURL: process.env['PD_UI_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
  },
});
