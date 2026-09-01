import { InjectionToken } from '@angular/core';

/**
 * Configuration that is only known when the container starts, not when the bundle is
 * built. An Angular build is static, so these values are fetched from `config.json`
 * before bootstrap; the nginx entrypoint writes that file from the environment.
 */
export interface RuntimeConfig {
  /** Base URL of the backend REST API, without a trailing slash. */
  readonly apiBaseUrl: string;
  /** Identifier of the seeded HUMAN agent that answers submitted from the browser are attributed to. */
  readonly humanAgentId: string;
}

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('RUNTIME_CONFIG');

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Fetches and validates `config.json`. Rejects rather than falling back to a default:
 * a dashboard pointing at the wrong backend, or attributing answers to the wrong agent,
 * is worse than one that refuses to start.
 */
export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const response = await fetch('config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(
      `config.json could not be loaded (HTTP ${response.status}). The container entrypoint ` +
        `writes this file from PROGRAM_DASHBOARD_API_BASE_URL and PROGRAM_DASHBOARD_HUMAN_AGENT_ID.`,
    );
  }
  return parseRuntimeConfig(await response.json());
}

export function parseRuntimeConfig(raw: unknown): RuntimeConfig {
  if (typeof raw !== 'object' || raw === null) {
    throw new Error(`config.json must contain a JSON object, but contained ${typeof raw}.`);
  }

  const candidate = raw as Record<string, unknown>;
  const apiBaseUrl = candidate['apiBaseUrl'];
  const humanAgentId = candidate['humanAgentId'];

  if (typeof apiBaseUrl !== 'string' || apiBaseUrl.length === 0) {
    throw new Error(
      `config.json field "apiBaseUrl" must be a non-empty string, but was ${JSON.stringify(apiBaseUrl)}. ` +
        `Expected something like "http://localhost:8080".`,
    );
  }
  if (typeof humanAgentId !== 'string' || !UUID_PATTERN.test(humanAgentId)) {
    throw new Error(
      `config.json field "humanAgentId" must be a UUID, but was ${JSON.stringify(humanAgentId)}. ` +
        `Expected the seeded HUMAN agent id from PROGRAM_DASHBOARD_HUMAN_AGENT_ID.`,
    );
  }

  return {
    apiBaseUrl: apiBaseUrl.replace(/\/+$/, ''),
    humanAgentId,
  };
}
