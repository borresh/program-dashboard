import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { renderMarkdown } from './markdown';

/** Read-only markdown. The dashboard never edits a prompt, it only shows one. */
@Component({
  selector: 'pd-markdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="pd-markdown" [innerHTML]="html$()"></div>`,
  styles: `
    .pd-markdown {
      line-height: 1.6;
      overflow-wrap: anywhere;
    }
    .pd-markdown :first-child {
      margin-top: 0;
    }
    .pd-markdown :last-child {
      margin-bottom: 0;
    }
    .pd-markdown pre {
      background: var(--mat-sys-surface-container-high);
      border-radius: 8px;
      overflow-x: auto;
      padding: 0.75rem 1rem;
    }
    .pd-markdown code {
      font-family: ui-monospace, monospace;
      font-size: 0.9em;
    }
    .pd-markdown blockquote {
      border-left: 3px solid var(--mat-sys-outline-variant);
      color: var(--mat-sys-on-surface-variant);
      margin-inline: 0;
      padding-left: 1rem;
    }
  `,
})
export class Markdown {
  public readonly source = input.required<string>();

  protected readonly html$ = computed(() => renderMarkdown(this.source()));
}
