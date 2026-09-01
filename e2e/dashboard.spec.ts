import { expect, Page, test } from '@playwright/test';

import { abandon, pollClarification, Seeded, seedProgram } from './api';

/**
 * The success check, in a real browser against the built bundle.
 *
 * An implementing agent posts a clarification question through the REST API; I see it on
 * the dashboard and answer it there; the agent reads the answer back on its next poll.
 *
 * This suite exists because 44 backend tests once passed while the dashboard rendered
 * nothing: the OpenAPI document declared a wildcard media type instead of
 * application/json, so the generated client read every response as a Blob. Nothing below
 * the browser can see that class of failure.
 */
test.describe('program dashboard', () => {
  let seeded: Seeded;

  test.beforeAll(async () => {
    seeded = await seedProgram();
  });

  const failOnConsoleErrors = (page: Page): string[] => {
    const errors: string[] = [];
    page.on('console', (message) => message.type() === 'error' && errors.push(message.text()));
    page.on('pageerror', (error) => errors.push(String(error)));
    return errors;
  };

  test('the overview lists every program with its progress and open questions', async ({ page }) => {
    const errors = failOnConsoleErrors(page);
    await page.goto('/');

    const row = page.locator('tbody tr', { hasText: seeded.slug });
    await expect(row).toBeVisible();

    // R7, R12: two of four done. A program with no milestones shows no percentage at all,
    // because 0% would make "nobody has broken it down" look like "nobody has started".
    await expect(row).toContainText('50%');
    await expect(row).toContainText('(2/4)');

    const emptyRow = page.locator('tbody tr', { hasText: seeded.emptySlug });
    await expect(emptyRow).toBeVisible();
    await expect(emptyRow).not.toContainText('%');

    // R21: a blocking question means an agent has stopped and is waiting on me.
    await expect(row.locator('[data-testid="blocking-flag"]')).toContainText('1 blocking');

    expect(errors).toEqual([]);
  });

  test('the detail page shows the prompt as markdown and the milestone checklist', async ({ page }) => {
    const errors = failOnConsoleErrors(page);
    await page.goto(`/programs/${seeded.slug}`);

    // R22: rendered markdown, not the raw source.
    await expect(page.locator('.pd-markdown h1')).toContainText('e2e-dashboard');
    await expect(page.locator('.pd-markdown strong')).toContainText('and');
    await expect(page.locator('.pd-markdown pre')).toBeVisible();

    await expect(page.locator('.pd-milestone')).toHaveCount(4);
    await expect(page.locator('.pd-milestone.pd-complete')).toHaveCount(2);

    expect(errors).toEqual([]);
  });

  test('the activity timeline records what happened, newest first', async ({ page }) => {
    const errors = failOnConsoleErrors(page);
    await page.goto(`/programs/${seeded.slug}`);

    // R19, R20: creating the program, completing two milestones and asking the question.
    const entries = page.locator('[data-testid="activity-timeline"] li');
    await expect(entries.first()).toContainText('asked');
    await expect(entries.last()).toContainText('Registered program');
    await expect(entries).toHaveCount(4);

    expect(errors).toEqual([]);
  });

  test('abandoned programs are hidden until the toggle reveals them', async ({ page }) => {
    // R21: there is no delete endpoint, so ABANDONED plus this toggle is how a program
    // leaves the list.
    await abandon(seeded.emptySlug, seeded.agentId);
    await page.goto('/');

    const row = page.locator('tbody tr', { hasText: seeded.emptySlug });
    await expect(row).toHaveCount(0);

    const toggle = page.locator('[data-testid="show-abandoned"]');
    await expect(toggle).toBeVisible();
    await toggle.click();
    await expect(row).toBeVisible();
    await expect(row).toContainText('ABANDONED');
  });

  test('answering a blocking question in the browser reaches the agent that asked it', async ({
    page,
  }) => {
    const errors = failOnConsoleErrors(page);
    await page.goto(`/programs/${seeded.slug}`);

    const card = page.locator('pd-clarification-card', { hasText: 'Can any registered agent' });
    await expect(card.locator('[data-testid="blocking-tag"]')).toBeVisible();

    // R14: an answer needs at least one of a chosen option and free text, so the button
    // stays disabled rather than letting the backend deliver the news.
    const submit = card.locator('[data-testid="submit-answer"]');
    await expect(submit).toBeDisabled();

    await expect(card.locator('mat-radio-button')).toHaveCount(2);

    // Target the input itself rather than the Material wrapper: clicking the wrapper can
    // land on the rationale text, which does not toggle the radio.
    const preferredOption = card.getByRole('radio', { name: /Any registered agent/ });
    await preferredOption.check();
    await expect(preferredOption).toBeChecked();
    await expect(submit).toBeEnabled();

    await card
      .locator('[data-testid="answer-text"]')
      .fill('Option 1. completedByAgentId is enough of an audit trail.');
    await submit.click();

    // Once answered it is no longer blocking, and the answer is attributed to me.
    await expect(card.locator('[data-testid="blocking-tag"]')).toHaveCount(0);
    await expect(page.locator('body')).toContainText('Any registered agent');
    await expect(page.locator('body')).toContainText('harald');

    // R21: and the overview stops flagging the program.
    await page.goto('/');
    const row = page.locator('tbody tr', { hasText: seeded.slug });
    await expect(row.locator('[data-testid="blocking-flag"]')).toHaveCount(0);

    // R16: the agent polls and reads back exactly what I submitted.
    const polled = await pollClarification(seeded.blockingClarificationId);
    expect(polled.status).toBe('ANSWERED');
    expect(polled.answerText).toBe('Option 1. completedByAgentId is enough of an audit trail.');
    expect(polled.chosenOption?.label).toBe('Any registered agent');
    expect(polled.answeredBy?.name).toBe('harald');
    expect(polled.answeredBy?.role).toBe('HUMAN');

    expect(errors).toEqual([]);
  });
});
