const API = process.env['PD_API_URL'] ?? 'http://localhost:8080';

/** Seeded by V1__init.sql, and what the browser attributes answers to. */
export const HUMAN_AGENT_ID = '00000000-0000-0000-0000-000000000001';

export interface Seeded {
  readonly slug: string;
  readonly emptySlug: string;
  readonly agentId: string;
  readonly blockingClarificationId: string;
  readonly firstOptionId: string;
}

async function call<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(API + path, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`${method} ${path} -> ${response.status} ${await response.text()}`);
  }
  return (await response.json()) as T;
}

/**
 * Creates a program with a unique slug, so a run neither depends on a clean database nor
 * leaves the next run with a duplicate-slug conflict.
 */
export async function seedProgram(): Promise<Seeded> {
  const stamp = Date.now().toString(36);
  const slug = `e2e-dashboard-${stamp}`;
  const emptySlug = `e2e-empty-${stamp}`;

  const agent = await call<{ id: string }>('POST', '/api/agents', {
    name: 'e2e-implementer',
    role: 'IMPLEMENTER',
  });

  await call('POST', '/api/programs', {
    slug,
    name: `E2E Dashboard ${stamp}`,
    initialPrompt:
      '# e2e-dashboard\n\nA browser dashboard **and** REST API.\n\n' +
      '```sh\ncurl -s localhost:8080/api/programs\n```\n',
    createdByAgentId: agent.id,
    milestones: [
      { title: 'Skeleton' },
      { title: 'Agents and programs' },
      { title: 'Clarifications' },
      { title: 'Activity log' },
    ],
  });

  const detail = await call<{ milestones: { id: string }[] }>('GET', `/api/programs/${slug}`);
  for (const milestone of detail.milestones.slice(0, 2)) {
    await call('PATCH', `/api/milestones/${milestone.id}`, {
      actorAgentId: agent.id,
      complete: true,
    });
  }

  const blocking = await call<{ id: string; proposedOptions: { id: string }[] }>(
    'POST',
    `/api/programs/${slug}/clarifications`,
    {
      askedByAgentId: agent.id,
      question: 'Can any registered agent complete a milestone, or only the creating agent?',
      context: 'R10 records `completedByAgentId` but never says who may set it.',
      blocking: true,
      proposedOptions: [
        { label: 'Any registered agent', rationale: 'A second agent often picks up work mid-project.' },
        { label: 'Only the creating agent', rationale: 'Prevents a stray script marking work done.' },
      ],
    },
  );

  // A program with no milestones at all, for the "progress is unknown, not 0%" check.
  await call('POST', '/api/programs', {
    slug: emptySlug,
    name: `E2E Empty ${stamp}`,
    initialPrompt: '# empty',
    createdByAgentId: agent.id,
  });

  return {
    slug,
    emptySlug,
    agentId: agent.id,
    blockingClarificationId: blocking.id,
    firstOptionId: blocking.proposedOptions[0].id,
  };
}

export async function abandon(slug: string, actorAgentId: string): Promise<void> {
  await call('PATCH', `/api/programs/${slug}`, { actorAgentId, status: 'ABANDONED' });
}

export async function pollClarification(id: string): Promise<{
  status: string;
  answerText?: string;
  chosenOption?: { label: string };
  answeredBy?: { name: string; role: string };
}> {
  return call('GET', `/api/clarifications/${id}`);
}
