import DOMPurify from 'dompurify';
import { marked } from 'marked';

/**
 * Renders markdown to HTML that is safe to insert into the page.
 *
 * <p>Sanitising is not optional here. An initial prompt is written by an agent and stored
 * verbatim, so it is untrusted input that gets rendered as HTML; without DOMPurify a
 * prompt containing a script tag would execute in the dashboard.
 */
export function renderMarkdown(source: string): string {
  const html = marked.parse(source, { async: false, gfm: true, breaks: false });
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } });
}
