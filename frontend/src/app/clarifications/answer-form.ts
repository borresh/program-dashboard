import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';

import { ClarificationResponse, ClarificationsService } from '../api';
import { RUNTIME_CONFIG } from '../core/runtime-config';
import { problemDetailMessage } from '../shared/problem-detail';

/**
 * R14, R22. Answering one open question.
 *
 * <p>The answer is attributed to the seeded HUMAN agent, whose id arrives through
 * `PROGRAM_DASHBOARD_HUMAN_AGENT_ID`. That is what lets the asking agent see who replied
 * without the browser needing an identity of its own.
 */
@Component({
  selector: 'pd-answer-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
  ],
  templateUrl: './answer-form.html',
})
export class AnswerForm {
  private readonly clarificationsApi = inject(ClarificationsService);
  private readonly runtimeConfig = inject(RUNTIME_CONFIG);

  public readonly clarification = input.required<ClarificationResponse>();
  public readonly answered = output<ClarificationResponse>();

  protected readonly chosenOptionId$ = signal<string | null>(null);
  protected readonly answerText$ = signal('');
  protected readonly isSubmitting$ = signal(false);
  protected readonly errorMessage$ = signal<string | null>(null);

  // R14: an answer needs at least one of the two. The backend enforces this; the button
  // is disabled so the rejection is not the way you find out.
  protected readonly canSubmit$ = computed(
    () => this.answerText$().trim().length > 0 || this.chosenOptionId$() !== null,
  );

  protected onChooseOption(optionId: string): void {
    this.chosenOptionId$.set(optionId);
  }

  protected clearChosenOption(): void {
    this.chosenOptionId$.set(null);
  }

  protected onAnswerTextChange(value: string): void {
    this.answerText$.set(value);
  }

  protected submit(): void {
    if (!this.canSubmit$() || this.isSubmitting$()) {
      return;
    }

    const answerText = this.answerText$().trim();
    this.isSubmitting$.set(true);
    this.errorMessage$.set(null);

    this.clarificationsApi
      .answerClarification(this.clarification().id, {
        answeredByAgentId: this.runtimeConfig.humanAgentId,
        chosenOptionId: this.chosenOptionId$() ?? undefined,
        answerText: answerText.length > 0 ? answerText : undefined,
      })
      .subscribe({
        next: (answered) => {
          this.isSubmitting$.set(false);
          this.answered.emit(answered);
        },
        error: (error: unknown) => {
          // A 409 here means someone else answered first. Showing the backend's detail
          // tells the user who, which is more useful than retrying blindly.
          this.errorMessage$.set(problemDetailMessage(error));
          this.isSubmitting$.set(false);
        },
      });
  }
}
