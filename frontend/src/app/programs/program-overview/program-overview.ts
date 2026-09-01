import { DatePipe, PercentPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { interval } from 'rxjs';

import { ProgramSummaryResponse, ProgramsService } from '../../api';
import { problemDetailMessage } from '../../shared/problem-detail';

/**
 * R21. Every program in one list: status, progress, open questions.
 *
 * <p>Refreshes on a timer because the point of the dashboard is noticing that an agent is
 * waiting on me without my having to reload. Only this page polls; the detail page reloads
 * on navigation and after each write, which avoids a timer racing the answer form.
 */
const REFRESH_INTERVAL_MS = 15_000;

@Component({
  selector: 'pd-program-overview',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    PercentPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatSlideToggleModule,
    MatTableModule,
    MatTooltipModule,
    RouterLink,
  ],
  templateUrl: './program-overview.html',
  styleUrl: './program-overview.scss',
})
export class ProgramOverview {
  private readonly programsApi = inject(ProgramsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns = ['name', 'status', 'progress', 'questions', 'updatedAt'];

  private readonly allPrograms$ = signal<ProgramSummaryResponse[]>([]);

  // R21: abandoned programs are hidden by default. There is no delete endpoint, so this
  // toggle is what keeps the list about work that is still alive.
  protected readonly showAbandoned$ = signal(false);

  protected readonly programs$ = computed(() =>
    this.showAbandoned$()
      ? this.allPrograms$()
      : this.allPrograms$().filter((program) => program.status !== 'ABANDONED'),
  );

  protected readonly abandonedCount$ = computed(
    () => this.allPrograms$().filter((program) => program.status === 'ABANDONED').length,
  );
  protected readonly isLoading$ = signal(false);
  protected readonly errorMessage$ = signal<string | null>(null);
  protected readonly lastRefreshedAt$ = signal<Date | null>(null);
  protected readonly hasLoadedOnce$ = signal(false);

  constructor() {
    this.refresh();
    interval(REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh());
  }

  protected toggleAbandoned(show: boolean): void {
    this.showAbandoned$.set(show);
  }

  protected refresh(): void {
    this.isLoading$.set(true);
    this.programsApi
      .listPrograms()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (programs) => {
          this.allPrograms$.set(programs);
          this.errorMessage$.set(null);
          this.lastRefreshedAt$.set(new Date());
          this.isLoading$.set(false);
          this.hasLoadedOnce$.set(true);
        },
        error: (error: unknown) => {
          this.errorMessage$.set(problemDetailMessage(error));
          this.isLoading$.set(false);
          this.hasLoadedOnce$.set(true);
        },
      });
  }
}
