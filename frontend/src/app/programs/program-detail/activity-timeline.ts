import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

import { ActivityEntryResponse, ActivityService } from '../../api';
import { problemDetailMessage } from '../../shared/problem-detail';

/** How much history is worth showing before "Show more" earns its place. */
const PAGE_SIZE = 25;

/** R20, R22. The append-only history of one program, newest first. */
@Component({
  selector: 'pd-activity-timeline',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './activity-timeline.html',
  styleUrl: './activity-timeline.scss',
})
export class ActivityTimeline {
  private readonly activityApi = inject(ActivityService);

  public readonly programSlug = input.required<string>();
  /** Changing this reloads the log, so answering a question shows up immediately. */
  public readonly reloadToken = input<number>(0);

  protected readonly entries$ = signal<ActivityEntryResponse[]>([]);
  protected readonly totalElements$ = signal(0);
  protected readonly errorMessage$ = signal<string | null>(null);
  protected readonly size$ = signal(PAGE_SIZE);

  constructor() {
    // Reloads when the program changes and whenever the parent bumps the token, which it
    // does after every write. The signal writes happen in the response callback, well
    // after the effect has finished running.
    effect(() => {
      this.programSlug();
      this.reloadToken();
      this.load();
    });
  }

  protected showMore(): void {
    this.size$.update((size) => size + PAGE_SIZE);
    this.load();
  }

  private load(): void {
    this.activityApi.getProgramActivity(this.programSlug(), 0, this.size$()).subscribe({
      next: (page) => {
        this.entries$.set(page.content);
        this.totalElements$.set(page.totalElements);
        this.errorMessage$.set(null);
      },
      error: (error: unknown) => this.errorMessage$.set(problemDetailMessage(error)),
    });
  }

  protected iconFor(type: ActivityEntryResponse['type']): string {
    switch (type) {
      case 'PROGRAM_CREATED':
        return 'add_circle_outline';
      case 'PROGRAM_STATUS_CHANGED':
        return 'swap_horiz';
      case 'MILESTONE_COMPLETED':
        return 'check_circle_outline';
      case 'MILESTONE_DELETED':
        return 'delete_outline';
      case 'CLARIFICATION_ASKED':
        return 'help_outline';
      case 'CLARIFICATION_ANSWERED':
        return 'mark_chat_read';
      default:
        return 'edit';
    }
  }
}
