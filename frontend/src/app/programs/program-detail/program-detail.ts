import { DatePipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { ProgramDetailResponse, ProgramsService } from '../../api';
import { ClarificationCard } from '../../clarifications/clarification-card';
import { Markdown } from '../../shared/markdown.component';
import { ActivityTimeline } from './activity-timeline';
import { problemDetailMessage } from '../../shared/problem-detail';

/**
 * R22. One program: its prompt, its milestone checklist, its open questions and its answers.
 *
 * <p>No timer here on purpose. It reloads on navigation and after every write, so nothing
 * can pull the ground out from under a half-written answer.
 */
@Component({
  selector: 'pd-program-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    PercentPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
    ActivityTimeline,
    ClarificationCard,
    Markdown,
  ],
  templateUrl: './program-detail.html',
  styleUrl: './program-detail.scss',
})
export class ProgramDetail {
  private readonly programsApi = inject(ProgramsService);

  /** Bound from the route, so navigating between programs reloads without a manual subscription. */
  public readonly slug = input.required<string>();

  protected readonly program$ = signal<ProgramDetailResponse | null>(null);
  /** Bumped on every reload so the activity timeline refetches alongside the program. */
  protected readonly reloadToken$ = signal(0);
  protected readonly isLoading$ = signal(false);
  protected readonly errorMessage$ = signal<string | null>(null);

  protected readonly openClarifications$ = computed(
    () => this.program$()?.clarifications.filter((it) => it.status === 'OPEN') ?? [],
  );
  protected readonly answeredClarifications$ = computed(
    () => this.program$()?.clarifications.filter((it) => it.status === 'ANSWERED') ?? [],
  );

  constructor() {
    // input.required is resolved before the first change detection, so reading it during
    // construction of the routed component is safe here.
    queueMicrotask(() => this.reload());
  }

  protected reload(): void {
    this.isLoading$.set(true);
    this.reloadToken$.update((token) => token + 1);
    this.programsApi.getProgram(this.slug()).subscribe({
      next: (program) => {
        this.program$.set(program);
        this.errorMessage$.set(null);
        this.isLoading$.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage$.set(problemDetailMessage(error));
        this.isLoading$.set(false);
      },
    });
  }
}
