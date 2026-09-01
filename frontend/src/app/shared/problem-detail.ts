import { HttpErrorResponse } from '@angular/common/http';

interface ProblemDetail {
  readonly title?: string;
  readonly detail?: string;
}

/**
 * Turns a failed request into something worth showing a human.
 *
 * <p>The backend answers every error with RFC 7807, and its `detail` already names the
 * offending input and what was expected. Showing that beats "Something went wrong", which
 * is what the browser would otherwise surface.
 */
export function problemDetailMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'The dashboard could not reach the API. Is the backend running?';
    }
    const problem = error.error as ProblemDetail | null;
    if (problem?.detail) {
      return problem.title ? `${problem.title}: ${problem.detail}` : problem.detail;
    }
    return `The API returned ${error.status} ${error.statusText}.`;
  }
  return 'The request failed for an unknown reason.';
}
